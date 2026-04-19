package site.minnan.robotmanage.service.impl;

import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.CharacterExpDaily;
import site.minnan.robotmanage.entity.dao.CharacterExpDailyRepository;
import site.minnan.robotmanage.entity.dto.CharacterQueryDTO;
import site.minnan.robotmanage.entity.enumeration.World;
import site.minnan.robotmanage.entity.response.ResponseCode;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.infrastructure.validate.ValidateResult;
import site.minnan.robotmanage.mysql.dao.CharacterRecordMysqlRepository;
import site.minnan.robotmanage.mysql.dao.JobRepository;
import site.minnan.robotmanage.mysql.entity.CharacterRecordMysql;
import site.minnan.robotmanage.mysql.entity.Job;
import site.minnan.robotmanage.service.CharacterRankService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CharacterRankServiceImpl implements CharacterRankService {


    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CharacterExpDailyRepository characterExpDailyRepository;

    @Autowired
    private CharacterRecordMysqlRepository characterRecordMysqlRepository;

    private static final String REGION_LEVEL = "region:%s";

    private static final String REGION_JOB = "region:%s:job:%s";

    private static final String WORLD_LEVEL = "world:%d";

    private static final String WORLD_JOB = "world:%d:job:%s";

    private static final String REGION_LEGION = "region:%s:legion";

    private static final String WORLD_LEGION = "world:%d:legion";

    private static final BigDecimal thousand = BigDecimal.valueOf(1_000);


    @Override
    @Transactional(value = "mysqlTransactionManager")
    public void flushRank() {
        World[] worlds = World.values();
        for (World world : worlds) {
            flushRank(world);
        }
    }


    @Override
    @Transactional(value = "mysqlTransactionManager")
    public void flushRank(World world) {
        Integer worldId = world.getWorldId();
        String region = world.regionName();
        List<Job> jobNameList = jobRepository.findAll();
        byte[] regionLevelKey = REGION_LEVEL.formatted(region).getBytes();
        byte[] worldLevelKey = WORLD_LEVEL.formatted(worldId).getBytes();
        byte[] regionLegionKey = REGION_LEGION.formatted(region).getBytes();
        byte[] worldLegionKey = WORLD_LEGION.formatted(worldId).getBytes();

        for (Job job : jobNameList) {
            String jobName = job.getJobName();
            byte[] regionJobKey = REGION_JOB.formatted(region, jobName).getBytes();
            byte[] worldJobKey = WORLD_JOB.formatted(worldId, jobName).getBytes();
            Specification<CharacterRecordMysql> spec = ((root, query, criteriaBuilder) -> {
                Predicate jobNamePredicate = criteriaBuilder.equal(root.get("jobName"), jobName);
                Predicate regionPredicate = criteriaBuilder.equal(root.get("worldId"), worldId);
                return query.where(regionPredicate, jobNamePredicate).getRestriction();
            });
            List<CharacterRecordMysql> characterList = characterRecordMysqlRepository.findAll(spec);


            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (CharacterRecordMysql c : characterList) {
                    byte[] id = c.getId().toString().getBytes();
                    int score = c.getLevel() * 1_000_000 + c.getLevelPercent().multiply(thousand).intValue();
                    connection.zAdd(regionLevelKey, score, id);
                    connection.zAdd(worldLevelKey, score, id);
                    connection.zAdd(regionJobKey, score, id);
                    connection.zAdd(worldJobKey, score, id);
                    Integer legionLevel = c.getLegion();
                    if (legionLevel != null) {
                        connection.zAdd(regionLegionKey, legionLevel, id);
                        connection.zAdd(worldLegionKey, legionLevel, id);
                    }
                }
                connection.keyCommands().expire(regionJobKey, 60 * 60 * 24);
                connection.keyCommands().expire(worldJobKey, 60 * 60 * 24);
                return null;
            });
        }
        redisUtil.setExpire(new String(regionLevelKey), 24L, TimeUnit.HOURS);
        redisUtil.setExpire(new String(worldLevelKey), 24L, TimeUnit.HOURS);
    }

    @Override
    @Transactional(value = "mysqlTransactionManager")
    public ResponseEntity<?> getCharacterList(CharacterQueryDTO dto) {
        Integer rankType = dto.getRankType();
        String region = dto.getRegion();
        Integer worldId = dto.getWorldId();
        String jobName = dto.getJobName();

        World world = World.getById(worldId);
        ValidateResult result = new ValidateResult();
        if (worldId != null && world == null) {
            result.add("worldId", "worldId is and invalid id");
            return ResponseEntity.fail(ResponseCode.INVALID_PARAM, result);
        }
        if (world != null && region != null) {
            if (!region.equals(world.regionName())) {
                result.add("region",
                        "World with ID %d belongs to region '%s', but '%s' was provided".formatted(worldId, world.regionName(), region));
                return ResponseEntity.fail(ResponseCode.INVALID_PARAM, result);
            }
        }

        int code = rankType == 1 ? 0 : 1; //第一位，排名类型
        code = code << 1 | (worldId == null ? 0 : 1); //第二位，排名范围，world/region
        code = code << 1 | (jobName == null ? 0 : 1); //第三位，是否按职业
        String key = switch (code) {
            case 0b100 -> REGION_LEGION.formatted(region);
            case 0b110 -> WORLD_LEGION.formatted(worldId);
            case 0b11 -> WORLD_JOB.formatted(worldId, jobName);
            case 0b1 -> REGION_JOB.formatted(region, jobName);
            case 0b10 ->  WORLD_LEVEL.formatted(worldId);
            case 0b0 -> REGION_LEVEL.formatted(region);
            default -> "";
        };

        if (key.isEmpty()) {
            return  ResponseEntity.fail(ResponseCode.INVALID_PARAM, "invalid query command");
        }

        Integer pageIndex = dto.getPageIndex();
        Integer pageSize = dto.getPageSize();
        int start = (pageIndex - 1) * pageSize;
        Set<Integer> idSet = redisTemplate.opsForZSet().reverseRange(key, start, start + pageSize - 1);
        Long totalCount = redisTemplate.opsForZSet().count(key, 0, Integer.MAX_VALUE);
        assert idSet != null;


        List<CharacterRecordMysql> characterList = characterRecordMysqlRepository.findAllById(idSet);

        Map<Integer, CharacterRecordMysql> characterMap = characterList.stream()
                .collect(Collectors.toMap(CharacterRecordMysql::getId, Function.identity()));

        List<CharacterRecordMysql> sortedList = idSet.stream()
                .map(characterMap::get)  // 根据 idSet 的顺序取实体
                .filter(Objects::nonNull) // 防止可能的 null
                .collect(Collectors.toList());

        ListQueryVO<CharacterRecordMysql> queryResult = new ListQueryVO<>(sortedList, totalCount, idSet.size());
        return ResponseEntity.success(queryResult);
    }
}

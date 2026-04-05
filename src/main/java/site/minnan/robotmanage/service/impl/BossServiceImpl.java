package site.minnan.robotmanage.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.Boss;
import site.minnan.robotmanage.entity.aggregate.BossNickname;
import site.minnan.robotmanage.entity.dao.BossNicknameRepository;
import site.minnan.robotmanage.entity.dao.BossRepository;
import site.minnan.robotmanage.entity.dto.DetailsQueryDTO;
import site.minnan.robotmanage.entity.dto.GetBossListDTO;
import site.minnan.robotmanage.entity.dto.SaveBossDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.boss.BossInfoVO;
import site.minnan.robotmanage.infrastructure.exception.EntityAlreadyExistException;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.service.BossService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BossServiceImpl implements BossService {

    private BossRepository bossRepository;

    private BossNicknameRepository bossNicknameRepository;

    public BossServiceImpl(BossRepository bossRepository, BossNicknameRepository bossNicknameRepository) {
        this.bossRepository = bossRepository;
        this.bossNicknameRepository = bossNicknameRepository;
    }

    @Override
    public ListQueryVO<BossInfoVO> getBossList(GetBossListDTO dto) {
        Specification<Boss> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotBlank(dto.getKeyword())) {
                String keyword = dto.getKeyword().toUpperCase();
                Join<Boss, BossNickname> nickJoin = root.join("nicknames", JoinType.LEFT);
                predicates.add(
                        builder.or(
                                builder.like(builder.upper(root.get("bossName")), "%%%s%%".formatted(keyword)),
                                builder.like(builder.upper(nickJoin.get("bossNickName")), "%%%s%%".formatted(keyword))
                        )
                );
                query.distinct(true);
            }
            if (predicates.isEmpty()) {
                return query.getRestriction();
            }
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
        PageRequest page = PageRequest.of(dto.getPageIndex() - 1, dto.getPageSize(), Sort.by(Sort.Direction.ASC, "bossName"));
        Page<Boss> result = bossRepository.findAll(specification, page);
        List<BossInfoVO> list = result.stream().map(BossInfoVO::assemble).collect(Collectors.toList());
        return new ListQueryVO<>(list, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public BossInfoVO getBossInfo(DetailsQueryDTO dto) {
        Boss boss = bossRepository.findById(dto.getId()).orElseThrow(() -> new EntityNotExistException("BOSS不存在"));
        return BossInfoVO.assemble(boss);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBoss(SaveBossDTO dto) {
        if (bossRepository.findByBossNameIgnoreCase(dto.getBossName()).isPresent()) {
            throw new EntityAlreadyExistException("BOSS已存在");
        }
        validateNicknames(null, dto.getNicknames());
        Boss boss = new Boss();
        applyBossFields(dto, boss);
        bossRepository.save(boss);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBoss(SaveBossDTO dto) {
        Boss boss = bossRepository.findById(dto.getId()).orElseThrow(() -> new EntityNotExistException("BOSS不存在"));
        validateNicknames(boss.getId(), dto.getNicknames());
        applyBossFields(dto, boss);
        bossRepository.save(boss);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBoss(DetailsQueryDTO dto) {
        Boss boss = bossRepository.findById(dto.getId()).orElseThrow(() -> new EntityNotExistException("BOSS不存在"));
        bossRepository.delete(boss);
    }

    private void applyBossFields(SaveBossDTO dto, Boss boss) {
        boss.setBossName(dto.getBossName());
        boss.setHp(dto.getHp());
        boss.setLevel(dto.getLevel());
        boss.setPhysicalDefense(dto.getPhysicalDefense());
        boss.setMagicalDefense(dto.getMagicalDefense());
        boss.setElementReduction(dto.getElementReduction() ? 1 : 0);
        boss.setArc(dto.getArc());
        boss.setAut(dto.getAut());
        boss.setReenterInterval(dto.getReenterInterval());
        boss.setClaimLimit(dto.getClaimLimit());
        boss.setReward(dto.getReward());

        List<String> normalized = CollectionUtil.isNotEmpty(dto.getNicknames())
                ? dto.getNicknames().stream()
                .filter(StrUtil::isNotBlank)
                .map(String::toUpperCase)
                .distinct()
                .toList()
                : new ArrayList<>();

        // Update nickname list in-place to avoid delete/insert collisions on UNIQUE(boss_nick_name)
        List<BossNickname> existing = boss.getNicknames() == null ? new ArrayList<>() : boss.getNicknames();
        existing.removeIf(n -> n == null || !normalized.contains(n.getBossNickName()));
        for (String name : normalized) {
            boolean alreadyExists = existing.stream().anyMatch(n -> name.equals(n.getBossNickName()));
            if (!alreadyExists) {
                BossNickname nick = new BossNickname();
                nick.setBossNickName(name);
                boss.addNickname(nick);
            }
        }
        if (boss.getNicknames() == null) {
            boss.setNicknames(existing);
        }
    }

    private void validateNicknames(Integer currentBossId, List<String> nicknames) {
        if (CollectionUtil.isEmpty(nicknames)) {
            return;
        }
        for (String nick : nicknames) {
            Optional<BossNickname> nickOpt = bossNicknameRepository.findByBossNickNameIgnoreCase(nick);
            if (nickOpt.isPresent() && (currentBossId == null || !nickOpt.get().getBoss().getId().equals(currentBossId))) {
                throw new EntityAlreadyExistException("昵称已被其他BOSS占用：" + nick);
            }
        }
    }
}

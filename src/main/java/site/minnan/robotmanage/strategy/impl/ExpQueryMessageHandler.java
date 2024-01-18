package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.LvExp;
import site.minnan.robotmanage.entity.dao.LvExpRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.CharacterData;
import site.minnan.robotmanage.entity.vo.ExpData;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 经验查询消息处理
 *
 * @author Minnan on 2024/01/18
 */
@Service("expQuery")
@Slf4j
public class ExpQueryMessageHandler implements MessageHandler {


    private CharacterSupportService characterSupportService;

    private LvExpRepository lvExpRepository;

    public ExpQueryMessageHandler(CharacterSupportService characterSupportService, LvExpRepository lvExpRepository) {
        this.characterSupportService = characterSupportService;
        this.lvExpRepository = lvExpRepository;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        String queryContent = message.substring(4);
        String queryTarget;
        try {
            queryTarget = characterSupportService.parseQueryContent(queryContent, dto.getSender().userId());
            log.info("查询内容为[{}]，解析出查询目标为：[{}]", queryContent, queryTarget);
        } catch (EntityNotFoundException e) {
            log.warn("解析查询内容失败，查询内容为{}", queryContent);
            return Optional.of("查询失败");
        }

        CharacterData c;
        try {
            c = characterSupportService.fetchCharacterInfo(queryTarget);
        } catch (Exception e) {
            log.error("查询角色信息失败，查询目标为" + queryTarget, e);
            return Optional.of("查询失败");
        }

        StringBuilder sb = new StringBuilder();
        String baseInfo = """
                角色:%s(%s)
                等级:%s - %s （区排名%s,服排名%s）
                职业:%s (区排名%s,服排名%s)
                """
                .formatted(c.getName(), c.getServer(),
                        c.getLevel(), c.getExpPercent(), c.getServerLevelRank(), c.getGlobalLevelRank(),
                        c.getJob(), c.getServerClassRank(), c.getGlobalClassRank());

        sb.append(baseInfo)
                .append("---------------------------------\n");

        List<ExpData> expData = c.getExpData();
        if (CollectionUtil.isEmpty(expData)) {
            return Optional.of(sb.toString());
        }

        expData = expData.stream()
                .sorted(Comparator.comparing(ExpData::dateLabel))
                .limit(14)
                .toList();
        expData.stream()
                .map(e -> "%s：%s\n".formatted(e.dateLabel(), e.formatExpDifference()))
                .forEachOrdered(sb::append);

        return Optional.of(sb.toString());
    }
}

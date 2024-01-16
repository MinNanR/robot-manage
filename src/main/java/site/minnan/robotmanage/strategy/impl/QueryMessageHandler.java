package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.CharacterData;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;

/**
 * 角色信息查询消息处理
 *
 * @author Minnan on 2024/01/15
 */
@Service("query")
public class QueryMessageHandler implements MessageHandler {


    private CharacterSupportService characterSupportService;

    public QueryMessageHandler(CharacterSupportService characterSupportService) {
        this.characterSupportService = characterSupportService;
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
        String queryContent = message.substring(2);
        CharacterData c = characterSupportService.fetchCharacterInfo(queryContent);

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
        if (c.getAchievementPoints() != null) {
//            String achievementInfo = """
//                    成就值:%s（排名：%s）
//                    联盟等级:%s（排名：%s）
//                    联盟战斗力:%s（每日%s币）
//                    """
//                    .formatted(c.getAchievementPoints(), c.getAchievementRank(),
//                            c.getLegionLevel(), c.getLegionRank(),
//                            c.getLegionPower(), c.getLegionCoinsPerDay());
            String achievementInfo = "成就值:%s（排名：%s）".formatted(c.getAchievementPoints(), c.getAchievementRank());
            sb.append(achievementInfo).append("\n");
        }
        if (c.getLegionLevel() != null) {
            String legionInfo = """
                    联盟等级:%s（排名：%s）
                    联盟战斗力:%.2f（每日%s币）
                    """
                    .formatted(c.getLegionLevel(), c.getLegionRank(),
                            (float) Integer.parseInt(c.getLegionPower()) / 1000000, c.getLegionCoinsPerDay());
            sb.append(legionInfo);
        }

        List<CharacterData> nearRankList = c.getNearRank();
        if (CollUtil.isNotEmpty(nearRankList)) {
            sb.append("---------------------------------\n")
                    .append("职业排名附近的人：\n");
            for (CharacterData nearRank : nearRankList) {
                String rankLine = "%s-%s:%s(%s)\n".formatted(nearRank.getServerClassRank(), nearRank.getName(),
                        nearRank.getLevel(), nearRank.getExpPercent());
                sb.append(rankLine);
            }
        }

        return Optional.of(sb.toString());
    }
}

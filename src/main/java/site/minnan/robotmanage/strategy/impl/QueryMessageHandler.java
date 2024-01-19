package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONObject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.CharacterData;
import site.minnan.robotmanage.entity.vo.ExpData;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 角色信息查询消息处理
 *
 * @author Minnan on 2024/01/15
 */
@Service("query")
@Slf4j
public class QueryMessageHandler implements MessageHandler {


    private CharacterSupportService characterSupportService;

    private Integer billion = 1000000000;

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
        if (c.getAchievementPoints() != null) {
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
//
//        int queryCount = characterSupportService.getQueryCount(queryTarget, dto.getSender().userId());
//        if (queryCount >= 3) {
//            sb.append("[CQ:image,file=https://minnan.site:2005/rot/20240118/toomuch.jpg,subType=0]");
//        }

        createPic(c);

        return Optional.of(sb.toString());
    }

    private void createPic(CharacterData characterData) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("c", characterData);

        List<ExpData> expData = characterData.getExpData();
        expData = ListUtil.sub(expData, expData.size() - 14, expData.size());
        List<String> dateList = expData.stream().map(e -> e.dateLabel()).toList();
        List<Double> expList = expData.stream()
                .map(e -> e.expDifference())
                .map(e -> NumberUtil.round(e / billion, 1))
                .map(e -> e.doubleValue())
                .toList();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("noteDate", dateList)
                .set("exp", expList);

        List<ExpData> reverseExpData = ListUtil.reverseNew(expData);

        long sum7 = reverseExpData.stream()
                .limit(7)
                .mapToLong(e -> e.expDifference() / billion)
                .sum();
        context.setVariable("sum7", sum7);
        context.setVariable("avg7", String.format("%.4fb", (float) (sum7 / 7)));

        long sum14 = reverseExpData.stream()
                .limit(14)
                .mapToLong(e -> e.expDifference() / billion)
                .sum();

        context.setVariable("sum14", sum14);
        context.setVariable("avg14", String.format("%.4fb", (float) (sum14 / 14)));

        List<List<ExpData>> expDataSplit = ListUtil.split(expData, 5);
        context.setVariable("exp1", expDataSplit.get(0));
        context.setVariable("exp2", expDataSplit.get(1));
        context.setVariable("exp3", expDataSplit.get(2));

        context.setVariable("levelPredicate", Collections.emptyList());

//        context.setVariable("expDataString", jsonObject.toJSONString(0).replaceAll("\"", "\\\\\""));
        context.setVariable("expDataString", jsonObject);

        String html = templateEngine.process("picTemplate/query", context);
        System.out.println(html);
    }

}

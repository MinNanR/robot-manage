package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import site.minnan.robotmanage.entity.aggregate.LvExp;
import site.minnan.robotmanage.entity.dao.LvExpRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;
import site.minnan.robotmanage.entity.vo.bot.ExpData;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;

/**
 * 角色信息查询消息处理
 *
 * @author Minnan on 2024/01/15
 */
@Service("query")
@Slf4j
public class QueryMessageHandler implements MessageHandler {


    private CharacterSupportService characterSupportService;

    private LvExpRepository lvExpRepository;

    private static final Integer billion = 1000000000;

    @Value("${query.pythonPath}")
    private String pythonPath;

    @Value("${query.folder}")
    public String folder;

    @Value("${query.baseUrl}")
    private String baseUrl;

    private Proxy proxy;

    public QueryMessageHandler(CharacterSupportService characterSupportService, LvExpRepository lvExpRepository, Proxy proxy) {
        this.characterSupportService = characterSupportService;
        this.lvExpRepository = lvExpRepository;
        this.proxy = proxy;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String userId = dto.getSender().userId();

        String message = dto.getRawMessage();
        String queryContent = message.substring(2).strip();
        String queryTarget;
        try {
            queryTarget = characterSupportService.parseQueryContent(queryContent, userId);
            log.info("查询内容为[{}]，解析出查询目标为：[{}]", queryContent, queryTarget);
        } catch (EntityNotFoundException e) {
            log.warn("解析查询内容失败，查询内容为{}", queryContent);
            return Optional.of("查询失败");
        }

        String today = DateTime.now().offset(DateField.HOUR, -8).toString("yyyMMdd");
        Supplier<String> getResult = () -> {
            String url = "%s/%s/%s.png".formatted(baseUrl, today, queryTarget.toLowerCase());
            return "[CQ:image,file=%s,subType=0]".formatted(url);
        };

        String pngPath = "%s/%s/%s.png".formatted(folder, today, queryTarget.toLowerCase());
        if (FileUtil.exist(pngPath)) {
            return Optional.of(getResult.get());
        }

        CharacterData c;
        try {
            c = characterSupportService.fetchCharacterInfo(queryTarget);
        } catch (Exception e) {
            log.error("查询角色信息失败，查询目标为" + queryTarget, e);
            return Optional.of("查询失败");
        }

//        StringBuilder sb = new StringBuilder();
//        String baseInfo = """
//                角色:%s(%s)
//                等级:%s - %s （区排名%s,服排名%s）
//                职业:%s (区排名%s,服排名%s)
//                """
//                .formatted(c.getName(), c.getServer(),
//                        c.getLevel(), c.getExpPercent(), c.getServerLevelRank(), c.getGlobalLevelRank(),
//                        c.getJob(), c.getServerClassRank(), c.getGlobalClassRank());
//
//        sb.append(baseInfo)
//                .append("---------------------------------\n");
//        if (c.getAchievementPoints() != null) {
//            String achievementInfo = "成就值:%s（排名：%s）".formatted(c.getAchievementPoints(), c.getAchievementRank());
//            sb.append(achievementInfo).append("\n");
//        }
//        if (c.getLegionLevel() != null) {
//            String legionInfo = """
//                    联盟等级:%s（排名：%s）
//                    联盟战斗力:%.2f（每日%s币）
//                    """
//                    .formatted(c.getLegionLevel(), c.getLegionRank(),
//                            (float) Integer.parseInt(c.getLegionPower()) / 1000000, c.getLegionCoinsPerDay());
//            sb.append(legionInfo);
//        }
//
//        List<CharacterData> nearRankList = c.getNearRank();
//        if (CollUtil.isNotEmpty(nearRankList)) {
//            sb.append("---------------------------------\n")
//                    .append("职业排名附近的人：\n");
//            for (CharacterData nearRank : nearRankList) {
//                String rankLine = "%s-%s:%s(%s)\n".formatted(nearRank.getServerClassRank(), nearRank.getName(),
//                        nearRank.getLevel(), nearRank.getExpPercent());
//                sb.append(rankLine);
//            }
//        }
//
        createPic(c);
        int queryCount = characterSupportService.getQueryCount(queryTarget, dto.getSender().userId());
        if (queryCount >= 3) {
//            sb.append("[CQ:image,file=https://minnan.site:2005/rot/20240118/toomuch.jpg,subType=0]");
            String result = getResult.get();
            result = result + "[CQ:image,file=https://minnan.site:2005/rot/20240118/toomuch.jpg,subType=0]";
            return Optional.of(result);
        } else {
            return Optional.of(getResult.get());
        }


    }

    /**
     * 绘制角色信息图
     *
     * @param characterData 角色信息
     */
    private void createPic(CharacterData characterData) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Context context = new Context();
        context.setVariable("c", characterData);

        String characterImgUrl = characterData.getCharacterImgUrl();
        //下载角色证件照，直接用浏览器启动不能使用代理，先下载然后转base64可以快一点
        if (StrUtil.isNotBlank(characterImgUrl)) {
            HttpResponse imgRequest = HttpUtil.createGet(characterImgUrl).setProxy(proxy).execute();
            String imgBase64 = Base64.encode(imgRequest.bodyStream());
            context.setVariable("img", "data:image/png;base64,%s".formatted(imgBase64));
        }

        List<ExpData> expData = characterData.getExpData();
        //截取最后14天经验数据
        expData = ListUtil.sub(expData, expData.size() - 14, expData.size());
        List<String> dateList = expData.stream().map(e -> e.dateLabel()).toList();
        List<Double> expList = expData.stream()
                .map(e -> e.expDifference())
                .map(e -> NumberUtil.round(e / billion, 4))
                .map(e -> e.doubleValue())
                .toList();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("noteDate", dateList)
                .set("exp", expList);

        List<ExpData> reverseExpData = ListUtil.reverseNew(expData);

        double sum7 = reverseExpData.stream()
                .limit(7)
                .mapToDouble(e -> (double) e.expDifference() / billion)
                .sum();
        context.setVariable("sum7", "%.4fb".formatted(sum7));
        context.setVariable("avg7", "%.4fb".formatted(BigDecimal.valueOf(sum7).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP).doubleValue()));

        double sum14 = reverseExpData.stream()
                .limit(14)
                .mapToDouble(e -> (double) e.expDifference() / billion)
                .sum();

        context.setVariable("sum14", "%.4fb".formatted(sum14));
        context.setVariable("avg14", "%.4fb".formatted(BigDecimal.valueOf(sum14).divide(BigDecimal.valueOf(14), 4, RoundingMode.HALF_UP).doubleValue()));

        List<List<ExpData>> expDataSplit = ListUtil.split(expData, 5);
        //拆分成三栏进行展示
        context.setVariable("exp1", expDataSplit.get(0));
        context.setVariable("exp2", expDataSplit.get(1));
        context.setVariable("exp3", expDataSplit.get(2));

        //经验预测
        PageRequest page = PageRequest.of(0, 10);
        int lv = Integer.parseInt(characterData.getLevel());
        List<LvExp> stageList = lvExpRepository.findByLvGreaterThanEqual(lv, page);
        DateTime now = DateTime.now();
        if (stageList.isEmpty()) {
            context.setVariable("levelPredicate", Collections.emptyList());
        } else {
            //计算7日均经验
            double avg7 = BigDecimal.valueOf(sum7).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP).doubleValue() * billion;
            Iterator<LvExp> itr = stageList.iterator();
            LvExp currentStage = itr.next();
            //当前等级经验百分比
            double expPercent = Double.parseDouble(characterData.getExpPercent().replace("%", ""));
            //当前等级升级经验
            long expToNextLevel = Long.parseLong(currentStage.getExpToNextLevel());
            //当前刷了多少经验
            long currentExp = (long) (expToNextLevel * (expPercent / 100));
            //需要多少经验升级
            long expNeed = expToNextLevel - currentExp;
            //这一级还需要刷多少天升级
            int dayNeed = NumberUtil.round(expNeed / avg7, 0).intValue();
            ReckonLevel reckonItem = new ReckonLevel(lv + 1, now.offsetNew(DateField.DAY_OF_YEAR, dayNeed).toDateStr());
            ArrayList<ReckonLevel> reckonList = ListUtil.toList(reckonItem);

            while (itr.hasNext()) {
                LvExp lvInfo = itr.next();
                lv = lvInfo.getLv() + 1;
                //等级大于300结束预测
                if (lv > 300) {
                    break;
                }
                expNeed = Long.parseLong(lvInfo.getExpToNextLevel());
                dayNeed = dayNeed + (int) (expNeed / avg7);
                reckonItem = new ReckonLevel(lv, now.offsetNew(DateField.DAY_OF_YEAR, dayNeed).toDateStr());
                reckonList.add(reckonItem);
            }
            context.setVariable("levelPredicate", reckonList);
        }

        context.setVariable("expDataString", jsonObject);
        //使用模板引擎，生成html代码
        String html = templateEngine.process("picTemplate/query", context);

        String today = now.offset(DateField.HOUR, -8).toString("yyyyMMdd");
        String folderPath = "%s/%s".formatted(folder, today);
        File folderFile = new File(folderPath);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        //html文件路径
        String htmlPath = "%s/%s/%s.html".formatted(folder, today, characterData.getName());
        //png图片文件路径
        String pngPath = "%s/%s/%s.png".formatted(folder, today, characterData.getName().toLowerCase());
        //将html代码写入文件
        FileUtil.writeString(html, htmlPath, Charset.defaultCharset());
        //调用python代码，使用无头浏览器渲染html代码后截图
        RuntimeUtil.execForStr("python3 %s %s %s".formatted(pythonPath, htmlPath, pngPath));
    }

    private record ReckonLevel(int lv, String dateLabel) {
    }

}

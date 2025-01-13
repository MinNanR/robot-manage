package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.HyperStat.CurrentLevel;
import site.minnan.robotmanage.entity.vo.HyperStat.CurrentStat;
import site.minnan.robotmanage.infrastructure.utils.BotSessionUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 超级属性计算器
 *
 * @author Minnan on 2024/10/08
 */
@Component("hyperStat")
public class HyperStatMessageHandler implements MessageHandler {

    //升级所需的超级属性点数
    private static final int[] hyperStatPoint = new int[]{1, 2, 4, 8, 10, 15, 20, 25, 30, 35, 50, 65, 80, 95, 110, 0};

    private static final int[] hyperStatPointTotal = new int[]{0, 1, 3, 7, 15, 25, 40, 60, 85, 115, 150, 200, 265, 345, 440, 550};

    private static final int[] ignGain = new int[]{0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45};

    private static final int[] bdGain = new int[]{0, 3, 6, 9, 12, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55};

    private static final int[] dmgGain = new int[]{0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45};

    private static final int[] crdGain = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    private static final int[] statGain = new int[]{0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 360, 390, 420, 450};

    private static final int[] attGain = new int[]{0, 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39, 42, 45};

    private static final int[] crrGain = new int[]{0, 1, 2, 3, 4, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25};


    private static final IgnoreMessageHandler ignoreMessageHandler = new IgnoreMessageHandler();

    private final BotSessionUtil botSessionUtil;

    public HyperStatMessageHandler(BotSessionUtil botSessionUtil) {
        this.botSessionUtil = botSessionUtil;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String reply = """
                你的职业是否为弓箭手系？
                是输入1，否输入0
                """;
        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();
        botSessionUtil.startSession(groupId, userId, this::recordClass, Duration.ofMinutes(5));
        return Optional.of(reply);
    }

    /**
     * 记录职业(记录是否为弓箭手)
     *
     * @param dto
     * @return
     */
    public Optional<String> recordClass(MessageDTO dto) {

        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();

        String message = dto.getRawMessage();
        boolean isArcher = "1".equals(message);
        botSessionUtil.startSession(groupId, userId, d -> recordCurrentStat(d, isArcher), Duration.ofMinutes(5));

        if (!isArcher) {
            String reply = """
                    请依次输入当前角色面板上下列数据参数
                    BD DMG 无视 爆伤 主属性 副属性 攻击力基础
                    """;
            return Optional.of(reply);
        } else {
            String reply = """
                    请依次输入当前角色面板上下列数据参数
                    BD DMG 无视 爆伤 主属性 副属性 攻击力基础 暴击率
                    """;
            return Optional.of(reply);
        }


    }

    /**
     * 记录当前面板属性
     *
     * @param dto
     * @return
     */
    public Optional<String> recordCurrentStat(MessageDTO dto, boolean isArcher) {
        String rawMessage = dto.getRawMessage();
        String[] paramSplit = rawMessage.split("\\s+");
        try {
            int bd = Integer.parseInt(paramSplit[0]);
            int dmg = Integer.parseInt(paramSplit[1]);
            float ign = Float.parseFloat(paramSplit[2]);
            float crd = Float.parseFloat(paramSplit[3]);
            int primaryStat = Integer.parseInt(paramSplit[4]);
            int secondaryStat = Integer.parseInt(paramSplit[5]);
            int att = Integer.parseInt(paramSplit[6]);
            float crr = 0;
            if (isArcher) {
                crr = Float.parseFloat(paramSplit[7]);
            }
            CurrentStat currentStat = new CurrentStat(bd, dmg, ign, crd, primaryStat, secondaryStat, att, crr, isArcher);
            botSessionUtil.updateSessionMessageHandler(dto.getGroupId(), dto.getSender().userId(), d -> recordCurrentLevel(d, currentStat));
        } catch (Exception e) {
            return Optional.of("请输入正确的参数");
        }

        if (!isArcher) {
            String reply = """
                    请依次输入以下超属等级
                    BD DMG 无视 爆伤 主属 att 超级属性点数剩余
                    """;
            return Optional.of(reply);
        } else {
            String reply = """
                    请依次输入以下超属等级
                    BD DMG 无视 爆伤 主属 att 暴击率 超级属性点数剩余
                    """;
            return Optional.of(reply);
        }


    }

    /**
     * 记录当前超级属性等级
     *
     * @param dto
     * @param currentStat
     * @return
     */
    public Optional<String> recordCurrentLevel(MessageDTO dto, CurrentStat currentStat) {

        String rawMessage = dto.getRawMessage();
        String[] paramSplit = rawMessage.split("\\s+");
        try {
            int bdLevel = Integer.parseInt(paramSplit[0]);
            int dmgLevel = Integer.parseInt(paramSplit[1]);
            int ignLevel = Integer.parseInt(paramSplit[2]);
            int crdLevel = Integer.parseInt(paramSplit[3]);
            int primaryStatLevel = Integer.parseInt(paramSplit[4]);
            int attLevel = Integer.parseInt(paramSplit[5]);
            int crrLevel;
            int pointLeft;
            if (currentStat.isArcher()) {
                crrLevel = Integer.parseInt(paramSplit[6]);
                pointLeft = Integer.parseInt(paramSplit[7]);
            } else {
                crrLevel = 0;
                pointLeft = Integer.parseInt(paramSplit[6]);
            }
            CurrentLevel currentLevel = new CurrentLevel(bdLevel, dmgLevel, ignLevel, crdLevel, primaryStatLevel, attLevel, crrLevel, pointLeft);

            int bd = currentStat.bd() - bdGain[bdLevel];
            int dmg = currentStat.dmg() - dmgGain[dmgLevel];
            float ign = ignoreMessageHandler.newDef(currentStat.ign() / 100f, ignGain[ignLevel] / -100f);
            float crd = currentStat.crd() - crdGain[crdLevel];
            int primaryStat = currentStat.primaryStat() - statGain[primaryStatLevel];
            int att = currentStat.att() - attGain[attLevel];
            int crr = currentLevel.crr() - crrGain[crrLevel];

            CurrentStat currentStatWithoutHyper = new CurrentStat(bd, dmg, ign, crd, primaryStat, currentStat.secondaryStat(), att, crr, currentStat.isArcher());
            botSessionUtil.updateSessionMessageHandler(dto.getGroupId(), dto.getSender().userId(),
                    d -> executeCalculate(d, currentStatWithoutHyper, currentLevel));
        } catch (Exception e) {
            return Optional.of("请输入正确的参数");
        }

        String reply = "请输入目标BOSS防御力";
        return Optional.of(reply);
    }


    public Optional<String> executeCalculate(MessageDTO dto, CurrentStat currentStat, CurrentLevel currentLevel) {
        String rawMessage = dto.getRawMessage();
        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();
        boolean isArcher = currentStat.isArcher();

        int bossDef = Integer.parseInt(rawMessage);

        int point = currentLevel.pointLeft();

        int bdLevel = currentLevel.bd();
        int dmgLevel = currentLevel.dmg();
        int ignLevel = currentLevel.ign();
        int crdLevel = currentLevel.crd();

        //将初始等级调至10级
        if (bdLevel < 10) {
            int pointConsume = hyperStatPointTotal[10] - hyperStatPointTotal[bdLevel];
            point -= pointConsume;
            bdLevel = 10;
        }
        if (dmgLevel < 10) {
            int pointConsume = hyperStatPointTotal[10] - hyperStatPointTotal[dmgLevel];
            point -= pointConsume;
            dmgLevel = 10;
        }
        if (ignLevel < 10) {
            int pointConsume = hyperStatPointTotal[10] - hyperStatPointTotal[ignLevel];
            point -= pointConsume;
            ignLevel = 10;
        }
        if (crdLevel < 10) {
            int pointConsume = hyperStatPointTotal[10] - hyperStatPointTotal[crdLevel];
            point -= pointConsume;
            crdLevel = 10;
        }

        //点数不能将初始等级调至10级，不做计算
        if (point < 0) {
            botSessionUtil.endSession(groupId, userId);
            return Optional.of("当前输入的参数无法计算，请重新输入");
        }

        int bdLevelStart = bdLevel;
        int dmgLevelStart = dmgLevel;
        int ignLevelStart = ignLevel;
        int crdLevelStart = crdLevel;
        int primaryStatLevelStart = currentLevel.primaryStat();
        int attLevelStart = currentLevel.att();
        int crrLevelStart = currentLevel.crr();
        int pointLeft = point;

        List<CurrentLevel> levelList = new ArrayList<>();
        int maxEncode = 1 << (isArcher ? 28 : 24);
        for (int code = 0; code < maxEncode; code++) {
            int level1 = code & 0xF;
            int level2 = (code >> 4) & 0xF;
            int level3 = (code >> 8) & 0xF;
            int level4 = (code >> 12) & 0xF;
            int level5 = (code >> 16) & 0xF;
            int level6 = (code >> 20) & 0xF;
            int level7 = (code >> 24) & 0xF;
            if (level1 < 10 || level2 < 10 || level3 < 10 || level4 < 10) {
                continue;
            }
            if (level1 < bdLevelStart || level2 < dmgLevelStart || level3 < ignLevelStart || level4 < crdLevelStart
                    || level5 < primaryStatLevelStart || level6 < attLevelStart) {
                continue;
            }
            int pointConsume1 = hyperStatPointTotal[level1] - hyperStatPointTotal[bdLevelStart];
            int pointConsume2 = hyperStatPointTotal[level2] - hyperStatPointTotal[dmgLevelStart];
            int pointConsume3 = hyperStatPointTotal[level3] - hyperStatPointTotal[ignLevelStart];
            int pointConsume4 = hyperStatPointTotal[level4] - hyperStatPointTotal[crdLevelStart];
            int pointConsume5 = hyperStatPointTotal[level5] - hyperStatPointTotal[primaryStatLevelStart];
            int pointConsume6 = hyperStatPointTotal[level6] - hyperStatPointTotal[attLevelStart];
            int pointConsume7 = isArcher ? hyperStatPointTotal[level7] - hyperStatPointTotal[crrLevelStart] : 0;
            int pointConsume = pointConsume1 + pointConsume2 + pointConsume3 + pointConsume4 + pointConsume5 + pointConsume6 + pointConsume7;
            if (pointConsume > pointLeft) {
                continue;
            }
            int surplusPoint = pointLeft - pointConsume;
            if (surplusPoint < hyperStatPoint[level1]
                    && surplusPoint < hyperStatPoint[level2]
                    && surplusPoint < hyperStatPoint[level3]
                    && surplusPoint < hyperStatPoint[level4]
                    && surplusPoint < hyperStatPoint[level5]
                    && surplusPoint < hyperStatPoint[level6]
                    && (!isArcher || surplusPoint < hyperStatPoint[level7])) {
                CurrentLevel level = new CurrentLevel(level1, level2, level3, level4, level5, level6, level7, 0);
                levelList.add(level);
            }
        }


        Function<CurrentLevel, Float> indicator = e -> {
            int bd = currentStat.bd() + bdGain[e.bd()];
            int dmg = currentStat.dmg() + dmgGain[e.dmg()];
            float ign = ignoreMessageHandler.newDef(currentStat.ign(), ignGain[e.ign()] / 100f);
            float crd = currentStat.crd() + crdGain[e.crd()];
            float ignFix = ignoreMessageHandler.bossDmg(bossDef / 100f, ign);
            int primaryStat = currentStat.primaryStat() + statGain[e.primaryStat()];
            int secondaryStat = currentStat.secondaryStat();
            int att = currentStat.att() + attGain[e.att()];
            if (isArcher) {
                float crr = currentStat.crr() + crrGain[e.crr()];
                crd = crd + crr * 0.25f;
            }
            return (100 + bd + dmg) * (135 + crd) * ignFix * (primaryStat * 4 + secondaryStat) * att;
        };

        Function<CurrentLevel, Integer> pointUse = e -> {
            int pointConsume1 = hyperStatPointTotal[e.bd()] - hyperStatPointTotal[bdLevelStart];
            int pointConsume2 = hyperStatPointTotal[e.dmg()] - hyperStatPointTotal[dmgLevelStart];
            int pointConsume3 = hyperStatPointTotal[e.ign()] - hyperStatPointTotal[ignLevelStart];
            int pointConsume4 = hyperStatPointTotal[e.crd()] - hyperStatPointTotal[crdLevelStart];
            int pointConsume5 = hyperStatPointTotal[e.primaryStat()] - hyperStatPointTotal[primaryStatLevelStart];
            int pointConsume6 = hyperStatPointTotal[e.att()] - hyperStatPointTotal[attLevelStart];
            int pointConsume7 = isArcher ? hyperStatPointTotal[e.crr()] - hyperStatPointTotal[crrLevelStart] : 0;
            return pointConsume1 + pointConsume2 + pointConsume3 + pointConsume4 + pointConsume5 + pointConsume6 + pointConsume7;
        };


        Optional<CurrentLevel> maxGainOpt = levelList.stream().max(Comparator.comparing(indicator));

        if (maxGainOpt.isEmpty()) {
            return Optional.of("计算收益失败");
        }

        CurrentLevel maxGain = maxGainOpt.get();


        CurrentLevel originLevel = new CurrentLevel(bdLevelStart, dmgLevelStart, ignLevelStart, crdLevelStart, primaryStatLevelStart, attLevelStart, crrLevelStart, 0);
        Float originValue = indicator.apply(originLevel);
        Float gainValue = indicator.apply(maxGain);

        BigDecimal differ = NumberUtil.div(gainValue, originValue, 4).subtract(BigDecimal.ONE);
        String reply = """
                加点方案：
                BD：%d级
                DMG：%d级
                无视：%d级
                爆伤：%d级
                主属：%d级
                att：%d级
                暴击率：%d级
                增加FD：%s，剩余点数：%d
                再次输入BOSS防御力重新计算，输入-1结束计算
                """.formatted(maxGain.bd(), maxGain.dmg(), maxGain.ign(), maxGain.crd(), maxGain.primaryStat(),
                maxGain.att(), maxGain.crr(), NumberUtil.decimalFormat("#.##%", differ), point - pointUse.apply(maxGain));
        return Optional.of(reply);


    }


//    public static void main(String[] args) {
//        CurrentStat currentStat = new CurrentStat(422, 200, 0.9443f, 128.1f, 72646, 9790, 3454, 100, true);
//        CurrentLevel currentLevel = new CurrentLevel(0, 0, 0, 0, 0, 0, 0, 1449);
//        HyperStatMessageHandler m = new HyperStatMessageHandler(null);
//        String jsonString = "{'raw_message': '#380', 'group_id': '667082876', 'sender': {'user_id': '978312456','open_id': '123'}, 'message_id': '46393'}";
//        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
//        MessageDTO dto = MessageDTO.fromJson(jsonObject);
//        Optional<String> s = m.executeCalculate(dto, currentStat, currentLevel);
//        System.out.println(s.orElse("error"));
//    }
}

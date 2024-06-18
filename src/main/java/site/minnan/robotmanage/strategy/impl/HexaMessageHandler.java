package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.*;
import java.util.stream.Stream;

/**
 * 六转核心数量计算
 *
 * @author Minnan on 2024/03/12
 */
@Component("hexa")
public class HexaMessageHandler implements MessageHandler {

    /**
     * 起源技能核心需求数量
     */
    private static final int[] USAGE_SKILL = new int[]{
            0, 0, 30, 35, 40, 45, 50, 55, 60, 65, 200,
            80, 90, 100, 110, 120, 130, 140, 150, 160, 350,
            170, 180, 190, 200, 210, 220, 230, 240, 250, 500
    };

    /**
     * 精通技能核心需求数量
     */
    private static final int[] USAGE_MASTERY = new int[]{
            0, 50, 15, 18, 20, 23, 25, 28, 30, 33, 100,
            40, 45, 50, 55, 60, 65, 70, 75, 80, 175,
            85, 90, 95, 100, 105, 110, 115, 120, 125, 250
    };

    /**
     * 强化技能核心需求数量
     */
    private static final int[] USAGE_ENHANCE = new int[]{
            0, 75, 23, 27, 30, 34, 38, 42, 45, 49, 150,
            60, 68, 75, 83, 90, 98, 105, 113, 120, 263,
            128, 135, 143, 150, 158, 165, 173, 180, 188, 375
    };

    private static final int[] USAGE_COMMON = new int[]{
            0, 125, 38, 44, 50, 57, 63, 69, 75, 72, 300,
            110, 124, 138, 152, 165, 179, 193, 207, 220,
            525, 234, 248, 262, 275, 289, 303, 317, 330, 344, 750
    };

    /**
     * 储存六转各技能信息
     *
     * @param usageMatrix 需求数量矩阵
     * @param usageTotal  各技能需求总量
     * @param label
     * @param grandTotal  总需求量
     */
    private record HexaInfo(int[][] usageMatrix, int[] usageTotal, String[] label, int grandTotal) {
    }

    //newAge版本六转信息
//    private static final HexaInfo newAgeHexa;
//
//    //梦都版本六转信息
//    private static final HexaInfo dreamerHexa;

    private static final Map<String, HexaInfo> hexaVersion;

    private record Range(int start, int end) {
    }

    //第一层键，版本名称，第二层键，分技能查询的名称，
    private static final Map<String, Map<String, Range>> partInfoMap;


    /**
     * 需求数量矩阵
     */
//    private static final int[][] USAGE_MATRIX = new int[][]{USAGE_SKILL, USAGE_MASTERY, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE};

//    private static final String[] LABEL = new String[]{"起源", "精通", "V1", "V2", "V3", "V4"};

    /**
     * 各技能需求总量
     */
//    private static final int[] USAGE_TOTAL;

    /**
     * 总需求量
     */
//    private static final int GRAND_TOTAL;

    private static final String VERSION_NEW_AGE = "newAge";

    private static final String VERSION_DREAMER = "dreamer";

    static {
        int skillTotal = Arrays.stream(USAGE_SKILL).sum();
        int masteryTotal = Arrays.stream(USAGE_MASTERY).sum();
        int enhanceTotal = Arrays.stream(USAGE_ENHANCE).sum();
        int commonTotal = Arrays.stream(USAGE_COMMON).sum();

        HexaInfo newAgeHexa = new HexaInfo(new int[][]{USAGE_SKILL, USAGE_MASTERY, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE},
                new int[]{skillTotal, masteryTotal, enhanceTotal, enhanceTotal, enhanceTotal, enhanceTotal},
                new String[]{"起源", "精通", "V1", "V2", "V3", "V4"},
                skillTotal + masteryTotal + enhanceTotal * 4);

        HexaInfo dreamerHexa = new HexaInfo(new int[][]{USAGE_SKILL, USAGE_MASTERY, USAGE_MASTERY, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_COMMON},
                new int[]{skillTotal, masteryTotal, masteryTotal, enhanceTotal, enhanceTotal, enhanceTotal, enhanceTotal, commonTotal},
                new String[]{"起源", "精通1", "精通2", "V1", "V2", "V3", "V4", "通用"},
                skillTotal + masteryTotal * 2 + enhanceTotal * 4 + commonTotal);

        hexaVersion = new HashMap<>();
        hexaVersion.put(VERSION_NEW_AGE, newAgeHexa);
        hexaVersion.put(VERSION_DREAMER, dreamerHexa);
//        GRAND_TOTAL = skillTotal + masteryTotal + enhanceTotal * 4;
//        USAGE_TOTAL = new int[]{skillTotal, masteryTotal, enhanceTotal, enhanceTotal, enhanceTotal, enhanceTotal};
        Map<String, Range> partNewAge = MapBuilder.create(new HashMap<String, Range>())
                .put("H", new Range(0, 1))
                .put("精通", new Range(1, 2))
                .put("V", new Range(2, 6))
                .build();

        Map<String, Range> partDreamer = MapBuilder.create(new HashMap<String, Range>())
                .put("H", new Range(0, 1))
                .put("精通", new Range(1, 3))
                .put("V", new Range(3, 7))
                .put("通用", new Range(7, 8))
                .build();

        partInfoMap = new HashMap<>();
        partInfoMap.put(VERSION_NEW_AGE, partNewAge);
        partInfoMap.put(VERSION_DREAMER, partDreamer);


    }


    @Value("${game_version:newAge}")
    private String gameVersion;

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        String param = message.toLowerCase().replace("hexa", "").strip();
        String[] paramString = param.split("\\s+");
        if (!NumberUtil.isNumber(paramString[0])) {
            return partCalculator(paramString);
        }

        HexaInfo hexaInfo = hexaVersion.get(gameVersion);


        if (paramString.length != hexaInfo.usageTotal.length) {
            return Optional.of(invalidTip());
        }
        for (String l : paramString) {
            if (!NumberUtil.isNumber(l)) {
                return Optional.of(invalidTip());
            }
        }
        int[] levels = Stream.of(paramString).mapToInt(Integer::parseInt).toArray();

        List<String> replyMessage = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < levels.length; i++) {
            int level = levels[i];
            if (level < 0 || level > 30) {
                return Optional.of(invalidTip());
            }
            int[] usage = hexaInfo.usageMatrix[i];
            int totalSpent = Arrays.stream(ArrayUtil.sub(usage, 0, level + 1)).sum();
            total += totalSpent;
            int totalNeed = hexaInfo.usageTotal[i];
            int needToMax = totalNeed - totalSpent;
            String process = NumberUtil.formatPercent((double) totalSpent / totalNeed, 2);
            String line = "%s: %d/%d，还需%d个小核升满，进度%s".formatted(hexaInfo.label[i], totalSpent, totalNeed, needToMax, process);
            replyMessage.add(line);
        }
        int grandTotal = hexaInfo.grandTotal;
        String totalProcess = NumberUtil.formatPercent((double) total / grandTotal, 2);
        String line = "总进度: %d/%d，还需%d个小核升满，进度%s".formatted(total, grandTotal, grandTotal - total, totalProcess);
        replyMessage.add(line);
        return Optional.of("\n" + String.join("\n", replyMessage));
    }


    /**
     * 局部计算六转核心
     *
     * @param paramString
     * @return
     */
    public Optional<String> partCalculator(String[] paramString) {
        Map<String, Range> detailMap = partInfoMap.get(gameVersion);
        HexaInfo hexaInfo = hexaVersion.get(gameVersion);

        String partName = paramString[0].toUpperCase();
        Range range = detailMap.get(partName);
        if (range == null) {
            return Optional.of(invalidTip());
        }
        if (paramString.length - 1 != range.end - range.start) {
            return Optional.of(invalidTip());
        }
        for (int i = 1; i < paramString.length; i++) {
            if (!NumberUtil.isNumber(paramString[i])) {
                return Optional.of(invalidTip());
            }
        }
        int[] levels = Stream.of(paramString).skip(1).mapToInt(Integer::parseInt).toArray();

        List<String> replyMessage = new ArrayList<>();
        for (int i = 0; i < levels.length; i++) {
            int level = levels[i];
            if (level < 0 || level > 30) {
                return Optional.of(invalidTip());
            }
            int[] usage = hexaInfo.usageMatrix[i + range.start];
            int totalSpent = Arrays.stream(ArrayUtil.sub(usage, 0, level + 1)).sum();
            int totalNeed = hexaInfo.usageTotal[i + range.start];
            int needToMax = totalNeed - totalSpent;
            String process = NumberUtil.formatPercent((double) totalSpent / totalNeed, 2);
            String line = "%s: %d/%d，还需%d个小核升满，进度%s".formatted(hexaInfo.label[i + range.start], totalSpent, totalNeed, needToMax, process);
            replyMessage.add(line);
        }
        return Optional.of("\n" + String.join("\n", replyMessage));
    }

    /**
     * 计算参数错误提示
     * @return
     */
    public String invalidTip() {
        if (VERSION_NEW_AGE.equals(gameVersion)) {
            return """
                    请输入正确的计算参数
                    整体计算：hexa 起源 精通 V1 V2 V3 V4
                    局部计算：
                    起源：hexah 起源等级
                    精通：hexa精通 精通等级
                    V技能：hexav V1 V2 V3 V4
                    """;
        } else if (VERSION_DREAMER.equals(gameVersion)) {
            return """
                    请输入正确的计算参数
                    整体计算：hexa 起源 精通1 精通2 V1 V2 V3 V4 通用
                    局部计算：
                    起源：hexah 起源等级
                    精通：hexa精通 精通1等级 精通2等级
                    V技能：hexav V1 V2 V3 V4
                    通用：hexa通用 通用等级
                    """;
        } else {
            return """
                    请输入正确的计算参数
                    整体计算：hexa 起源 精通1 精通2 V1 V2 V3 V4 通用
                    局部计算：
                    起源：hexah 起源等级
                    精通：hexa精通 精通1等级 精通2等级
                    V技能：hexav V1 V2 V3 V4
                    通用：hexa通用 通用等级
                    """;
        }
    }
}

package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.jsoup.internal.StringUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    /**
     * 需求数量矩阵
     */
    private static final int[][] USAGE_MATRIX = new int[][]{USAGE_SKILL, USAGE_MASTERY, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE, USAGE_ENHANCE};

    private static final String[] LABEL = new String[]{"起源", "精通", "V1", "V2", "V3", "V4"};

    /**
     * 各技能需求总量
     */
    private static final int[] USAGE_TOTAL;

    /**
     * 总需求量
     */
    private static final int GRAND_TOTAL;

    static {
        int skillTotal = Arrays.stream(USAGE_SKILL).sum();
        int masteryTotal = Arrays.stream(USAGE_MASTERY).sum();
        int enhanceTotal = Arrays.stream(USAGE_ENHANCE).sum();
        GRAND_TOTAL = skillTotal + masteryTotal + enhanceTotal * 4;
        USAGE_TOTAL = new int[]{skillTotal, masteryTotal, enhanceTotal, enhanceTotal, enhanceTotal, enhanceTotal};
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
        String param = message.replace("hexa", "").strip();
        String[] levelString = param.split("\\s+");

        if (levelString.length != 6) {
            return Optional.of("请输入正确的计算参数");
        }
        for (String l : levelString) {
            if (!NumberUtil.isNumber(l)) {
                return Optional.of("请输入正确的计算参数");
            }
        }
        int[] levels = Stream.of(levelString).mapToInt(Integer::parseInt).toArray();

        List<String> replyMessage = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < levels.length; i++) {
            int level = levels[i];
            if (level < 0 || level > 30) {
                return Optional.of("请输入正确的计算参数");
            }
            int[] usage = USAGE_MATRIX[i];
            int totalSpent = Arrays.stream(ArrayUtil.sub(usage, 0, level + 1)).sum();
            total += totalSpent;
            int totalNeed = USAGE_TOTAL[i];
            int needToMax = totalNeed - totalSpent;
            String process = NumberUtil.formatPercent((double) totalSpent / totalNeed, 2);
            String line = "%s: %d/%d，还需%d个小核升满，进度%s".formatted(LABEL[i], totalSpent, totalNeed, needToMax, process);
            replyMessage.add(line);
        }
        String totalProcess = NumberUtil.formatPercent((double) total / GRAND_TOTAL, 2);
        String line = "总进度: %d/%d，还需%d个小核升满，进度%s".formatted(total, GRAND_TOTAL, GRAND_TOTAL - total, totalProcess);
        replyMessage.add(line);
        return Optional.of("\n" + String.join("\n", replyMessage));
    }

}

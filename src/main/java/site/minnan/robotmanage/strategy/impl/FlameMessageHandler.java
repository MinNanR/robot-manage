package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.FlameCost;
import site.minnan.robotmanage.entity.dao.FlameCostRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.FlameCalculator;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 火花消息处理器
 *
 * @author Minnan on 2024/01/12
 */
@Component("flame")
public class FlameMessageHandler implements MessageHandler {

    private final FlameCostRepository flameCostRepository;

    public FlameMessageHandler(FlameCostRepository flameCostRepository) {
        this.flameCostRepository = flameCostRepository;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String rawMessage = dto.getRawMessage();
        String param = rawMessage.substring(4).strip();
        String result = null;
        if (rawMessage.startsWith("火花期望")) {
            result = handleExpect(Float.valueOf(param));
        } else if (rawMessage.startsWith("火花花费")) {
            int target = parseTarget(param, 3, 9);
            result = handelCost(target);
        } else if (rawMessage.startsWith("火花需求")) {
            result = "\n" + handleRequire(param);
        }
        return Optional.ofNullable(result);
    }

    private String handleExpect(Float expect) {
        List<FlameCost> flameCostList = flameCostRepository.findExpect(expect * 1000);
        String content = flameCostList.stream().map(FlameCost::expectationFormat)
                .collect(Collectors.joining("\n"));
        return """
                花费：%.1fB
                %s
                以上数据均使用红火，单价950万计算
                🐱⑨：为简化小数点，all记9，att记3，副属不记
                """.formatted(expect, content);
    }

    private String handelCost(Integer target) {
        List<FlameCost> flameCostList = flameCostRepository.findCost(target);
        String content = flameCostList.stream().map(FlameCost::costFormat)
                .collect(Collectors.joining("\n"));
        return """
                目标：%d
                %s
                以上数据均使用红火，单价950万计算
                🐱⑨：为简化小数点，all记9，att记3，副属不记
                """.formatted(target, content);
    }

    private int parseTarget(String param, double attTransfer, double allTransfer) {
        String[] paramSplit = param.replaceAll("\\s", "").split("\\+");

        String firstParam = paramSplit[0];
        int number;
        if (firstParam.contains("%")) {
            number = (int) Math.ceil(Integer.parseInt(firstParam.replace("%", "")) * allTransfer);
        } else if (firstParam.contains("A") || firstParam.contains("a")) {
            number = (int) Math.ceil(Integer.parseInt(firstParam.replace("(?i)a", "")) * attTransfer);
        } else {
            number = Integer.parseInt(firstParam);
        }

        if (paramSplit.length == 3) {
            int percentage = Integer.parseInt(paramSplit[1].replace("%", ""));
            int att = Integer.parseInt(paramSplit[2].replaceAll("(?i)a", ""));
            return (int) Math.ceil(number + percentage * allTransfer + att * attTransfer);
        }
        if (paramSplit.length == 2) {
            String secondParam = paramSplit[1];
            if (secondParam.endsWith("a") || secondParam.endsWith("A")) {
                int att = Integer.parseInt(secondParam.replaceAll("(?i)a", ""));
                number = number + (int) Math.ceil(att * attTransfer);
            } else {
                int percentage = Integer.parseInt(secondParam.replaceAll("%", ""));
                number = number + (int) Math.ceil(percentage * allTransfer);
            }
            return number;
        }
        return number;
    }


    private static final String attRegex = "att=(\\d+(?:\\.\\d+)?)";
    private static final String allRegex = "all=(\\d+(?:\\.\\d+)?)";
    private static final List<Integer> supportedItemLevel = List.of(250, 200, 160,150, 140);

    private String handleRequire(String paramString) {
        String[] paramArray = paramString.split("\\s");
        if (paramArray.length < 2) {
            return "请输入火花目标和装备等级";
        }
        //计算目标内容
        String targetString = paramArray[0];
        //装备等级
        int itemLevel = Integer.parseInt(paramArray[1]);
        if (!supportedItemLevel.contains(itemLevel)) {
            String s = supportedItemLevel.stream()
                    .map(Object::toString).collect(Collectors.joining("、"));
            return "装备等级仅支持" + s;
        }

        //转换值
        Double att = null;
        Double all = null;
        //使用的道具
        FlameCalculator.FlameProp prop = null;
        for (int i = 2; i < paramArray.length; i++) {
            String param = paramArray[i];
            if (ReUtil.isMatch(attRegex, param)) {
                String number = ReUtil.get(attRegex, param, 1);
                att = Double.parseDouble(number);
            } else if (ReUtil.isMatch(allRegex, param)) {
                String number = ReUtil.get(allRegex, param, 1);
                all = Double.parseDouble(number);
            } else if ("红火".equals(param)) {
                prop = FlameCalculator.POWERFUL_FLAME;
            } else if ("彩火".equals(param)) {
                prop = FlameCalculator.ETERNAL_FLAME;
            } else if ("紫火".equals(param)) {
                prop = FlameCalculator.ABYSS_FLAME;
            }
        }
        att = att == null ? 3 : att;
        all = all == null ? 11 : all;
        prop = prop == null ? FlameCalculator.ETERNAL_FLAME : prop;
        FlameCalculator.ScoreTransfer transfer = new FlameCalculator.ScoreTransfer(att, all);
        FlameCalculator calculator = new FlameCalculator(prop, itemLevel, transfer);
        int target = parseTarget(targetString, att, all);
        FlameCalculator.Result calculateResult = calculator.process(target);
        if (calculateResult == FlameCalculator.INFEASIBLE) {
            return """
                    目标：%d
                    使用道具：%s
                    无法达到
                    """.formatted(target, prop.label());
        } else {
            return """
                    目标：%d
                    使用道具：%s
                    平均数：%s
                    中位数：%s
                    P75：%s
                    P95：%s
                    """.formatted(target, prop.label(),
                    NumberUtil.decimalFormat(",###", calculateResult.avg()),
                    NumberUtil.decimalFormat(",###", calculateResult.median()),
                    NumberUtil.decimalFormat(",###", calculateResult.P75()),
                    NumberUtil.decimalFormat(",###", calculateResult.P95()));
        }
    }

    public static void main(String[] args) {
        FlameMessageHandler handler = new FlameMessageHandler(null);
        MessageDTO dto = new MessageDTO();
        dto.setRawMessage("火花需求114+6 250 all=13 紫火");
        Optional<String> s = handler.handleMessage(dto);
        System.out.println(s.orElse("fail"));
    }

}

package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 属性计算器
 *
 * @author Minnan on 2024/03/22
 */
@Component("stat")
public class StatMessageHandler implements MessageHandler {


    private static final BigDecimal ONE_PERCENT = new BigDecimal("0.01");

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        String[] paramSplit = message
                .replaceAll("[!！]", "").split("\\s+");
        if ("all".equalsIgnoreCase(paramSplit[0])) {
            BigDecimal baseValue = new BigDecimal(paramSplit[1]);
            BigDecimal percentageValue = new BigDecimal(paramSplit[2]);
//            percentageValue = percentageValue.compareTo(BigDecimal.TEN) > 0 ? percentageValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN) : percentageValue;
            percentageValue = percentageValue.compareTo(BigDecimal.TEN) > 0 ? percentageValue : percentageValue.multiply(new BigDecimal(100));
            String result = allToJobStat(baseValue, percentageValue);
            String reply = """
                    %s
                    此计算结果仅为1all对应主属面板的变化，实际伤害计算时还会加上副属性计算，所以实际值比计算结果要更高一点
                    """.formatted(result);
            return Optional.of("\n" + reply);
        } else if ("alls".equalsIgnoreCase(paramSplit[0])) {
            BigDecimal baseMain = new BigDecimal(paramSplit[1]);
            BigDecimal percentageMain = new BigDecimal(paramSplit[2]);
            percentageMain = percentageMain.compareTo(BigDecimal.TEN) > 0 ? percentageMain.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN) : percentageMain;
            String[] secondary = ArrayUtil.sub(paramSplit, 2, paramSplit.length);
            List<BigDecimal> secondaryList = Arrays.stream(secondary)
                    .map(BigDecimal::new)
                    .collect(Collectors.toList());
            String result = allToStatValue(baseMain, percentageMain, secondaryList);
            return Optional.of(result);
        }
        return Optional.empty();
    }


    /**
     * 计算每1all=多少主属
     *
     * @param baseValue       基础数值总值
     * @param percentageValue 潜能百分比总值
     * @return
     */
    public String allToJobStat(BigDecimal baseValue, BigDecimal percentageValue) {
        BigDecimal equivalent = baseValue.divide(BigDecimal.valueOf(100).add(percentageValue), 4, RoundingMode.FLOOR);
        return "1all=%s".formatted(NumberUtil.decimalFormat("#.##", equivalent));
    }

    private String allToStatValue(BigDecimal baseMain, BigDecimal mainPercentage, List<BigDecimal> baseSecondary) {
        BigDecimal mainDiffer = baseMain.multiply(BigDecimal.ONE.add(mainPercentage).add(ONE_PERCENT)).multiply(BigDecimal.valueOf(4));
        BigDecimal secondaryDiffer = baseSecondary.stream().map(e -> e.multiply(ONE_PERCENT)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal differ = mainDiffer.add(secondaryDiffer);

        BigDecimal firstPart = differ.divide(BigDecimal.valueOf(4).multiply(BigDecimal.ONE.add(mainPercentage)), 4, RoundingMode.FLOOR);
        BigDecimal result = firstPart.subtract(baseMain);
        return "1all=%s".formatted(NumberUtil.decimalFormat("#.##", result));
    }
}

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

    private BigDecimal ensurePercentage (BigDecimal n) {
        return n.compareTo(BigDecimal.TEN) > 0 ? n.multiply(ONE_PERCENT) : n;
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
        String[] paramSplit = message
                .replaceAll("[!！]", "").split("\\s+");
        if ("%".equalsIgnoreCase(paramSplit[0])) {
            //!% 主属基础 主属百分比
            BigDecimal baseValue = new BigDecimal(paramSplit[1]);
            BigDecimal percentageValue = new BigDecimal(paramSplit[2]);
//            percentageValue = percentageValue.compareTo(BigDecimal.TEN) > 0 ? percentageValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN) : percentageValue;
            percentageValue = percentageValue.compareTo(BigDecimal.TEN) > 0 ? percentageValue : percentageValue.multiply(new BigDecimal(100));
            String result = percentToMainStat(baseValue, percentageValue);
            return Optional.of(result);
        } else if ("all".equalsIgnoreCase(paramSplit[0]) || "alls".equalsIgnoreCase(paramSplit[0])) {
            //!all 主属基础 主属百分比 副属基础
            BigDecimal baseMain = new BigDecimal(paramSplit[1]);
            BigDecimal percentageMain = new BigDecimal(paramSplit[2]);
            percentageMain = ensurePercentage(percentageMain);
            String[] secondary = ArrayUtil.sub(paramSplit, 3, paramSplit.length);
            List<BigDecimal> secondaryList = Arrays.stream(secondary)
                    .map(BigDecimal::new)
                    .collect(Collectors.toList());
            String result = allToMainStat(baseMain, percentageMain, secondaryList);
            return Optional.of(result);
        } else if ("att".equalsIgnoreCase(paramSplit[0])) {
            //!att 基础攻击力 主属百分比 总主属 总副属1 总副属2
            BigDecimal baseAtt = new BigDecimal(paramSplit[1]);
            BigDecimal percentageMain = new BigDecimal(paramSplit[2]);
            percentageMain = ensurePercentage(percentageMain);
            BigDecimal mainTotal = new BigDecimal(paramSplit[3]);
            String[] secondary = ArrayUtil.sub(paramSplit, 4, paramSplit.length);
            List<BigDecimal> secondaryList = Arrays.stream(secondary)
                    .map(BigDecimal::new)
                    .collect(Collectors.toList());
            String result = attToMainStat(baseAtt, percentageMain, mainTotal, secondaryList);
            return Optional.of(result);
        } else if ("att-bd".equalsIgnoreCase(paramSplit[0])) {
            //!att-bd 攻击力百分比 bd dmg
            BigDecimal attPercent = new BigDecimal(paramSplit[1]);
            BigDecimal bd = new BigDecimal(paramSplit[2]);
            BigDecimal dmg = new BigDecimal(paramSplit[3]);
            attPercent = ensurePercentage(attPercent);
            bd = ensurePercentage(bd);
            dmg = ensurePercentage(dmg);
            String result = attPercentToDmg(attPercent, bd, dmg);
            return Optional.of(result);
        }
        return Optional.empty();
    }


    /**
     * 计算每1%=多少主属
     *
     * @param baseValue       基础数值总值
     * @param percentageValue 潜能百分比总值
     * @return
     */
    public String percentToMainStat(BigDecimal baseValue, BigDecimal percentageValue) {
        BigDecimal equivalent = baseValue.divide(BigDecimal.valueOf(100).add(percentageValue), 4, RoundingMode.FLOOR);
        return "1%%=%s".formatted(NumberUtil.decimalFormat("#.##", equivalent));
    }

    private String allToMainStat(BigDecimal baseMain, BigDecimal mainPercentage, List<BigDecimal> baseSecondary) {
        BigDecimal mainDiffer = baseMain.multiply(BigDecimal.ONE.add(mainPercentage).add(ONE_PERCENT)).multiply(BigDecimal.valueOf(4));
        BigDecimal secondaryDiffer = baseSecondary.stream().map(e -> e.multiply(ONE_PERCENT)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal differ = mainDiffer.add(secondaryDiffer);

        BigDecimal firstPart = differ.divide(BigDecimal.valueOf(4).multiply(BigDecimal.ONE.add(mainPercentage)), 4, RoundingMode.FLOOR);
        BigDecimal result = firstPart.subtract(baseMain);
        return "1all=%s".formatted(NumberUtil.decimalFormat("#.##", result));
    }

    /**
     * 攻击力转主属
     *
     * @param baseAtt        基础攻击力
     * @param mainPercentage 主属百分比
     * @param mainTotal      总主属
     * @param secondaryTotal 总副属
     * @return
     */
    private String attToMainStat(BigDecimal baseAtt, BigDecimal mainPercentage, BigDecimal mainTotal, List<BigDecimal> secondaryTotal) {
        BigDecimal secondaryValue = secondaryTotal.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal statValue = mainTotal.multiply(BigDecimal.valueOf(4)).add(secondaryValue);

        BigDecimal result = statValue.divide(BigDecimal.valueOf(4).multiply(BigDecimal.ONE.add(mainPercentage)).multiply(baseAtt), 4, RoundingMode.HALF_UP);
        return "1att=%s".formatted(NumberUtil.decimalFormat("#.##", result));
    }


    /**
     * 攻击力百分比转BD
     *
     * @param attPercent 攻击力百分比
     * @param bd BD
     * @param dmg dmg
     * @return
     */
    private String attPercentToDmg(BigDecimal attPercent, BigDecimal bd, BigDecimal dmg) {
        BigDecimal up = BigDecimal.ONE.add(bd).add(dmg);
        BigDecimal down = BigDecimal.ONE.add(attPercent);
        BigDecimal result = up.divide(down, 4, RoundingMode.HALF_UP);
        return "1%%att=%sbd/dmg".formatted(NumberUtil.decimalFormat("#.##", result));
    }
}

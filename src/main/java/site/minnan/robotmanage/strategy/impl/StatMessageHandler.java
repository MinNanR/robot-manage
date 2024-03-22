package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

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
            percentageValue = percentageValue.compareTo(BigDecimal.TEN) > 0 ? percentageValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_DOWN) : percentageValue;
            String result = allToJobStat(baseValue, percentageValue);
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
        BigDecimal currentValue = baseValue.multiply(BigDecimal.ONE.add(percentageValue));

        BigDecimal newValue = baseValue.multiply(BigDecimal.ONE.add(percentageValue).add(ONE_PERCENT));

        BigDecimal valueDiffer = newValue.subtract(currentValue);
        BigDecimal equivalent = valueDiffer.divide(BigDecimal.ONE.add(percentageValue), 4, RoundingMode.HALF_UP);
        return "1all=%s".formatted(NumberUtil.decimalFormat("#.##", equivalent));
    }
}

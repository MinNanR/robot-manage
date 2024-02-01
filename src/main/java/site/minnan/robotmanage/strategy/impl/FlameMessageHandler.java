package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.FlameCost;
import site.minnan.robotmanage.entity.dao.FlameCostRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
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
            int target = parseTarget(param);
            result = handelCost(target);
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

    private int parseTarget(String param) {
        String[] paramSplit = param.replaceAll("\\s", "").split("\\+");

        String firstParam = paramSplit[0];
        int number;
        if (firstParam.contains("%")) {
            number = Integer.parseInt(firstParam.replace("%", "")) * 9;
        } else if (firstParam.contains("A") || firstParam.contains("a")) {
            number = Integer.parseInt(firstParam.replace("(?i)a", "")) * 3;
        } else {
            number = Integer.parseInt(firstParam);
        }

        if (paramSplit.length == 3) {
            int percentage = Integer.parseInt(paramSplit[1].replace("%", ""));
            int att = Integer.parseInt(paramSplit[2].replaceAll("(?i)a", ""));
            return number + percentage * 9 + att * 3;
        }
        if (paramSplit.length == 2) {
            String secondParam = paramSplit[1];
            if (secondParam.endsWith("a") || secondParam.endsWith("A")) {
                int att = Integer.parseInt(secondParam.replaceAll("(?i)a", ""));
                number = number + att * 3;
            } else {
                int percentage = Integer.parseInt(secondParam.replaceAll("%", ""));
                number = number + percentage * 9;
            }
            return number;
        }
        return number;
    }

}

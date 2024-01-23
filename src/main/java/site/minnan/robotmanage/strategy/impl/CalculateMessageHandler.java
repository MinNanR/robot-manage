package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.NumberUtil;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 计算器消息处理
 *
 * @author Minnan on 2024/01/17
 */
@Service("calculate")
public class CalculateMessageHandler implements MessageHandler {


    //运算操作处理方法映射
    public static final Map<String, BiFunction<BigDecimal, BigDecimal, BigDecimal>> operateMap;

    //运算符优先级表
    public static final Map<String, Integer> operatorPriority;

    static {
        operateMap = new HashMap<>();
        operateMap.put("+", BigDecimal::add);
        operateMap.put("-", BigDecimal::subtract);
        operateMap.put("*", BigDecimal::multiply);
        operateMap.put("/", (operand1, operand2) -> operand1.divide(operand2,6, RoundingMode.HALF_UP));
        operateMap.put("mod", (operand1, operand2) -> operand1.divideAndRemainder(operand2)[1]);

        operatorPriority = new HashMap<>();
        operatorPriority.put("+", 1);
        operatorPriority.put("-", 1);
        operatorPriority.put("*", 2);
        operatorPriority.put("/", 2);
        operatorPriority.put("mod", 2);
        operatorPriority.put("(", 3);
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String formula = dto.getRawMessage();
        formula = formula.toLowerCase().replace(" ", "");

        Stack<String> operatorStack = new Stack<>();
        List<ExpressionItem> suffixExpression = new ArrayList<>();

        Supplier<String> top = operatorStack::peek;

        int i = 0;
        if (formula.charAt(i) == '-') {
            NumberResult findResult = findNumber(formula.substring(i + 1));
            i = i + findResult.length() + 1;
            suffixExpression.add(new ExpressionItem(findResult.number().negate()));
        }

        int length = formula.length();
        while (i < length) {
            String s = String.valueOf(formula.charAt(i));
            if (s.isBlank()) {
                i = i + 1;
                continue;
            }
            if (NumberUtil.isNumber(s)) {
                NumberResult findResult = findNumber(formula.substring(i));
                i = i + findResult.length - 1;
                suffixExpression.add(new ExpressionItem(findResult.number()));
            } else if ("(".equals(s)) {
                operatorStack.push(s);
                if (i + 2 < length && formula.charAt(i + 1) == '-') {
                    NumberResult findResult = findNumber(formula.substring(i + 2));
                    i = i + findResult.length() + 1;
                    suffixExpression.add(new ExpressionItem(findResult.number().negate()));
                }
            } else if (")".equals(s)) {
                while (true) {
                    if (operatorStack.isEmpty()) {
                        return Optional.of("请输入正确的表达式");
                    }
                    String operator = operatorStack.pop();
                    if ("(".equals(operator)) {
                        break;
                    }
                    suffixExpression.add(new ExpressionItem(operator));
                }
            } else {
                if (operatorStack.isEmpty() || "(".equals(top.get())) {
                    operatorStack.push(s);
                } else {
                    while (!operatorStack.isEmpty()
                            && operatorPriority.get(s) <= operatorPriority.get(top.get())
                            && !"(".equals(top.get())) {
                        String operator = operatorStack.pop();
                        suffixExpression.add(new ExpressionItem(operator));
                    }
                    operatorStack.push(s);
                }
            }
            i = i + 1;
        }

        while (!operatorStack.isEmpty()) {
            String operator = operatorStack.pop();
            if ("(".equals(operator)) {
                return Optional.empty();
            }
            suffixExpression.add(new ExpressionItem(operator));
        }

        Stack<BigDecimal> operandStack = new Stack<>();
        for (ExpressionItem item : suffixExpression) {
            if (item.type == ExpressionItemType.OPERATOR) {
                BigDecimal operand2 = operandStack.pop();
                BigDecimal operand1 = operandStack.pop();
                String operator = item.operator;
                BigDecimal result = operateMap.get(operator).apply(operand1, operand2);
                operandStack.push(result);
            } else {
                operandStack.push(item.number);
            }
        }

        if (operatorStack.size() > 1) {
            return Optional.empty();
        }
        BigDecimal result = operandStack.pop();
        if (result.scale() <=0 || result.stripTrailingZeros().scale() <= 0) {
            return Optional.of(String.valueOf(result.intValue()));
        } else {
            return Optional.of(result.toString());
        }

    }

    private NumberResult findNumber(String formula) {
        StringBuilder digitStrBuilder = new StringBuilder();
        String[] charArray = formula.split("");
        int length = charArray.length;
        for (int i = 0; i < length; i++) {
            String s = charArray[i];
            if (NumberUtil.isNumber(s) || ".".equals(s)) {
                digitStrBuilder.append(s);
            } else if ("%".equals(s)) {
                i = i + 1;
                String digitStr = digitStrBuilder.toString();
                BigDecimal number = new BigDecimal(digitStr).divide(BigDecimal.valueOf(100));
                return new NumberResult(number, i);
            } else {
                String digitStr = digitStrBuilder.toString();
                BigDecimal number = new BigDecimal(digitStr);
                return new NumberResult(number, i);
            }
        }
        String digitStr = digitStrBuilder.toString();
        BigDecimal number = new BigDecimal(digitStr);
        return new NumberResult(number, length);
    }

    private record NumberResult(BigDecimal number, int length) {
    }

    private static class ExpressionItem {
        BigDecimal number;
        String operator;
        ExpressionItemType type;

        public ExpressionItem(BigDecimal number) {
            this.number = number;
            this.type = ExpressionItemType.NUMBER;
        }

        public ExpressionItem(String operator) {
            this.operator = operator;
            this.type = ExpressionItemType.OPERATOR;
        }
    }

    /**
     * 表达元素类型
     */
    private enum ExpressionItemType {
        //数字
        NUMBER,
        //操作符
        OPERATOR
    }

}

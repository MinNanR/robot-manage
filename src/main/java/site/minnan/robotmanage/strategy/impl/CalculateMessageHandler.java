package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 计算器消息处理
 *
 * @author Minnan on 2024/01/17
 */
@Component("calculate")
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
        operateMap.put("/", (operand1, operand2) -> operand1.divide(operand2, 6, RoundingMode.HALF_UP));
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
     * 解析运算符表达式，支持特殊表达式：开方，幂运算，2/8/16进制互逆
     * 普通四则运算表达式解析思想为转换为后缀表达式，使用后缀表达式进行计算
     * 目前已知问题：括号内接副号再接一个括号无法解析，如1+(-(3*2))，后续处理
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String formula = dto.getRawMessage();
        formula = formula.replace("=", "");
        formula = formula.toLowerCase().replace(" ", "").replaceAll("（", "(").replaceAll("）", ")");
        Function<Double, Optional<String>> specialFormula = d -> Optional.of(NumberUtil.decimalFormat("#.####", d));

        if (formula.toLowerCase().startsWith("pow")) {
            Pattern powPattern = Pattern.compile("pow\\((.*?),(.*?)\\)");
            List<String> powParam = ReUtil.getAllGroups(powPattern, formula);
            double base = Double.parseDouble(powParam.get(1));
            double exponent = Double.parseDouble(powParam.get(2));
            return specialFormula.apply(Math.pow(base, exponent));
        } else if (formula.toLowerCase().startsWith("sqr") || formula.toLowerCase().startsWith("sqrt")) {
            Pattern sqrPattern = Pattern.compile("sqrt?\\((.*?)\\)");
            String sqrParam = ReUtil.getGroup1(sqrPattern, formula);
            double sqrNumber = Double.parseDouble(sqrParam);
            return specialFormula.apply(Math.sqrt(sqrNumber));
        } else if (formula.toLowerCase().startsWith("bin")) {
            Pattern binPattern = Pattern.compile("bin\\((.*?)\\)");
            String binParam = ReUtil.getGroup1(binPattern, formula);
            String result = Integer.toBinaryString(Integer.parseInt(binParam));
            return Optional.of(result);
        } else if (formula.toLowerCase().startsWith("hex")) {
            Pattern hexPattern = Pattern.compile("hex\\((.*?)\\)");
            String hexParam = ReUtil.getGroup1(hexPattern, formula);
            String result = Integer.toHexString(Integer.parseInt(hexParam));
            return Optional.of(result);
        } else if (formula.toLowerCase().startsWith("oct\\((.*?)\\)")) {
            Pattern octPattern = Pattern.compile("oct\\((.*?)\\)");
            String octParam = ReUtil.getGroup1(octPattern, formula);
            String result = Integer.toOctalString(Integer.parseInt(octParam));
            return Optional.of(result);
        } else if (formula.toLowerCase().startsWith("0b")) {
            return Optional.of(Integer.valueOf(formula.substring(2), 2).toString());
        } else if (formula.toLowerCase().startsWith("0o")) {
            return Optional.of(Integer.valueOf(formula.substring(2), 8).toString());
        } else if (formula.toLowerCase().startsWith("0x")) {
            return Optional.of(Integer.valueOf(formula.substring(2), 16).toString());
        }

        //运算符栈
        Stack<String> operatorStack = new Stack<>();
        //后缀表达式存储
        List<ExpressionItem> suffixExpression = new ArrayList<>();
        //查看运算符栈顶元素
        Supplier<String> top = operatorStack::peek;

        int i = 0;
        //处理表达式第一个数是负数的情况
        if (formula.charAt(i) == '-') {
            NumberResult findResult = findNumber(formula.substring(i + 1));
            i = i + findResult.length() + 1;
            suffixExpression.add(new ExpressionItem(findResult.number().negate()));
        }

        int length = formula.length();
        //循环查看表达式每一个字符
        while (i < length) {
            String s = String.valueOf(formula.charAt(i));
            //空格跳过
            //其实空格问题前面处理过了，但是空格跳过代码比空格替换先写的，所以留着了，也防止有人KR的时候把前面漏了
            if (s.isBlank()) {
                i = i + 1;
                continue;
            }
            //判断这个字符是否为数字
            if (NumberUtil.isNumber(s)) {
                //继续往后查找，查找整个数字
                NumberResult findResult = findNumber(formula.substring(i));
                //步进数字的长度，因为循环末尾还有步进，所以这里步进-1游标移至数字末尾
                i = i + findResult.length - 1;
                //生成表达式元素并添加到后缀表达式中
                suffixExpression.add(new ExpressionItem(findResult.number()));
            } else if ("(".equals(s)) {
                //左括号压入运算符栈
                operatorStack.push(s);
                //处理括号内第一个数字为负数的情况，i + 2 < length保证操作不会越界
                if (i + 2 < length && formula.charAt(i + 1) == '-') {
                    //查询左括号内的第一个数字
                    NumberResult findResult = findNumber(formula.substring(i + 2));
                    //步进数字长度，将游标移至数字末尾，
                    // 查找数字步进长度应该和上一个分支一样，但是这里有一个左括号，有一个负号，所以步进比上一个分支多2步，所以是+1
                    i = i + findResult.length + 1;
                    //取相反数后生成表达式元素并添加到后缀表达式中
                    suffixExpression.add(new ExpressionItem(findResult.number().negate()));
                }
            } else if (")".equals(s)) {
                //如果是右括号，则一直弹出运算符，直至遇到左括号，如果遇到左括号前运算符栈已空则表示输入的是错误表达式，结束解析
                while (true) {
                    if (operatorStack.isEmpty()) {
                        return Optional.of("请输入正确的表达式");
                    }
                    String operator = operatorStack.pop();
                    //遇到左括号则结束循环
                    if ("(".equals(operator)) {
                        break;
                    }
                    //使用将弹出的运算符生成表达式元素并添加到后缀表达式中
                    suffixExpression.add(new ExpressionItem(operator));
                }
            } else {
                //如果不是左右括号，也不是数字，则表示这个字符是运算符
                if (operatorStack.isEmpty() || "(".equals(top.get())) {
                    //运算符栈空，或者栈顶为左括号，则将运算符压入栈中
                    operatorStack.push(s);
                } else {
                    //栈不空或栈顶元素不为左括号，则将栈顶元素弹出并加入后缀表达式，重复操作直至栈空或栈顶元素的运算符优先级比自身优先级高或栈顶是左括号
                    //其实可以不判断是否为左括号，因为左括号是优先级最高的。但是之前左括号优先级是后面加的，懒得删了
                    while (!operatorStack.isEmpty()
                            && operatorPriority.get(s) <= operatorPriority.get(top.get())
                            && !"(".equals(top.get())) {
                        String operator = operatorStack.pop();
                        suffixExpression.add(new ExpressionItem(operator));
                    }
                    //将运算符压入运算符栈
                    operatorStack.push(s);
                }
            }
            //游标步进
            i = i + 1;
        }

        //游标结束后，将运算符栈内所有元素依次弹出加入到后缀表达式中
        //此时运算符栈内不应该右有左括号，因为游标扫描时，遇到右括号就会将左括号弹出，如果此时栈中还有左括号表示左右括号不匹配，输入的表达式错误，结束解析
        while (!operatorStack.isEmpty()) {
            String operator = operatorStack.pop();
            if ("(".equals(operator)) {
                return Optional.of("请输入正确的表达式");
            }
            suffixExpression.add(new ExpressionItem(operator));
        }

        //运算数栈储存
        Stack<BigDecimal> operandStack = new Stack<>();
        //边理后缀表达式
        for (ExpressionItem item : suffixExpression) {
            //如果是运算符，则弹出栈顶两个运算数，注意第一次弹出是运算数2，第二次弹出是运算数1，
            if (item.type == ExpressionItemType.OPERATOR) {
                BigDecimal operand2 = operandStack.pop();
                BigDecimal operand1 = operandStack.pop();
                String operator = item.operator;
                //在运算符操作映射表中查找运算操作，执行运算操作
                BigDecimal result = operateMap.get(operator).apply(operand1, operand2);
                //将运算结果加入运算数栈中
                operandStack.push(result);
            } else {
                //如果是运算数，则将运算数加入运算数栈
                operandStack.push(item.number);
            }
        }

        //表达式扫描结束后，操作数栈应该只剩一个元素，如果多于一个元素说明表达式错误
        if (operandStack.size() > 1) {
            return Optional.of("请输入正确的表达式");
        }
        //获取栈顶元素即为运算结果
        BigDecimal result = operandStack.pop();
        //判断是否以整数形式返回
        if (result.scale() <= 0 || result.stripTrailingZeros().scale() <= 0) {
            return Optional.of(String.valueOf(result.intValue()));
        } else {
            return Optional.of(result.toString());
        }

    }

    /**
     * 查询表达式中第一个数字
     *
     * @param formula
     * @return
     */
    private NumberResult findNumber(String formula) {
        StringBuilder digitStrBuilder = new StringBuilder();
        String[] charArray = formula.split("");
        int length = charArray.length;
        for (int i = 0; i < length; i++) {
            String s = charArray[i];
            //小数点或数字则加入字符串中
            if (NumberUtil.isNumber(s) || ".".equals(s)) {
                digitStrBuilder.append(s);
            } else if ("%".equals(s)) {
                //百分号则除100返回
                i = i + 1;
                String digitStr = digitStrBuilder.toString();
                BigDecimal number = new BigDecimal(digitStr).divide(BigDecimal.valueOf(100));
                return new NumberResult(number, i);
            } else {
                //如果不是数字，小数点，百分号，结束查找并返回
                String digitStr = digitStrBuilder.toString();
                BigDecimal number = new BigDecimal(digitStr);
                return new NumberResult(number, i);
            }
        }
        //循环结束表示是最后一个数字了，同样返回该数字
        String digitStr = digitStrBuilder.toString();
        BigDecimal number = new BigDecimal(digitStr);
        return new NumberResult(number, length);
    }

    /**
     * 查找数字的结果
     *
     * @param number 解析出的数字
     * @param length 数字字符串长度
     */
    private record NumberResult(BigDecimal number, int length) {
    }

    /**
     * 后缀表达式元素
     */
    private static class ExpressionItem {
        BigDecimal number;
        String operator;
        ExpressionItemType type;

        /**
         * 运算数构造器
         *
         * @param number
         */
        public ExpressionItem(BigDecimal number) {
            this.number = number;
            this.type = ExpressionItemType.NUMBER;
        }

        /**
         * 运算符构造器
         *
         * @param operator
         */
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

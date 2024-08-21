package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.community.dialect.pagination.IngresLimitHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
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

    private final IgnoreMessageHandler ignoreMessageHandler;

    private final QuestionRepository questionRepository;

    private final AnswerRepository answerRepository;

    private static final BigDecimal ONE_PERCENT = new BigDecimal("0.01");

    public StatMessageHandler(IgnoreMessageHandler ignoreMessageHandler, QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.ignoreMessageHandler = ignoreMessageHandler;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    private BigDecimal ensurePercentage(BigDecimal n) {
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
        } else if ("ign-bd".equalsIgnoreCase(paramSplit[0])) {
            //!ign-bd BOSS防御力 当前面板无视 bd dmg 追加的无视
            BigDecimal bossDef = new BigDecimal(paramSplit[1]);
            BigDecimal currentIgn = new BigDecimal(paramSplit[2]);
            BigDecimal bd = new BigDecimal(paramSplit[3]);
            BigDecimal dmg = new BigDecimal(paramSplit[4]);
            BigDecimal addIgn = new BigDecimal(paramSplit[5]);
            bossDef = ensurePercentage(bossDef);
            currentIgn = ensurePercentage(currentIgn);
            bd = ensurePercentage(bd);
            dmg = ensurePercentage(dmg);
            addIgn = ensurePercentage(addIgn);
            String result = ignToBd(bossDef, currentIgn, bd, dmg, addIgn);
            return Optional.of(result);
        } else if ("crd-bd".equalsIgnoreCase(paramSplit[0])) {
            //!crd-bd 爆伤 bd dmg
            BigDecimal crd = new BigDecimal(paramSplit[1]);
            BigDecimal bd = new BigDecimal(paramSplit[2]);
            BigDecimal dmg = new BigDecimal(paramSplit[3]);
            crd = ensurePercentage(crd);
            bd = ensurePercentage(bd);
            dmg = ensurePercentage(dmg);
            String result = crdToBd(crd, bd, dmg);
            return Optional.of(result);
        } else if ("bd".equalsIgnoreCase(paramSplit[0])) {
            //!bd bd dmg 主属百分比 总主属 总副属1 总副属2
            BigDecimal bd = new BigDecimal(paramSplit[1]);
            BigDecimal dmg = new BigDecimal(paramSplit[2]);
            BigDecimal percentageMain = new BigDecimal(paramSplit[3]);
            BigDecimal mainTotal = new BigDecimal(paramSplit[4]);
            String[] secondary = ArrayUtil.sub(paramSplit, 5, paramSplit.length);
            List<BigDecimal> secondaryList = Arrays.stream(secondary)
                    .map(BigDecimal::new)
                    .collect(Collectors.toList());
            bd = ensurePercentage(bd);
            dmg = ensurePercentage(dmg);
            percentageMain = ensurePercentage(percentageMain);
            String result = bdToMainStat(bd, dmg, percentageMain, mainTotal, secondaryList);
            return Optional.of(result);
        }
        return Optional.ofNullable(fallback());
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

    /**
     * 计算1%all=多少主属
     *
     * @param baseMain       主属基础值
     * @param mainPercentage 主属百分比
     * @param baseSecondary  副属性基础值
     * @return
     */
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
     * @param bd         BD
     * @param dmg        dmg
     * @return
     */
    private String attPercentToDmg(BigDecimal attPercent, BigDecimal bd, BigDecimal dmg) {
        BigDecimal up = BigDecimal.ONE.add(bd).add(dmg);
        BigDecimal down = BigDecimal.ONE.add(attPercent);
        BigDecimal result = up.divide(down, 4, RoundingMode.HALF_UP);
        return "1%%att=%sbd/dmg".formatted(NumberUtil.decimalFormat("#.##", result));
    }

    /**
     * 无视转BD
     *
     * @param bossDef    BOSS防御力
     * @param currentIgn 当前面板无视
     * @param bd         BD
     * @param dmg        dmg
     * @param addIgn     追加无视
     * @return
     */
    private String ignToBd(BigDecimal bossDef, BigDecimal currentIgn, BigDecimal bd, BigDecimal dmg, BigDecimal addIgn) {
        BigDecimal currentBossDmg = BigDecimal.valueOf(ignoreMessageHandler.bossDmg(bossDef.floatValue(), currentIgn.floatValue()));
        float newIgn = ignoreMessageHandler.newDef(currentIgn.floatValue(), addIgn.floatValue());
        BigDecimal newBossDmg = BigDecimal.valueOf(ignoreMessageHandler.bossDmg(bossDef.floatValue(), newIgn));
        //计算出无视提升的FD
        BigDecimal fdModify = newBossDmg.divide(currentBossDmg, 4, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
        //根据提升的FD计算出对应的BD
        BigDecimal result = BigDecimal.ONE.add(bd).add(dmg).multiply(fdModify);
        return "%s无视=%sbd/dmg".formatted(NumberUtil.decimalFormat("#.##", addIgn.multiply(BigDecimal.valueOf(100))),
                NumberUtil.decimalFormat("#.##", result.multiply(BigDecimal.valueOf(100))));
    }

    /**
     * 爆伤转bd
     *
     * @param crd 爆伤
     * @param bd  BD
     * @param dmg DMG
     * @return
     */
    private String crdToBd(BigDecimal crd, BigDecimal bd, BigDecimal dmg) {
        BigDecimal crdIncrease = ONE_PERCENT.multiply(BigDecimal.ONE.add(bd).add(dmg));
        BigDecimal result = crdIncrease.divide(crd.add(new BigDecimal("1.35")), 4, RoundingMode.HALF_UP);
        result = result.multiply(BigDecimal.valueOf(100));
        return "1爆伤=%sbd/dmg".formatted(NumberUtil.decimalFormat("#.##", result));
    }

    /**
     * bd/dmg转主属
     *
     * @param bd             BD
     * @param dmg            dmg
     * @param mainPercentage 主属百分比
     * @param mainTotal      总主属
     * @param secondaryTotal 总副属
     * @return
     */
    private String bdToMainStat(BigDecimal bd, BigDecimal dmg, BigDecimal mainPercentage, BigDecimal mainTotal, List<BigDecimal> secondaryTotal) {
        BigDecimal secondaryValue = secondaryTotal.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal statValue = mainTotal.multiply(BigDecimal.valueOf(4)).add(secondaryValue);

        BigDecimal td = BigDecimal.ONE.add(bd).add(dmg);
        BigDecimal result = statValue.divide(BigDecimal.valueOf(4).multiply(BigDecimal.ONE.add(mainPercentage)).multiply(td), 4, RoundingMode.HALF_UP);
        result = result.multiply(ONE_PERCENT);
        return "1bd/dmg=%s主属".formatted(NumberUtil.decimalFormat("#.##", result));
    }

    private String fallback() {
        Specification<Question> specification = (root, query, builder) -> {
            Predicate contentPredicate = builder.equal(root.get("content"), "属性计算");
            Predicate whetherDeletePredicate = builder.equal(root.get("whetherDelete"), 0);
            return query.where(contentPredicate, whetherDeletePredicate).getRestriction();
        };

        Optional<Question> questionOpt = questionRepository.findOne(specification);

        if (questionOpt.isEmpty()) {
            return null;
        }

        Question question = questionOpt.get();
        List<Answer> answerList = answerRepository.findAnswerByQuestionIdAndWhetherDeleteIs(question.getId(), 0);

        if (answerList.isEmpty()) {
            return null;
        }
        Answer answer = answerList.get(0);
        return answer.getContent();
    }

}

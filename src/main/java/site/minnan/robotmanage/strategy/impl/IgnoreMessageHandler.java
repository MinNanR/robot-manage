package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 无视消息计算器
 *
 * @author minnan on 2024/01/12
 */
@Component("ignore")
public class IgnoreMessageHandler implements MessageHandler {

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        Function<String, Optional<Float>> parseParam = s -> {
            try {
                return Optional.of(Float.valueOf(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        };

        String rawMessage = dto.getRawMessage();
        String message = rawMessage.substring(2);
        int bossDef;
        List<String> paramStringList = ListUtil.toList(message.split(" "));
        List<Float> defList;
        if (message.charAt(0) == ' ') {
            bossDef = 300;
            defList = paramStringList.stream()
                    .map(parseParam).filter(Optional::isPresent).map(Optional::get).toList();
        } else {
            bossDef = Integer.valueOf(paramStringList.get(0));
            defList = paramStringList.stream().skip(1)
                    .map(parseParam).filter(Optional::isPresent).map(Optional::get).toList();
        }

        Iterator<Float> paramItr = defList.iterator();
        float baseIgn = paramItr.next() / 100;
        float originalBossDmg = bossDmg(bossDef / 100, baseIgn);
        while (paramItr.hasNext()) {
            float addIgn = paramItr.next() / 100;
            baseIgn = newDef(baseIgn, addIgn);
        }
        float newBossDmg = bossDmg(bossDef / 100, baseIgn);

        String fdAdd = "-";
        if (originalBossDmg > 0 && newBossDmg > 0) {
            float fdModify = (newBossDmg - originalBossDmg) / originalBossDmg;
            fdAdd = NumberUtil.decimalFormat("#.##%", fdModify);
        }

        String result = """
                新无视：%s
                BOSS伤害(%d防)：%s -> %s,
                提升FD：%s
                """
                .formatted(NumberUtil.decimalFormat("#.##%", baseIgn), bossDef, bossDmgFormat(originalBossDmg),
                bossDmgFormat(newBossDmg), fdAdd);
        return Optional.of("\n" + result);
    }

    /**
     * 记算BOSS伤害
     *
     * @param bossDef
     * @param ign
     * @return
     */
    private float bossDmg(int bossDef, float ign) {
        return 1 - bossDef * (1 - ign);
    }

    /**
     * 记算新无视
     *
     * @param currentDef
     * @param add
     * @return
     */
    private float newDef(float currentDef, float add) {
        if (add > 0) {
            return currentDef + add * (1 - currentDef);
        } else {
            add = Math.abs(add);
            return (currentDef - add) / (1 - add);
        }
    }

    /**
     * 格式化BOSS伤害
     * @param dmg
     * @return
     */
    private String bossDmgFormat(float dmg) {
        return dmg < 0 ? "不可破防" : NumberUtil.decimalFormat("#.##%", dmg);
    }
}

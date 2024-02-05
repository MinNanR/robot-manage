package site.minnan.robotmanage.strategy.impl;


import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Trigram;
import site.minnan.robotmanage.entity.dao.TrigramRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 起卦消息处理
 *
 * @author Minnan on 2024/01/16
 */
@Component("stirUp")
public class StirUpMessageHandler implements MessageHandler {

    public StirUpMessageHandler(TrigramRepository trigramRepository) {
        this.trigramRepository = trigramRepository;
    }

    private static final String PIC_URL_TEMPLATE = "[CQ:image,file=https://minnan.site:2005/rot/trigram/trigram/%s.png,subType=0]";

    private TrigramRepository trigramRepository;

    public static final String[] yaoName;

    static {
        yaoName = new String[]{"乾天", "巽风", "离火", "艮山", "兑泽", "坎水", "震雷", "坤地"};
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        //起卦六次
        List<Integer> yaoList = Stream.iterate(1, i -> i + 1).limit(6)
                .map(e -> makeOneTrigram())
                .toList();

        String picName = yaoList.stream().map(e -> e.toString()).collect(Collectors.joining());
        String picLine = PIC_URL_TEMPLATE.formatted(picName);

        //本卦转换
        Function<List<Integer>, String> ogTransfer = l -> l.stream().map(e -> (e == 7 || e == 9) ? "0" : "1").collect(Collectors.joining());
        //变卦转换
        Function<List<Integer>, String> chTransfer = l -> l.stream().map(e -> (e == 7 || e == 6) ? "0" : "1").collect(Collectors.joining());

        //解析本卦信息
        int ogUpTrigramIndex = Integer.parseInt(ogTransfer.apply(yaoList.subList(0, 3)), 2);//本卦上卦索引
        int ogDownTrigramIndex = Integer.parseInt(ogTransfer.apply(yaoList.subList(3, 6)), 2);//本卦下卦索引
        int ogTrigramIndex = (ogUpTrigramIndex << 3) + ogDownTrigramIndex;//本卦卦象索引
        Trigram ogTrigram = trigramRepository.findByIndez(ogTrigramIndex);
        //解析变卦信息
        int chUpTrigramIndex = Integer.parseInt(chTransfer.apply(yaoList.subList(0, 3)), 2);//变卦上卦索引
        int chDownTrigramIndex = Integer.parseInt(chTransfer.apply(yaoList.subList(3, 6)), 2);//变卦下卦索引
        int chTrigramIndex = (chUpTrigramIndex << 3) + chDownTrigramIndex;//变卦卦象索引
        Trigram chTrigram = trigramRepository.findByIndez(chTrigramIndex);

        String message = """
                %s
                本卦上卦解为 %s，下卦解为 %s
                解为%s，%s，%s，%s
                --------
                变卦上卦解为 %s，下卦解为 %s
                解为%s，%s，%s，%s
                象曰：%s
                """
                .formatted(picLine,
                        yaoName[ogUpTrigramIndex], yaoName[ogDownTrigramIndex],
                        ogTrigram.getShortName(), ogTrigram.getWholeName(), ogTrigram.getQuality(), ogTrigram.getDescription(),
                        yaoName[chUpTrigramIndex], yaoName[chDownTrigramIndex],
                        chTrigram.getShortName(), chTrigram.getWholeName(), chTrigram.getQuality(), chTrigram.getDescription(),
                        chTrigram.getExplanation());


        return Optional.of("\n" + message);
    }

    /**
     * 占卜一个卦象
     *
     * @return
     */
    private int makeOneTrigram() {
        int coin1 = throwCoin();
        int coin2 = throwCoin();
        int coin3 = throwCoin();
        return coin1 + coin2 + coin3;
    }

    /**
     * 模拟抛掷硬币，大于50为正面，小于50为反面
     *
     * @return 根据周易起卦法，硬币正面计3，反面计2
     */
    private int throwCoin() {
        return RandomUtil.randomInt(0, 100) > 50 ? 3 : 2;
    }
}

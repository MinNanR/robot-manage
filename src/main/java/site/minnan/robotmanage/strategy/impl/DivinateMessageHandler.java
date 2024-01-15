package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.cachekeygen.DivinateKeyGenerator;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 占卜消息处理器
 *
 * @author Minnan on 2024/01/12
 */
@Component("divinate")
public class DivinateMessageHandler implements MessageHandler {

    //随机地点列表
    private static final List<String> placeList;

    //占卜项
    private static final List<String> divinateItemList;

    //占卜结果映射
    private static final Map<String, String> trigramResultMap;

    //六爻列表
    private static final String[] trigramList;

    static {
        divinateItemList = ListUtil.toList("点星", "洗魔方", "洗火花", "刷卡");

        placeList = ListUtil.toList("无名村", "反转城", "啾啾岛", "真香岛", "拉克兰", "阿尔卡那", "莫拉斯", "埃斯佩拉", "塞拉斯",
                "月桥", "黎曼", "神木村", "天空之城", "雷欧树", "真三屋", "明珠港", "玩具城", "塞尔提乌", "亚科斯旅馆",
                "仙都大门", "百草堂", "埃欧雷樱花树", "时间神殿", "圣地", "香格里拉", "阿尔特里亚", "水底世界", "万神殿", "玛加提亚",
                "冰峰雪域", "扎昆洞", "勇士部落", "废弃都市", "射手村", "魔法密林", "阿里安特", "武陵寺院", "里恩", "埃德尔斯坦",
                "避风港", "诺特勒斯", "六岔路口", "林中之城", "废弃营地", "通天塔门口", "天空楼梯Ⅱ");

        trigramList = new String[]{"大安", "流连", "速喜", "赤口", "小吉", "空亡"};

        trigramResultMap = MapBuilder.create(new HashMap<String, String>())
                .put("大安", "大吉")
                .put("流连", "中凶")
                .put("速喜", "中吉")
                .put("赤口", "小凶")
                .put("小吉", "小吉")
                .put("空亡", "大凶")
                .build();
    }


    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    @Cacheable(value = "divination", keyGenerator = "DivinateKeyGenerator")
    public Optional<String> handleMessage(MessageDTO dto) {
        DateTime now = DateTime.now();
        //获取当前微秒时间戳
        long cutime = System.currentTimeMillis() * 1000;
        long nanoTime = System.nanoTime();
        String microsecond = Long.valueOf(cutime + (nanoTime - nanoTime / 1000000 * 1000000) / 1000).toString();
        microsecond = microsecond.substring(microsecond.length() - 6);

        StringBuilder sb = new StringBuilder();
        int i = 0;//占卜偏移量
        for (String item : divinateItemList) {
            //占卜一个卦象
            int trigramIndex = getSixTrigram(now, i, microsecond);
            i++;
            String trigram = trigramList[trigramIndex];
            String trigramResult = trigramResultMap.get(trigram);

            sb.append(item).append("：").append(trigramResult);
            //如果是有吉爻，则挑选一个地点
            if (trigramResult.contains("吉")) {
                String place = RandomUtil.randomEle(placeList);
                sb.append("刷卡".equals(item) ? "，推荐开卡地点：" : "，推荐地点：").append(place);
            }
            sb.append("\n");
        }
        //占卜一个频道
        ChannelRollResult channelRollResult = rollChannel(microsecond);
        String channelMsg = StrUtil.format("今日幸运频道：{}（幸运指数：{}）", channelRollResult.channel, trigramList[channelRollResult.trigramIndex]);
        sb.append(channelMsg);

        return Optional.of(sb.toString());
    }

    /**
     * 六爻起卦，获取一爻
     *
     * @param t           日期
     * @param secStart    起始偏移量
     * @param microsecond 微妙时间戳
     * @return 六爻卦象的下标值
     * @see DivinateMessageHandler#trigramList
     */
    private int getSixTrigram(DateTime t, int secStart, String microsecond) {
        ChineseDate lunarDate = new ChineseDate(t);
        int monthStep = lunarDate.getMonth(), dayStep = lunarDate.getDay();
        int hourStep = ((t.getField(DateField.HOUR) + 1) % 24) / 2 + 1;
        int microsecondLen = microsecond.length();
        int customStep1 = Integer.valueOf(String.valueOf(microsecond.charAt(secStart % microsecondLen)));
        int customStep2 = Integer.valueOf(String.valueOf(microsecond.charAt((secStart + 4) % microsecondLen)));

        int trigramIndex = 0;
        int trigramLen = trigramList.length;
        trigramIndex = (trigramIndex + monthStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + dayStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + hourStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + hourStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + (customStep1 > 0 ? customStep1 - 1 : customStep1)) % trigramLen;
        trigramIndex = (trigramIndex + (customStep2 > 0 ? customStep2 - 1 : customStep2)) % trigramLen;
        return trigramIndex;
    }

    private ChannelRollResult rollChannel(String microsecond) {
        int microsecondLen = microsecond.length();
        //储存每条线占卜结果
        Map<Integer, Integer> channelTrigramMap = new HashMap<>();
        //频道迭代器
        Iterator<Integer> channelItr = Stream.iterate(1, i -> i + 1).limit(40).iterator();
        while (channelItr.hasNext()) {
            Integer channel = channelItr.next();
            //随机盐
            int saltStep = RandomUtil.randomInt(0, 120);
            //时间盐
            int secStep = Integer.valueOf(microsecond.charAt(channel % microsecondLen));
            saltStep = Math.max(saltStep, 1);
            secStep = Math.max(secStep, 1);
            //计算这个频道的占卜结果索引
            int trigramIndex = ((secStep - 1) + (saltStep - 1)) % trigramList.length;
            channelTrigramMap.put(channel, trigramIndex);
        }

        //按占卜结果索引分组
        Map<Integer, List<Integer>> rollResultMap = channelTrigramMap.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        //选取占卜结果为吉的频道，按大安，速喜，小吉顺序选取
        List<Integer> targetChannelList;
        if (rollResultMap.containsKey(0)) {
            targetChannelList = rollResultMap.get(0);
        } else if (rollResultMap.containsKey(2)) {
            targetChannelList = rollResultMap.get(2);
        } else if (rollResultMap.containsKey(4)) {
            targetChannelList = rollResultMap.get(4);
        } else {
            //如果没有为吉的频道，则重新选取
            return rollChannel(microsecond);
        }
        //在选定的频道列表里随机一个
        Integer luckChannel = RandomUtil.randomEle(targetChannelList);
        return new ChannelRollResult(luckChannel, channelTrigramMap.get(luckChannel));

    }

    private record ChannelRollResult(Integer channel, Integer trigramIndex) {
    }
}

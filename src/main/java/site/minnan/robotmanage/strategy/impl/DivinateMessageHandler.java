package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.chinese.LunarInfo;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.RandomUtil;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.*;

/**
 * 占卜消息处理器
 *
 * @author Minnan on 2024/01/12
 */
public class DivinateMessageHandler implements MessageHandler {

    private final RedisUtil redisUtil;

    public DivinateMessageHandler(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

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
    public Optional<String> handleMessage(MessageDTO dto) {
        DateTime now = DateTime.now();
        String today = now.toString("yyyyMMdd");

        long cutime = System.currentTimeMillis() * 1000;
        long nanoTime = System.nanoTime();
        String microsecond = Long.valueOf(cutime + (nanoTime - nanoTime / 1000000 * 1000000) / 1000).toString();

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String item : divinateItemList) {
            int trigramIndex = getSixTrigram(now, i, microsecond);
            String trigram = trigramList[trigramIndex];
            String trigramResult = trigramResultMap.get(trigram);

            sb.append(item + "：" + trigramResult);
            if (trigramResult.contains("吉")) {
                String place = RandomUtil.randomEle(placeList);
                if (!"刷卡".equals(item)) {
                    sb.append("，推荐地点：").append(place);
                } else {
                    sb.append("，推荐开卡点").append(place);
                }
            }
            sb.append("\n");
        }


        return Optional.empty();
    }


    private int getSixTrigram(DateTime t, int secStart, String microsecond) {
        ChineseDate lunarDate = new ChineseDate(t);
        int monthStep = lunarDate.getMonth(), dayStep = lunarDate.getDay();
        int hourStep = ((t.getField(DateField.HOUR) + 1) % 24) / 2 + 1;
        int microsecondLen = microsecond.length();
        int customStep1 = Integer.valueOf(String.valueOf(microsecond.charAt(secStart % microsecondLen)));
        int customStep2 = Integer.valueOf(String.valueOf(microsecond.charAt((secStart + 4) % microsecondLen)));

        int trigramIndex = 0;
        int trigramLen = trigramList.size();
        trigramIndex = (trigramIndex + monthStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + dayStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + hourStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + hourStep - 1) % trigramLen;
        trigramIndex = (trigramIndex + (customStep1 > 0 ? customStep1 - 1 : customStep1)) % trigramLen;
        trigramIndex = (trigramIndex + (customStep2 > 0 ? customStep2 - 1 : customStep2)) % trigramLen;
        return trigramIndex;
    }

    public static void main(String[] args) {
        DivinateMessageHandler h = new DivinateMessageHandler(null);

        h.getSixTrigram(DateTime.now(), 0, microsecond);
    }
}

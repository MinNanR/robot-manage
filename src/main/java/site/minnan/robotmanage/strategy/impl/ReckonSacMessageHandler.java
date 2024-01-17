package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.ReUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * sac预测消息处理
 *
 * @author Minnan on 2024/01/17
 */
@Component("reckonSac")
public class ReckonSacMessageHandler implements MessageHandler {

    //神都区域名称下标映射表
    private static final Map<String, Integer> areaIndexMap;

    //每个等级所需要的数量（下标就是等级-1）
    private static final int[] sacNeed;

    ///每个区域日常数量
    public static final int[] sacDaily;

    static {
        areaIndexMap = MapBuilder.create(new HashMap<String, Integer>())
                .put("一岛", 0)
                .put("1岛", 0)
                .put("神都", 0)
                .put("二岛", 1)
                .put("2岛", 1)
                .put("旅馆", 1)
                .put("酒店", 1)
                .put("三岛", 2)
                .put("3岛", 2)
                .put("仙都", 2)
                .put("四岛", 3)
                .put("4岛", 3)
                .put("五岛", 4)
                .put("5岛", 4)
                .put("六岛", 5)
                .put("6岛", 5)
                .build();

        sacNeed = new int[]{1, 29, 105, 246, 470, 795, 1239, 1820, 2556, 3465, 4565};

        sacDaily = new int[]{20, 10, 10, 10, 10, 10};
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
        //初始sac为0
        int currentSac = 0;
        String[] currentInfo = message.substring(3).split(" ");
        List<ReckonResult> reckons = new ArrayList<>();
        for (String info : currentInfo) {
            if (info.isBlank()) {
                continue;
            }
            //解析sac信息
            Optional<Sac> sacOpt = parseSac(info);
            if (sacOpt.isEmpty()) {
                continue;
            }
            Sac sac = sacOpt.get();
            //输入大于11级，限制到11级
            if (sac.level() >= 11) {
                currentSac = currentSac + 11;
                continue;
            }
            //sac累加
            currentSac = currentSac + sac.level();
            //预测单个岛sac等级变化
            try {
                List<ReckonResult> reckonItemList = reckonSacLevel(sac);
                reckons.addAll(reckonItemList);
            } catch (EntityNotFoundException e) {
                return Optional.of("请输入正确的区域");
            }
        }
        List<SacIncrease> increaseList = reckons.stream()
                //记录每一天sac涨了多少级
                .collect(Collectors.groupingBy(e -> e.date, Collectors.collectingAndThen(Collectors.toList(), List::size)))
                .entrySet().stream()
                //转换成记录
                .map(entry -> new SacIncrease(entry.getKey(), entry.getValue()))
                //按日期排序
                .sorted(Comparator.comparing(SacIncrease::date))
                .toList();
        //遍历增长记录，得到预测结果
        List<String> messageLine = new ArrayList<>();
        for (SacIncrease increase : increaseList) {
            currentSac = currentSac + increase.levelAdd();
            String dateString = increase.date().toString("yyyy-MM-dd");
            messageLine.add("%s：%d".formatted(dateString, currentSac * 10));
        }

        String reply = String.join("\n", messageLine);
        return Optional.of(reply);
    }

    private Optional<Sac> parseSac(String inputStr) {
        //输入格式为 2岛10级100
        List<String> regexResult = ReUtil.getAllGroups(Pattern.compile("\\s*(.*岛)(\\d+)级(\\d+)"), inputStr);
        if (regexResult == null || regexResult.isEmpty()) {
            return Optional.empty();
        }
        String area = regexResult.get(1);
        int level = Math.max(1, Integer.parseInt(regexResult.get(2)));
        int count = Integer.parseInt(regexResult.get(3));
        return Optional.of(new Sac(area, level, count));
    }

    private List<ReckonResult> reckonSacLevel(Sac sac) throws EntityNotFoundException {
        String areaName = sac.areaName();
        //区域表内找不到输入的区域，提示错误
        if (!areaIndexMap.containsKey(areaName)) {
            throw new EntityNotFoundException();
        }
        DateTime today = DateTime.now();
        int startLevel = sac.level();
        //大于11级时不操作
        if (startLevel >= 11) {
            return Collections.singletonList(new ReckonResult(today, 11));
        }
        //当前已有的岛球总量 = 上一级升级所需数量 + 当前数量
        int currentTotal = sacNeed[startLevel - 1] + sac.count();
        //升级到下一级所需数量
        int nextNeed = sacNeed[startLevel];
        //日常数量
        int countPerDay = sacDaily[areaIndexMap.get(areaName)];
        //日期偏移量
        int dayDiffer = 0;
        List<ReckonResult> result = new ArrayList<>();
        while (startLevel < sacNeed.length) {
            //每次循环模拟做了一天日常
            dayDiffer++;
            //岛球总量累加
            currentTotal = currentTotal + countPerDay;
            //模拟岛球升级
            if (currentTotal >= nextNeed) {
                //岛球升级
                startLevel++;
                //记录当前日期的等级，其实记不记无所谓，反正最后不会用到这个等级，记下这一天升级就行了
                ReckonResult reckonItem = new ReckonResult(today.offsetNew(DateField.DAY_OF_YEAR, dayDiffer), startLevel);
                result.add(reckonItem);
                //如果升级后还没有满级，继续预测
                if (startLevel < sacNeed.length) {
                    nextNeed = sacNeed[startLevel];
                }
            }
        }
        return result;
    }

    private record Sac(String areaName, int level, int count) {

    }

    /**
     * 预测结果记录
     *
     * @param date  日期
     * @param level 等级
     */
    private record ReckonResult(DateTime date, int level) {
    }

    /**
     * sac增长量
     *
     * @param date     日期
     * @param levelAdd 增长的等级
     */
    private record SacIncrease(DateTime date, int levelAdd) {

    }
}

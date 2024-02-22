package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.date.ChineseDate;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.Holiday;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component("holiday")
@Slf4j
public class HolidayMessageHandler implements MessageHandler {

    private RedisUtil redisUtil;

    public static final String HOLIDAY_KEY = "holiday";

    public static final String INFO_KEY = "today_info";

    /**
     * 工作日映射表，值为true表示是工作日，false表示是休息日
     */
    private static final Map<String, Boolean> workDayMap;

    static {
        workDayMap = new HashMap<>();
    }

    public HolidayMessageHandler(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    private String infoKey() {
        return INFO_KEY + ":" + DateTime.now().toString("yyyyMMdd");
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String infoKey = infoKey();
        //每天生成生成固定消息
        String content = (String) redisUtil.getValue(infoKey);
        if (StrUtil.isBlank(content)) {
            content = createContent();
            redisUtil.valueSet(infoKey, content, Duration.ofDays(1));
        }
        return Optional.of("\n" + content);
    }

    /**
     * 创建今日消息内容
     *
     * @return
     */
    private String createContent() {
        DateTime now = DateTime.now();
        String dateString = now.toString("yyyy年M月d日") + now.dayOfWeekEnum().toChinese();
        ChineseDate chineseDate = new ChineseDate(now);
        String chineseDateString = chineseDate.toString();
        //获取每日提示消息
        HttpResponse res = HttpUtil.createGet("https://timor.tech/api/holiday/tts").execute();
        JSONObject ttsJson = JSONUtil.parseObj(res.body());
        String tts = ttsJson.getStr("tts");
        //计算下个休息日还有多久
        int nextFreeDayCount = getNextFreeDayCount();
        try {
            List<Holiday> holidays = fetchHoliday();

            StringBuilder sb = new StringBuilder();
            sb.append("今天是").append(dateString).append("，").append(chineseDateString);
            //今天是否是二十四节气中
            String term = chineseDate.getTerm();
            //今天是否是中国农历节日
            String festivals = chineseDate.getFestivals();
            if (!term.isBlank()) {
                sb.append("，").append(term);
            }
            if (!festivals.isBlank()) {
                sb.append("，").append(festivals);
            }
            sb.append("\n");
            if (nextFreeDayCount > 0) {
                sb.append("距离下个休息日还有").append(nextFreeDayCount).append("天\n");
            }
            //将未来节日加入到消息中
            holidays.stream()
                    .map(e -> e.format(now))
                    .forEach(e -> sb.append(e).append("\n"));
            sb.append(tts);

            return sb.toString();
        } catch (JsonProcessingException e) {
            log.error("创建日期失败", e);
            return """
                    今天是%s，%s,
                    %s
                    """.formatted(dateString, chineseDateString,
                    tts);
        }
    }

    /**
     * 计算下个休息日还有几天
     *
     * @return
     */
    public int getNextFreeDayCount() {
        DateTime testDate = DateTime.now();
        int dayCount = 0;
        int size = workDayMap.size();
        //防止越界或死循环
        while (dayCount < size) {
            String dateString = testDate.toString("yyyy-MM-dd");
            //在工作日表中查看今天是否为工作日，默认是非工作日
            Boolean isWorkDay = workDayMap.getOrDefault(dateString, false);
            if (isWorkDay) {
                dayCount++;
            } else {
                break;
            }
            //步进一天
            testDate.offset(DateField.DAY_OF_YEAR, 1);
        }
        return dayCount;
    }

    /**
     * 获取假期数据
     * redis有时从redis获取，没有时刷新数据，刷新数据部分会放入定时任务
     *
     * @return
     * @throws JsonProcessingException
     */
    public List<Holiday> fetchHoliday() throws JsonProcessingException {
        String holiday = (String) redisUtil.getValue(HOLIDAY_KEY);
        if (holiday == null) {
            return refreshHoliday();
        } else {
            JSONArray array = JSONUtil.parseArray(holiday);
            List<Holiday> list = array.stream()
                    .map(e -> (JSONObject) e)
                    .map(e -> new Holiday(DateUtil.parseDate(e.getStr("date")), e.getStr("name")))
                    .sorted(Comparator.comparing(Holiday::date))
                    .toList();
            return list;
        }
    }

    /**
     * 刷新假日缓存
     *
     * @return
     */
    public List<Holiday> refreshHoliday() throws JsonProcessingException {
        DateTime now = DateTime.now();
        //查询未来12个月的假期安排，查询12个月全部数据后（因为有些假期会跨月），再按假期名字分组，取第一天作为假期
        Map<String, Holiday> holidayMap = DateUtil.rangeFunc(now, now.offsetNew(DateField.YEAR, 1),
                        DateField.MONTH, d -> fetchOneMonthData(DateUtil.format(d, "yyyy-MM")))
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Holiday::name,
                        Collectors.collectingAndThen(Collectors.toList(),
                                l -> l.stream().min(Comparator.comparing(Holiday::date)).get())));

        //国务院未发布来年假期安排是会没有春节和元旦，手动加上
        //这里有个逻辑漏洞，1月份查询的时候会查询到明年的春节和元旦，但是实际情况是1月肯定已经公布了今年的假期安排，所以这个漏洞可以忽略
        if (!holidayMap.containsKey("元旦")) {
            DateTime nextYearDate = DateTime.now().offset(DateField.YEAR, 1);
            DateTime newYearDate = DateUtil.beginOfYear(nextYearDate);
            Holiday newYear = new Holiday(newYearDate, "元旦");
            holidayMap.put(newYear.name(), newYear);
        }
        if (!holidayMap.containsKey("春节")) {
            ChineseDate chineseDate = new ChineseDate(now.year() + 1, 1, 1);
            Holiday springFestival = new Holiday(chineseDate.getGregorianDate(), "春节");
            holidayMap.put(springFestival.name(), springFestival);
        }

        //将假期数据按时间由近到远排序
        List<Holiday> holidayList = holidayMap.values()
                .stream().sorted(Comparator.comparing(Holiday::date))
                .collect(Collectors.toList());
        //删除已经过了的节日
        holidayList.removeIf(e -> now.isAfterOrEquals(e.date()));
        holidayList.sort(Comparator.comparing(Holiday::date));
        //Jackson可以处理record类序列化，hutool处理不了record类的json序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        String redisCache = objectMapper.writeValueAsString(holidayList);
        redisUtil.valueSet(HOLIDAY_KEY, redisCache);
        refreshWorkDay();
        return holidayList;
    }

    /**
     * 查询一个月的数据
     *
     * @param month
     * @return
     */
    private List<Holiday> fetchOneMonthData(String month) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
        HttpResponse response = HttpUtil.createGet("https://timor.tech/api/holiday/year/" + month)
                .header("Accept", "application/json")
                .execute();
        String resBody = response.body();
        log.info("查询https://timor.tech/api/holiday/year/" + month + "完成");
        JSONObject jsonObject = JSONUtil.parseObj(resBody);
        JSONObject holiday = jsonObject.getJSONObject("holiday");
        return holiday.values().stream()
                .map(e -> (JSONObject) e)
                .filter(e -> e.getBool("holiday"))
                .map(e -> {
                    String name = e.getStr("name");
                    return new Holiday(DateUtil.parseDate(e.getStr("date")), name.startsWith("初") ? "春节" : name);
                })
                .toList();
    }


    /**
     * 更新工作日映射表
     */
    @PostConstruct
    public void refreshWorkDay() {
        DateTime now = DateTime.now();
        HttpResponse response = HttpUtil.createGet("https://timor.tech/api/holiday/year/" + now.year())
                .header("Accept", "application/json")
                .execute();
        String resBody = response.body();
        JSONObject jsonObject = JSONUtil.parseObj(resBody);
        JSONObject holiday = jsonObject.getJSONObject("holiday");

        DateTime endOfYear = DateUtil.endOfYear(now);
        //从今天更新到今年最后一天
        DateUtil.rangeConsume(now, endOfYear, DateField.DAY_OF_MONTH, date -> {
            DateTime dateTime = DateTime.of(date);
            String monthAndDay = dateTime.toString("MM-dd");
            String dateString = dateTime.toString("yyyy-MM-dd");
            //假日表里有一天，就根据数据判断是不是工作日。否则看是不是周末
            boolean isWorkDay = holiday.containsKey(monthAndDay) ?
                    !holiday.getJSONObject(monthAndDay).getBool("holiday")
                    : !dateTime.isWeekend();
            workDayMap.put(dateString, isWorkDay);
        });
    }


}

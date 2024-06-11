package site.minnan.robotmanage.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.tags.EditorAwareTag;
import site.minnan.robotmanage.entity.aggregate.MaintainRecord;
import site.minnan.robotmanage.entity.dao.MaintainRecordRepository;
import site.minnan.robotmanage.infrastructure.config.ProxyConfig;
import site.minnan.robotmanage.service.MaintainService;
import site.minnan.robotmanage.service.ProxyService;

import java.net.Proxy;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 维护公告服务
 *
 * @author Minnan on 2024/01/24
 */
@Service
@Slf4j
public class MaintainServiceImpl implements MaintainService {

    //    public static final String LIST_URL = "https://maplestory.nexon.net/news/maintenance";
    public static final String LIST_URL = "https://g.nexonstatic.com/maplestory/cms/v1/news";

    public static final String BASE_URL = "https://maplestory.nexon.net";

    private Proxy proxy;

    private MaintainRecordRepository maintainRecordRepository;

    //日期格式化器，EEEE是星期几英文的全写，简写是EE，MMMM是月份英文的全写，缩写是MMM，a是匹配AM/PM
    private static final DateTimeFormatter timeFormatter1 = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter timeFormatter2 = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.ENGLISH);

    public MaintainServiceImpl(Proxy proxy, MaintainRecordRepository maintainRecordRepository) {
        this.proxy = proxy;
        this.maintainRecordRepository = maintainRecordRepository;
    }

    /**
     * 探测维护公告
     *
     * @return
     */
    @Override
    public Optional<MaintainRecord> detectMaintain() {
        //查询维护公告网页
        HttpResponse listRes = HttpUtil.createGet(LIST_URL).setProxy(proxy).execute();
        String listText = listRes.body();
        Document listDoc = Jsoup.parse(listText);
        //公告列表css
        Element container = listDoc.selectFirst(".news-container");
        Elements newsItemList = container.select(".news-item");

        Optional<MaintainRecord> opt = Optional.empty();
        for (Element newsItem : newsItemList) {
            //维护公共的详情链接
            String href = newsItem.select("a").attr("href");
            //切割链接，获取公告id（官网的id）和公告标题
            String[] hrefSplit = href.split("/");
            int newsId = Integer.parseInt(hrefSplit[2]);
            String newsTitle = hrefSplit[3];
            //检查第一条公告是否有记录，已经记录过了就结束
            MaintainRecord recordInDb = maintainRecordRepository.findFirstByNewsId(newsId);
            if (recordInDb != null) {
                break;
            }
            //查询详情页
            HttpResponse contentRes = HttpUtil.createGet(BASE_URL + href).setProxy(proxy).execute();
            String contentText = contentRes.body();
            Document contentDoc = Jsoup.parse(contentText);
            //公告内容css选择器
            Element articleContentDiv = contentDoc.selectFirst(".article-content");

            Elements children = articleContentDiv.children();
            //定位维护时间的html元素，内容为Time的下一个元素就是维护时间
            int timeElementIndex = 0;
            for (Element child : children) {
                timeElementIndex++;
                //以前是这一行是Times，某一次公告这里变成了Time，所以用前置匹配
                if (child.text().startsWith("Time")) {
                    break;
                }
            }

            Element timeEle = children.get(timeElementIndex);
            //元素内两个span，一个是日期，一个是时间
            Elements spans = timeEle.select("span");
            String dateStr, timeStr;
            if (!spans.isEmpty()) {
                Element dateSpan = spans.get(0);
                dateStr = dateSpan.text();
                //这里的内容是多个时区的时间，取第一个时间做解析就行
                String timeHtml = spans.get(1).selectFirst("strong").html();
                timeStr = timeHtml.split("<br>")[0];
            } else {
                //傻卵NX发公告格式不固定，有些公告是两个span，有些公告是只有一个strong，这里后续可能还有其他分支
                List<TextNode> textNodes = timeEle.selectFirst("strong").textNodes();
                dateStr = textNodes.get(0).text();
                timeStr = textNodes.get(1).text();
            }
            //第一个冒号前是时区信息，第一个冒号后是时间信息
            int timezoneAndTimeSplitIndex = timeStr.indexOf(":");
            String timezoneStr = timeStr.substring(0, timezoneAndTimeSplitIndex);
            String startAndEndTimeString = timeStr.substring(timezoneAndTimeSplitIndex + 1);
            //正则提取时区信息，以前是用时区名字做时区信息，但是时区名字不好对应，后面改成直接用给的UTC偏移量做时区信息
            String timeDeltaString = ReUtil.getGroup1("UTC\\s(-?\\d)", timezoneStr);
            //时区偏移量
            int timeDelta = Integer.parseInt(timeDeltaString);

            //开始时间和结束时间是用 “-“ 分割
            String[] timeSplit = startAndEndTimeString.split("-");
            //2024年3月8日优化，公告回出现带序数的日期，如，Friday, March 22nd, 2024
            DateTimeFormatter timeFormatter;
            if (ReUtil.contains("(\\d+)[(th)|(st)|(rd)|(nd)]+", dateStr)) {
                dateStr = dateStr.replaceAll("(\\d+)[(th)|(st)|(rd)|(nd)]+", "$1");
                timeFormatter = timeFormatter2;
            } else {
                timeFormatter = timeFormatter1;
            }
            //解析维护时间
            DateTime startTime = DateUtil.parse(dateStr + " " + timeSplit[0].strip(), timeFormatter);
            DateTime endTime = DateUtil.parse(dateStr + " " + timeSplit[1].strip(), timeFormatter);
            //校正时间至北京时间，timeDelta * -1是校正到UTC时间，+8是北京时间=UTC+8，写8 - timeDelta也行，但是这样写更能表达计算思想
            startTime.offset(DateField.HOUR, timeDelta * -1 + 8);
            endTime.offset(DateField.HOUR, timeDelta * -1 + 8);
            //生成维护记录
            MaintainRecord record = new MaintainRecord();
            record.setTitle(newsTitle);
            record.setNewsId(newsId);
            record.setStartTime(startTime.toString("yyyy-MM-dd HH:mm"));
            record.setEndTime(endTime.toString("yyyy-MM-dd HH:mm"));

            log.info("检测到新维护公告，公告id {}，维护时间为{}-{}", record.getNewsId(), record.getStartTime(), record.getEndTime());
            //存入数据库
            maintainRecordRepository.save(record);
            opt = Optional.of(record);
            break;
        }
        return opt;
    }

    /**
     * 查询最近一次维护时间
     *
     * @return
     */
    @Override
    public Optional<MaintainRecord> getMaintain() {
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm");
        MaintainRecord record = maintainRecordRepository.findFirstByEndTimeGreaterThanEqualOrderByStartTimeDesc(now);
        return Optional.ofNullable(record);
    }

    @Override
    public Optional<MaintainRecord> detectMaintainV2() {
        HttpResponse listRes = HttpUtil.createGet(LIST_URL).setProxy(proxy).execute();
        String listResJsonString = listRes.body();
        JSONArray noteList = JSONUtil.parseArray(listResJsonString);

        if (noteList.isEmpty()) {
            return Optional.empty();
        }

        Optional<JSONObject> newestNoteOpt = noteList.stream()
                .map(e -> (JSONObject) e)
                .filter(e -> "maintenance".equalsIgnoreCase(e.getStr("category")))
                .findFirst();

        if (newestNoteOpt.isEmpty()) {
            return Optional.empty();
        }

        JSONObject newestNote = newestNoteOpt.get();
        Integer newsId = newestNote.getInt("id");
        //检查第一条公告是否有记录，已经记录过了就结束
        MaintainRecord recordInDb = maintainRecordRepository.findFirstByNewsId(newsId);
        if (recordInDb != null) {
            return Optional.empty();
        }
//
        String contentUrl = LIST_URL + "/" + newsId;
        HttpResponse contentRes = HttpUtil.createGet(contentUrl).setProxy(proxy).execute();
        JSONObject contentJson = JSONUtil.parseObj(contentRes.body());
        String contentText = contentJson.getStr("body");

        Document contentDoc = Jsoup.parse(contentText);
        Elements children = contentDoc.body().children();
        //定位维护时间的html元素，内容为Time的下一个元素就是维护时间
        int timeElementIndex = 0;
        for (Element child : children) {
            timeElementIndex++;
            //以前是这一行是Times，某一次公告这里变成了Time，所以用前置匹配
            if (child.text().startsWith("Time")) {
                break;
            }
        }

        Element timeEle = children.get(timeElementIndex);
        //元素内两个span，一个是日期，一个是时间
        Elements spans = timeEle.select("span");
        String dateStr, timeStr;
        if (!spans.isEmpty()) {
            Element dateSpan = spans.get(0);
            dateStr = dateSpan.text();
            //这里的内容是多个时区的时间，取第一个时间做解析就行
            String timeHtml = spans.get(1).selectFirst("strong").html();
            timeStr = timeHtml.split("<br>")[0];
        } else {
            //傻卵NX发公告格式不固定，有些公告是两个span，有些公告是只有一个strong，这里后续可能还有其他分支
            List<TextNode> textNodes = timeEle.selectFirst("strong").textNodes();
            if (textNodes.size() < 2) {
                //分开了两个P标签
                textNodes = timeEle.select("strong").textNodes();
            }
            dateStr = textNodes.get(0).text();
            timeStr = textNodes.get(1).text();
        }
        //第一个冒号前是时区信息，第一个冒号后是时间信息
        int timezoneAndTimeSplitIndex = timeStr.indexOf(":");
        String timezoneStr = timeStr.substring(0, timezoneAndTimeSplitIndex);
        String startAndEndTimeString = timeStr.substring(timezoneAndTimeSplitIndex + 1);
        //正则提取时区信息，以前是用时区名字做时区信息，但是时区名字不好对应，后面改成直接用给的UTC偏移量做时区信息
        String timeDeltaString = ReUtil.getGroup1("UTC\\s(-?\\d)", timezoneStr);
        //时区偏移量
        int timeDelta = Integer.parseInt(timeDeltaString);

        //开始时间和结束时间是用 “-“ 分割
        String[] timeSplit = startAndEndTimeString.split("-");
        //2024年3月8日优化，公告会出现带序数的日期，如，Friday, March 22nd, 2024
        DateTimeFormatter timeFormatter;
        if (ReUtil.contains("(\\d+)[(th)|(st)|(rd)|(nd)]+", dateStr)) {
            dateStr = dateStr.replaceAll("(\\d+)[(th)|(st)|(rd)|(nd)]+", "$1");
            timeFormatter = timeFormatter2;
        } else {
            timeFormatter = timeFormatter1;
        }
        //解析维护时间
        DateTime startTime = DateUtil.parse(dateStr + " " + timeSplit[0].strip(), timeFormatter);
        DateTime endTime = DateUtil.parse(dateStr + " " + timeSplit[1].strip(), timeFormatter);
        //校正时间至北京时间，timeDelta * -1是校正到UTC时间，+8是北京时间=UTC+8，写8 - timeDelta也行，但是这样写更能表达计算思想
        startTime.offset(DateField.HOUR, timeDelta * -1 + 8);
        endTime.offset(DateField.HOUR, timeDelta * -1 + 8);
        //生成维护记录
        MaintainRecord record = new MaintainRecord();
        record.setTitle(dateStr + "维护公告");
        record.setNewsId(newsId);
        record.setStartTime(startTime.toString("yyyy-MM-dd HH:mm"));
        record.setEndTime(endTime.toString("yyyy-MM-dd HH:mm"));

        log.info("检测到新维护公告，公告id {}，维护时间为{}-{}", record.getNewsId(), record.getStartTime(), record.getEndTime());
        //存入数据库
        maintainRecordRepository.save(record);

        return Optional.of(record);
    }

    public static void main(String[] args) {
//        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH);
//        DateTime time = DateUtil.parse(dateStr, pattern);
//        System.out.println(time);
        Proxy proxy1 = new ProxyConfig().proxy();

        MaintainService maintainService = new MaintainServiceImpl(proxy1, null);
        Optional<MaintainRecord> maintainRecord = maintainService.detectMaintainV2();
//        System.out.println(maintainRecord.map(MaintainRecord::toString).orElse("error"));
    }
}

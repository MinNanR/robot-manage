package site.minnan.robotmanage.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.MaintainRecord;
import site.minnan.robotmanage.entity.dao.MaintainRecordRepository;
import site.minnan.robotmanage.service.MaintainService;

import java.net.Proxy;
import java.time.format.DateTimeFormatter;
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

    public static final String LIST_URL = "https://maplestory.nexon.net/news/maintenance";

    public static final String BASE_URL = "https://maplestory.nexon.net";

    private Proxy proxy;

    private MaintainRecordRepository maintainRecordRepository;

    public MaintainServiceImpl(Proxy proxy, MaintainRecordRepository maintainRecordRepository) {
        this.proxy = proxy;
        this.maintainRecordRepository = maintainRecordRepository;
    }

    /**
     * 探测维护公告
     */
    @Override
    public void detectMaintain() {
        //查询维护公告网页
        HttpResponse listRes = HttpUtil.createGet(LIST_URL).setProxy(proxy).execute();
        String listText = listRes.body();
        Document listDoc = Jsoup.parse(listText);
        //公告列表css
        Element container = listDoc.selectFirst(".news-container");
        Elements newsItemList = container.select(".news-item");

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
            Element dateSpan = spans.get(0);
            String dateStr = dateSpan.text();
            //这里的内容是多个时区的时间，取第一个时间做解析就行
            String timeHtml = spans.get(1).selectFirst("strong").html();
            String timeStr = timeHtml.split("<br>")[0];
            //第一个冒号前是时区信息，第一个冒号后是时间信息
            int timezoneAndTimeSplitIndex = timeStr.indexOf(":");
            String timezoneStr = timeStr.substring(0, timezoneAndTimeSplitIndex);
            String startAndEndTimeString = timeStr.substring(timezoneAndTimeSplitIndex + 1);
            //正则提取时区信息，以前是用时区名字做时区信息，但是时区名字不好对应，后面改成直接用给的UTC偏移量做时区信息
            String timeDeltaString = ReUtil.getGroup1("UTC\\s(-?\\d)", timezoneStr);
            //时区偏移量
            int timeDelta = Integer.parseInt(timeDeltaString);
            //日期格式化器，EEEE是星期几英文的全写，简写是EE，MMMM是月份英文的全写，缩写是MMM，a是匹配AM/PM
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy h:mm a", Locale.ENGLISH);
            //开始时间和结束时间是用 “-“ 分割
            String[] timeSplit = startAndEndTimeString.split("-");
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
            break;
        }
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
}

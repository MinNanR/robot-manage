package site.minnan.robotmanage.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dao.QueryMapRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.CharacterData;
import site.minnan.robotmanage.entity.vo.ExpData;
import site.minnan.robotmanage.service.CharacterSupportService;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 角色信息服务
 *
 * @author Minnan on 2024/01/15
 */
@Service
public class CharacterSupportServiceImpl implements CharacterSupportService {


    public static final String BASE_QUERY_URL = "https://mapleranks.com/u/";

    private QueryMapRepository queryMapRepository;

    public CharacterSupportServiceImpl(QueryMapRepository queryMapRepository) {
        this.queryMapRepository = queryMapRepository;
    }

    /**
     * 查询角色信息
     *
     * @param queryName 查询角色名称
     * @return
     */
    @Override
    public CharacterData fetchCharacterInfo(String queryName) {
        String queryUrl = BASE_QUERY_URL + queryName.strip();

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 2334));

        HttpRequest queryRequest = HttpUtil.createGet(queryUrl).setProxy(proxy);
        HttpResponse queryRes = queryRequest.execute();
        String html = queryRes.body();

        Document doc = Jsoup.parse(html, "UTF-8");

        //排名信息第一个元素为查询角色排名，第二个元素为排名附近角色排名信息，第二个元素内的第三个元素为区排名
        JSONArray rankInfo = new JSONArray();
        if (ReUtil.contains("fetch\\('(.*?)'\\)", html)) {
            String fetchUrl = ReUtil.get("fetch\\('(.*?)'\\)", html, 1);
            HttpResponse rankRes = HttpUtil.createGet(fetchUrl).setProxy(proxy).execute();
            if (rankRes.isOk()) {
                String rankBase64 = rankRes.body();
                byte[] rankInfoBytes = Base64.decode(rankBase64);
                rankInfo = JSONUtil.parseArray(rankInfoBytes);
            }
        }

        Function<String, String> getText = xpath -> {
            Elements elements = doc.selectXpath(xpath);
            return elements.isEmpty() ? "" : elements.get(0).text().strip();
        };

        //解析名称，角色图片，等级信息，职业信息
        String name = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/h3");
        String img = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/img");
        String lvInfo = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/h5");
        String jobInfo = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/p");
        //正则解析等级，经验百分比
        List<String> lvRe = ReUtil.getAllGroups(Pattern.compile("Lv.\\s+(\\d+)\\s+\\((.*?)\\)"), lvInfo);
        String lv = lvRe.get(1);
        String percent = lvRe.get(2);
        //解析职业名称，服务器信息
        List<String> jobRe = ReUtil.getAllGroups(Pattern.compile("(.*?)\\sin\\s(.*+)"), jobInfo);
        String jobName = jobRe.get(1);
        String serverName = jobRe.get(2);
        Console.log(lv, percent, jobName, serverName);

        CharacterData characterData = new CharacterData();
        characterData.setCharacterImgUrl(img);
        characterData.setName(name);
        characterData.setLevel(lv);
        characterData.setServer(serverName);
        characterData.setExpPercent(percent);
        characterData.setJob(jobName);

        if (rankInfo.isEmpty()) {
            characterData.setRankEmpty();
        } else {
            //排名信息依次为，职业全服排名，等级全服排名，等级区排名，职业区排名
            JSONArray characterRank = rankInfo.getJSONArray(0);
            characterData.setServerClassRank(characterRank.getStr(3).replace(",", ""));
            characterData.setServerLevelRank(characterRank.getStr(2).replaceAll(",", ""));
            characterData.setGlobalClassRank(characterRank.getStr(0).replaceAll(",", ""));
            characterData.setGlobalLevelRank(characterRank.getStr(1).replace(",", ""));
        }

        //解析成就信息
        Elements achievementRankEles = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[3]/ul/li[1]/span");
        if (!achievementRankEles.isEmpty()) {
            String achievementRank = achievementRankEles.get(0).text();
            String achievementPoint = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[3]/ul/li[3]/span");
            characterData.setAchievementRank(achievementRank);
            characterData.setAchievementPoints(achievementPoint);
        }

        //解析联盟信息
        Elements legionRankEles = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[2]/ul/li[1]/span");
        if (!legionRankEles.isEmpty()) {
            String legionRank = legionRankEles.get(0).text();
            String legionLv = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[2]/ul/li[2]/span");
            Element legionPowerEle = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[2]/ul/li[3]/span/input").get(0);
            Element legionCoinsEle = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[2]/ul/li[4]/div[2]/span[3]/input").get(0);
            characterData.setLegionLevel(legionLv);
            characterData.setLegionRank(legionRank);
            characterData.setLegionPower(legionPowerEle.attr("value").replaceAll(",", ""));
            characterData.setLegionCoinsPerDay(legionCoinsEle.attr("value").replace(",", ""));
        }

        List<String> expSearch = ReUtil.findAllGroup0("const zmChs=function\\(a\\)", html);
        if (!expSearch.isEmpty()) {
            List<ExpData> expData = extraExpData(html);
            characterData.setExpData(expData);
        }

        if (rankInfo.size() > 1) {
            JSONArray nearRankJson = rankInfo.getJSONArray(1).getJSONArray(3);
            List<CharacterData> nearRank = nearRankJson.stream()
                    .map(e -> (JSONArray) e)
                    .map(e -> {
                        CharacterData nearItem = new CharacterData();
                        nearItem.setName(e.getStr(1));
                        nearItem.setServerClassRank(e.getStr(2));
                        nearItem.setLevel(e.getInt(3).toString());
                        nearItem.setExpPercent(e.getStr(4));
                        return nearItem;
                    })
                    .toList();
            characterData.setNearRank(nearRank);
        }

        return characterData;
    }


    /**
     * 解析经验数据
     * 经验数据在html文本的一个图表渲染函数内，需要将这个函数内容提取出来，再做二次正则解析
     *
     * @param html html文本
     * @return 经验数据
     */
    private List<ExpData> extraExpData(String html) {
        //经验图渲染函数开始标记
        String functionSpec = "const zmChs=function(a)";
        int index = html.indexOf(functionSpec);
        //提取函数内容
        int charIndex = index + functionSpec.length() + 1;
        List<Character> functionContentList = new ArrayList<>();//用于储存函数字符
        Stack<Character> bracket = new Stack<>();//花括号匹配栈
        bracket.add('{');//初始化栈
        while (!bracket.isEmpty()) {//栈空时表示函数内容结束
            char c = html.charAt(charIndex);
            functionContentList.add(c);//储存函数内容
            if (c == '}') {//遇到右括号则弹出一个左括号
                bracket.pop();
            } else if (c == '{') {
                bracket.add('{');//遇到左括号则将左括号压入匹配栈中
            }
            charIndex++;
        }

        //将字符数组组合成一个字符串
        String functionContent = functionContentList.subList(1, functionContentList.size() - 1)
                .stream().map(Object::toString).collect(Collectors.joining());

        //用分号切割，第一个元素为日期数据，第二个为剩余数据
        String[] functionContentSplit = functionContent.split(";");
        String dateLinString = functionContentSplit[0];
        String expLineString = functionContentSplit[1];
        //解析日期变量
        List<String> dateGroups = ReUtil.getAllGroups(Pattern.compile("e=(\\[.*?])"), dateLinString);
        JSONArray dateArray = JSONUtil.parseArray(dateGroups.get(1));
        List<String> dateList = dateArray.stream().map(e -> (String) e).toList();
        //在剩余数据中正则匹配经验数据，经验数据储存在xxx.data.datasets[0].data变量中
        List<String> expGroups = ReUtil.getAllGroups(Pattern.compile("datasets\\[0].data=(\\[.*?])"), expLineString);
        JSONArray expArray = JSONUtil.parseArray(expGroups.get(1));
        List<String> expList = expArray.stream().map(Object::toString).toList();
        //将解析出来的数据拼合成经验数据
        Iterator<String> dateItr = dateList.iterator();
        Iterator<String> expItr = expList.iterator();
        List<ExpData> expDataList = new ArrayList<>();
        while (dateItr.hasNext() && expItr.hasNext()) {
            String date = dateItr.next();
            String exp = expItr.next();
            ExpData expData = new ExpData(date, Long.parseLong(exp));
            expDataList.add(expData);
        }

        return expDataList;
    }

    /**
     * 解析查询目标
     *
     * @param queryContent
     * @param userId
     * @return
     */
    @Override
    public String parseQueryContent(String queryContent, String userId) {
        if (queryContent.contains("第")) {
            // TODO: 2024/01/15 排名查询
            return queryContent;
        }
        // TODO: 2024/01/15 用户绑定的名称查询 
        return queryContent;
    }
}

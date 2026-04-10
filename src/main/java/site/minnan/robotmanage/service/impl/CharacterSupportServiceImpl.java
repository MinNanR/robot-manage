package site.minnan.robotmanage.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.NumberChineseFormatter;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import site.minnan.robotmanage.entity.aggregate.*;
import site.minnan.robotmanage.entity.dao.*;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.GetQueryMapListDTO;
import site.minnan.robotmanage.entity.dto.UpdateQueryMapDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;
import site.minnan.robotmanage.entity.vo.bot.ExpData;
import site.minnan.robotmanage.infrastructure.exception.EntityAlreadyExistException;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.mysql.dao.CharacterExpDailyMysqlRepository;
import site.minnan.robotmanage.mysql.dao.CharacterRecordMysqlRepository;
import site.minnan.robotmanage.mysql.entity.CharacterExpDailyMysql;
import site.minnan.robotmanage.mysql.entity.CharacterRecordMysql;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.impl.QueryMessageHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Proxy;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 角色信息服务
 *
 * @author Minnan on 2024/01/15
 */
@Service
@Slf4j
public class CharacterSupportServiceImpl implements CharacterSupportService {


    //    public static final String BASE_QUERY_URL = "https://mapleranks.com/u/";

    public static final String RANK_CACHE_KEY_TEMPLATE = "rank:%s:%s:%d";

    private RedisUtil redisUtil;

    private QueryMapRepository queryMapRepository;

    private NickRepository nickRepository;

    @Autowired
    @Qualifier("proxy")
    private Proxy proxy;

    @Value("${query_source:mapleranks}")
    private String source;

    private static final Integer billion = 1000000000;

    @Autowired
    private CharacterRecordMysqlRepository characterRecordMysqlRepository;

    @Autowired
    private CharacterExpDailyMysqlRepository characterExpDailyMysqlRepository;

    @Autowired
    private CharacterRecordRepository characterRecordRepository;

    @Autowired
    private LvExpRepository lvExpRepository;

    @Autowired
    private CharacterExpDailyRepository characterExpDailyRepository;

    @Autowired
    private JobMapRepository jobMapRepository;

    public CharacterSupportServiceImpl(QueryMapRepository queryMapRepository, NickRepository nickRepository, RedisUtil redisUtil) {
        this.queryMapRepository = queryMapRepository;
        this.nickRepository = nickRepository;
        this.redisUtil = redisUtil;
    }

    /**
     * 查询角色信息
     *
     * @param queryName 查询角色名称
     * @return
     */
    @Override
    public CharacterData fetchCharacterInfo(String queryName) {
        return fetchCharacterInfo(queryName, "u");
    }

    /**
     * 查询角色信息
     *
     * @param queryName 查询角色名称
     * @return
     */
    @Override
    public CharacterData fetchCharacterInfo(String queryName, String server) {
        if ("mapleranks".equals(source)) {
            CharacterData characterData = fetchCharacterInfoMapleRanks(queryName, server);
            characterData.setSource("https://mapleranks.com/");
            return characterData;
        } else if ("gg".equals(source)) {
            CharacterData characterData = fetchCharacterInfoGg(queryName, server);
            characterData.setSource("https://maplestory.gg/");
            return characterData;
        } else {
            CharacterData characterData = fetchCharacterInfoMapleRanks(queryName, server);
            characterData.setSource("https://mapleranks.com/");
            return characterData;
        }
    }

    /**
     * 查询角色信息，从gg查询
     *
     * @param queryName
     * @return
     */
    public CharacterData fetchCharacterInfoGg(String queryName, String server) {
        String baseQueryUrl;
        if ("u".equals(server)) {
            baseQueryUrl = "https://api.maplestory.gg/v2/public/character/gms/";
        } else {
            baseQueryUrl = "https://api.maplestory.gg/v2/public/character/ems/";
        }
        String queryUrl = baseQueryUrl + queryName.strip();
        HttpRequest queryRequest = HttpUtil.createGet(queryUrl).setProxy(proxy);
        HttpResponse queryRes = queryRequest.execute();

        String jsonString = queryRes.body();
        JSONObject j = JSONUtil.parseObj(jsonString);
        JSONObject data = j.getJSONObject("CharacterData");

        CharacterData characterData = new CharacterData();

        characterData.setCharacterImgUrl(data.getStr("CharacterImageURL"));
        characterData.setName(data.getStr("Name"));
        characterData.setLevel(data.getStr("Level"));
        characterData.setServer(data.getStr("Server"));
        characterData.setExpPercent(data.getStr("EXPPercent"));
        characterData.setJob(data.getStr("Class"));

        if (data.containsKey("GlobalRanking")) {
            characterData.setServerClassRank(data.getStr("ServerClassRanking"));
            characterData.setServerLevelRank(data.getStr("ServerRank"));
            characterData.setGlobalClassRank(data.getStr("ClassRank"));
            characterData.setGlobalLevelRank(data.getStr("GlobalRanking"));
        } else {
            characterData.setRankEmpty();
        }

        if (data.containsKey("AchievementRank")) {
            characterData.setAchievementRank(data.getStr("AchievementRank"));
            characterData.setAchievementPoints(data.getStr("AchievementPoints"));
        }

        if (data.containsKey("LegionRank")) {
            characterData.setLegionLevel(data.getStr("LegionLevel"));
            characterData.setLegionRank(data.getStr("LegionRank"));
            characterData.setLegionPower(data.getStr("LegionPower"));
            characterData.setLegionCoinsPerDay(data.getStr("LegionCoinsPerDay"));
        }

        characterData.setNearRank(Collections.emptyList());


        JSONArray graphData = data.getJSONArray("GraphData");
        List<JSONObject> expJsonList = graphData.stream().map(e -> (JSONObject) e).toList();
        List<ExpData> expDataList = new ArrayList<>();
        if (expJsonList.size() < 15) {
            int lackDay = 15 - expJsonList.size();
            JSONObject obj = expJsonList.get(0);
            DateTime firstDate = DateTime.of(obj.getStr("DateLabel"), "yyyy-MM-dd");
            Integer level = obj.getInt("Level");
            Long currentEXP = obj.getLong("CurrentEXP");
            Long expToNextLevel = obj.getLong("EXPToNextLevel");
            BigDecimal process = NumberUtil.div((Number) currentEXP, (long) currentEXP + expToNextLevel, 2, RoundingMode.HALF_UP);
            for (int i = 0; i < lackDay; i++) {
                DateTime date = firstDate.offsetNew(DateField.DAY_OF_YEAR, i - lackDay);
                expDataList.add(new ExpData(date.toString("M/dd"), 0L, level + process.doubleValue()));
            }
        }
        for (int i = 1; i < expJsonList.size(); i++) {
            JSONObject obj = expJsonList.get(i);
            String noteDate = DateUtil.parse(obj.getStr("DateLabel"), "yyyy-MM-dd").toString("M/dd");
            String expDifference = expJsonList.get(i - 1).getStr("EXPDifference");
            Integer level = obj.getInt("Level");
            Long currentEXP = obj.getLong("CurrentEXP");
            Long expToNextLevel = obj.getLong("EXPToNextLevel");
            BigDecimal process = NumberUtil.div((Number) currentEXP, (long) currentEXP + expToNextLevel, 2, RoundingMode.HALF_UP);
            double processDouble = process.doubleValue();
            expDataList.add(new ExpData(noteDate, Long.parseLong(expDifference), level + processDouble));
        }

        characterData.setExpData(expDataList);

        JSONObject lastExp = expJsonList.get(expJsonList.size() - 1);
        Long updateTimeStamp = lastExp.getLong("ImportTime");

        Instant instant = Instant.ofEpochSecond(updateTimeStamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
        String updateTime = formatter.format(instant);
        characterData.setUpdateTime(updateTime);

        return characterData;
    }

    /**
     * 查询角色信息,从mapleranks查询
     *
     * @param queryName 查询角色名称
     * @param server 服务器信息
     * @return
     */
    public CharacterData fetchCharacterInfoMapleRanks(String queryName, String server) {
        String baseQueryUrl;
        if (server.equals("u")) {
            baseQueryUrl = "https://mapleranks.com/u/";
        } else {
            baseQueryUrl = "https://mapleranks.com/u/eu/";
        }
        String queryUrl = baseQueryUrl + queryName.strip();

        HttpRequest queryRequest = HttpUtil.createGet(queryUrl).setProxy(proxy);
        HttpResponse queryRes = queryRequest.execute();
        String html = queryRes.body();


        Document doc = Jsoup.parse(html, "UTF-8");

        //排名信息第一个元素为查询角色排名，第二个元素为排名附近角色排名信息，第二个元素内的第三个元素为区排名
        JSONArray rankInfo = new JSONArray();
        if (ReUtil.contains("fetch\\('(.*?)'\\)", html)) {
            //排名信息查询链接有有效期，有效期比较短，可能就几秒钟，所以要先查询出排名信息再作后续解析
            String fetchUrl = ReUtil.get("fetch\\('(.*?)'\\)", html, 1);
            HttpResponse rankRes = HttpUtil.createGet(fetchUrl)
                    .setProxy(proxy)
                    .timeout(5000)
                    .execute();
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

        String notFoundFlag = getText.apply("/html/body/main/div/div/div[2]/h2");
        if ("Not Found".equals(notFoundFlag)) {
            throw new EntityNotExistException("角色不存在");
        }

        //解析名称，角色图片，等级信息，职业信息
        String name = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/h3");
//        String img = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/img");
        Elements imgEles = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/img");
        String img = imgEles.isEmpty() ? "" : imgEles.get(0).attr("src");
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

        try {
            //解析成就信息
            Elements achievementRankEles = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[3]/ul/li[1]/span");
            if (!achievementRankEles.isEmpty()) {
                String achievementRank = achievementRankEles.get(0).text();
                String achievementPoint = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[3]/ul/li[3]/span");
                characterData.setAchievementRank(achievementRank);
                characterData.setAchievementPoints(achievementPoint);
            }
        } catch (Exception ignored) {
        }

        try {
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
        } catch (Exception ignored) {

        }

        List<String> expSearch = ReUtil.findAllGroup0("const zmChs=function\\(a\\)", html);
        if (!expSearch.isEmpty()) {
            List<ExpData> expData = extraExpData(html);
            characterData.setExpData(expData);
        }

        Function<JSONArray, CharacterData> createNearItem = e -> {
            CharacterData nearItem = new CharacterData();
            nearItem.setName(e.getStr(1));
            nearItem.setServerClassRank(e.getStr(2));
            nearItem.setLevel(e.getInt(3).toString());
            nearItem.setExpPercent(e.getStr(4));
            return nearItem;
        };

        if (rankInfo.size() > 1) {
            JSONArray nearRankJson = rankInfo.getJSONArray(1).getJSONArray(3);
            List<CharacterData> nearRank = nearRankJson.stream()
                    .map(e -> (JSONArray) e)
                    .map(createNearItem)
                    .toList();
            characterData.setNearRank(nearRank);
        }


        Elements infoColumn = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div");
        List<String> infoText = infoColumn.eachText();
        String updateTimeContent = infoText.get(infoText.size() - 1);
        String updateTime = updateTimeContent.replace("Last Updated:", "").strip();
        characterData.setUpdateTime(updateTime);

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
        List<BigDecimal> expProcessList = extraExpProcess(html);
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
        Iterator<BigDecimal> processItr = expProcessList.iterator();
        List<ExpData> expDataList = new ArrayList<>();
        Supplier<Double> getLastProcess = () -> expDataList.isEmpty() ? 0.0 : expDataList.get(expDataList.size() - 1).expProcess();
        while (dateItr.hasNext() && expItr.hasNext() && processItr.hasNext()) {
            String date = dateItr.next();
            String exp = expItr.next();
            double expProcess = processItr.next().doubleValue();
            long expNumber = NumberUtil.isNumber(exp) ? Long.parseLong(exp) : 0;
            expProcess = expNumber == 0 ? getLastProcess.get() : expProcess;
            ExpData expData = new ExpData(date, expNumber, expProcess);
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
            return rankQueryCharacter(queryContent);
        }
        Nick nick = nickRepository.findByQqAndNick(userId, queryContent);
        return nick == null ? queryContent : nick.getCharacter();
    }

    /**
     * 排名查询角色
     *
     * @param queryMessage 查询消息
     * @return 查询目标
     */
    @Override
    public String rankQueryCharacter(String queryMessage) throws EntityNotFoundException {
        String[] querySplit = queryMessage.split("第");
        String queryContent = querySplit[0];
        String rankString = querySplit[1];
        int rank;
        if (NumberUtil.isInteger(rankString)) {
            rank = Integer.parseInt(rankString);
        } else {
            rank = NumberChineseFormatter.chineseToNumber(rankString);
        }

        String today = DateTime.now().toString("yyyyMMdd");
        String cacheKey = RANK_CACHE_KEY_TEMPLATE.formatted(today, queryContent, rank);
        String queryCache = (String) redisUtil.getValue(cacheKey);
        if (queryCache != null) {
            return queryCache;
        }

        QueryMap queryMap = queryMapRepository.findByQueryContentIgnoreCase(queryContent);
        if (queryMap == null) {
            throw new EntityNotFoundException();
        }

        String queryUrl = queryMap.getQueryUrl();
        String fullQueryUrl = StrUtil.format(queryUrl, rank);

        HttpResponse queryResponse = HttpUtil.createGet(fullQueryUrl).setProxy(proxy).execute();
        String responseJsonString = queryResponse.body();
        JSONObject responseJson = JSONUtil.parseObj(responseJsonString);
//        JSONArray queryResultList = JSONUtil.parseArray(responseJsonString);
        JSONArray queryResultList = responseJson.getJSONArray("ranks");
        if (CollectionUtil.isEmpty(queryResultList)) {
            throw new EntityNotFoundException();
        }

        queryResultList.stream()
                .map(e -> (JSONObject) e)
                .forEach(e -> {
                    String key = RANK_CACHE_KEY_TEMPLATE.formatted(today, queryContent, e.getInt("rank"));
                    String value = e.getStr("characterName");
                    redisUtil.valueSet(key, value, Duration.ofHours(6));
                });

        JSONObject targetCharacter = queryResultList.getJSONObject(0);
        return targetCharacter.getStr("characterName");
    }

    /**
     * 查询用户今日查询某个角色的次数
     *
     * @param target 查询母包
     * @param userId 用户id
     * @return
     */
    public int getQueryCount(String target, String userId) {
        String today = DateTime.now().offset(DateField.HOUR, -8).toString("yyyyMMdd");
        String key = "queryCount:%s:%s:%s".formatted(today, userId, target);
        int count;
        if (redisUtil.hasKey(key)) {
            count = (Integer) redisUtil.getValue(key);
        } else {
            count = 0;
        }
        count++;
        redisUtil.valueSet(key, count, Duration.ofDays(1L));
        return count;
    }

    /**
     * 查询昵称列表
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<Nick> getNickList(GetNickListDTO dto) {
        Specification<Nick> specification = ((root, query, builder) -> {
            List<Predicate> list = new ArrayList<>();
            if (StrUtil.isNotBlank(dto.getUserId())) {
                list.add(builder.equal(root.get("qq"), dto.getUserId()));
            }
            if (StrUtil.isNotBlank(dto.getKeyword())) {
                Predicate nickPredicate = builder.like(root.get("nick"), "%%%s%%".formatted(dto.getKeyword()));
                Predicate characterPredicate = builder.like(root.get("character"), "%%%s%%".formatted(dto.getKeyword()));
                Predicate keywordPredicate = builder.or(nickPredicate, characterPredicate);
                list.add(keywordPredicate);
            }
            return query.where(list.toArray(new Predicate[list.size()])).getRestriction();
        });
        PageRequest page = PageRequest.of(dto.getPageIndex() - 1, dto.getPageSize());
        Page<Nick> nickList = nickRepository.findAll(specification, page);
        return new ListQueryVO<>(nickList.toList(), nickList.getTotalElements(), nickList.getTotalPages());
    }

    /**
     * 查询快捷查询链接参数
     *
     * @param dto
     * @return
     */
    @Override
    public ListQueryVO<QueryMap> getQueryMapList(GetQueryMapListDTO dto) {
        Specification<QueryMap> specification = ((root, query, builder) -> {
            if (StrUtil.isNotBlank(dto.getKeyword())) {
                Predicate predicate = builder.like(root.get("queryContent"), dto.getKeyword());
                return query.where(predicate).getRestriction();
            } else {
                return query.where().getRestriction();
            }
        });
        PageRequest page = PageRequest.of(dto.getPageIndex() - 1, dto.getPageSize());
        Page<QueryMap> queryResult = queryMapRepository.findAll(specification, page);
        return new ListQueryVO<>(queryResult.toList(), queryResult.getTotalElements(), queryResult.getTotalPages());
    }

    /**
     * 修改快捷查询
     *
     * @param dto
     */
    @Override
    public void updateQueryMap(UpdateQueryMapDTO dto) {
        Integer id = dto.getId();
        Optional<QueryMap> queryMapOpt = queryMapRepository.findById(id);
        QueryMap queryMap = queryMapOpt.orElseThrow(() -> new EntityNotExistException("快捷查询记录不存在"));
        queryMap.setQueryUrl(dto.getQueryUrl());
        queryMap.setQueryContent(dto.getQueryContent());
        queryMapRepository.save(queryMap);
    }

    /**
     * 添加快捷查询
     *
     * @param dto
     */
    @Override
    public void addQueryMap(UpdateQueryMapDTO dto) {
        String queryContent = dto.getQueryContent();
        QueryMap queryMap = queryMapRepository.findByQueryContentIgnoreCase(queryContent);
        if (queryMap != null) {
            throw new EntityAlreadyExistException("快捷查询已存在");
        }
        queryMap = new QueryMap();
        queryMap.setQueryContent(dto.getQueryContent());
        queryMap.setQueryUrl(dto.getQueryUrl());
        queryMapRepository.save(queryMap);
    }

    public static List<BigDecimal> extraExpProcess(String html) {
//        String html = FileUtil.readAsString(new File("F:\\pdf\\CoderMinnan.html"));
        Document doc = Jsoup.parse(html);
        Elements progressLevel = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[2]/div[2]/div[2]/canvas");
        String id = progressLevel.get(0).attr("id");
        String spec = "const (.{8})=document\\.getElementById\\('" + id + "'\\)";
        String p = ReUtil.getGroup1(spec, html);

        int i = html.indexOf("new Chart(" + p + ",");
        int charIndex = i + ("new Chart(" + p + ",").length();
        while (html.charAt(charIndex) != '{') {
            charIndex++;
        }
        charIndex++;

        List<Character> objContentList = new ArrayList<>();
        Stack<Character> bracket = new Stack<>();//花括号匹配栈
        bracket.add('{');//初始化栈
        objContentList.add('{');
        while (!bracket.isEmpty()) {//栈空时表示函数内容结束
            char c = html.charAt(charIndex);
            objContentList.add(c);//储存函数内容
            if (c == '}') {//遇到右括号则弹出一个左括号
                bracket.pop();
            } else if (c == '{') {
                bracket.add('{');//遇到左括号则将左括号压入匹配栈中
            }
            charIndex++;
        }

        String content = objContentList.stream().map(Object::toString).collect(Collectors.joining());
        String dataLabel = ReUtil.getGroup1("\"data\":(\\[.*\\]),", content);
        JSONArray dataArray = JSONUtil.parseArray(dataLabel);

        return dataArray.stream()
                .map(e -> {
                    try {
                        return (BigDecimal) e;
                    } catch (Exception ex) {
                        return BigDecimal.ZERO;
                    }
                })
                .collect(Collectors.toList());
    }


    @Override
    public void expDailyTask(int pageIndex) {
        DateTime now = DateTime.now();
        DateTime expiredDate = DateTime.now().offset(DateField.DAY_OF_YEAR, -30);

        Specification<CharacterRecord> spec = (((root, query, builder) -> {
            List<Predicate> list = new ArrayList<>();
            list.add(builder.lessThan(root.get("updateTime"), now.toDateStr()));
            list.add(builder.greaterThan(root.get("queryTime"), expiredDate.toDateStr()));
            return query.where(list.toArray(new Predicate[2])).getRestriction();
        }));
        PageRequest pageRequest = PageRequest.of(pageIndex, 200);
        Page<CharacterRecord> pageRes = characterRecordRepository.findAll(spec, pageRequest);
        List<CharacterRecord> executeTargetList = pageRes.getContent();
//        List<CharacterRecord> allCharacterList = characterRecordRepository.findAll(pageRequest);


        if (executeTargetList.isEmpty()) {
            log.warn("查询目标为空");
            return;
        }
        for (CharacterRecord characterRecord : executeTargetList) {
            try {
                fetchCharacterExp(characterRecord);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("程序休眠异常", e);
                }
            } catch (Exception e) {
                log.error("查询经验任务异常", e);
            }
        }
//        try {
//            Thread.sleep(3 * 60 * 1000);
//        } catch (InterruptedException e) {
//            log.error("程序休眠异常", e);
//        }
//        expDailyTask(pageIndex + 1);

//        List<List<CharacterRecord>> recordSegmentList = ListUtil.split(executeTargetList, 300);
//        for (List<CharacterRecord> segment : recordSegmentList) {
//            segment.forEach(this::fetchCharacterExp);
//            try {
//                Thread.sleep(3 * 60 * 1000);
//            } catch (InterruptedException e) {
//                log.error("程序休眠异常", e);
//            }
//        }

    }

    @Deprecated
    public void fetchCharacterExp(CharacterRecord character) {
        String characterName = character.getCharacterName();
        String region = character.getRegion();

        String url = "https://www.nexon.com/api/maplestory/no-auth/ranking/v2/%s?type=overall&id=weekly&reboot_index=0&page_index=1&character_name=%s"
                .formatted(region, characterName);

        HttpResponse expResponse = HttpUtil.createGet(url).execute();
        String expResponseJsonString = expResponse.body();
        JSONArray ranksJsonArray;
        try {
            JSONObject expResponseJson = JSONUtil.parseObj(expResponseJsonString);
            ranksJsonArray = expResponseJson.getJSONArray("ranks");

        }  catch (JSONException e) {
            log.warn("查询信息为空，角色：[{}]",characterName);
            log.info("查询到空信息：{}", expResponseJsonString);
            try {
                Thread.sleep(10 * 60 * 1000);
            } catch (Exception e1) {
                log.error("程序休眠异常", e1);
            }
            return;
        } catch (Exception e) {
            log.warn("查询信息为空，角色：[{}]",characterName);
            log.info("查询到空信息：{}", expResponseJsonString);
            return;
        }


        JSONObject data = ranksJsonArray.getJSONObject(0);

        DateTime now = DateTime.now();
        String time = now.toString("yyyy-MM-dd HH:mm:ss");
        String recordDate = now.toString("yyyy-MM-dd");

        CharacterExpDaily expRecord = new CharacterExpDaily(character, recordDate, data);
        expRecord.setCreateTime(time);

        LvExp lvExp = lvExpRepository.findByLv(Integer.parseInt(expRecord.getLevel()));
        BigDecimal expRequired = new BigDecimal(lvExp.getExpToNextLevel());
        BigDecimal currentExp = new BigDecimal(expRecord.getCurrentExp());
        BigDecimal epxPercent = currentExp.divide(expRequired, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        String expPercent = String.valueOf(epxPercent.doubleValue());
        expRecord.setLevelPercent(expPercent);

        Specification<CharacterExpDaily> spec = (((root, query, builder) -> {
            List<Predicate> list = new ArrayList<>();
            list.add(builder.equal(root.get("characterId"), character.getId()));
            list.add(builder.equal(root.get("recordDate"), recordDate));
            return query.where(list.toArray(new Predicate[2])).getRestriction();
        }));
        Optional<CharacterExpDaily> expRecordOpt = characterExpDailyRepository.findOne(spec);
        expRecordOpt.ifPresent(characterExpDaily -> expRecord.setId(characterExpDaily.getId()));
        characterExpDailyRepository.save(expRecord);

        character.setLevel(data.getInt("level"));
//        character.setJobId(data.getInt("jobID"));
//        character.setJobDetail(data.getInt("jobDetail"));
        //2025年11月14日 官网API变更为直接提供职业名，无需自己匹配
        character.setJobName(data.getStr("jobName"));
        character.setCharacterImgUrl(data.getStr("characterImgURL"));
        character.setLevelPercent(expPercent);
        character.setUpdateTime(time);
        characterRecordRepository.save(character);

        log.info("完成角色经验数据查询，角色：[{}]", characterName);
    }

    @Override
    @Transactional(value = "mysqlTransactionManager")
    public Optional<CharacterData> queryCharacterInfoLocal(String queryName, String region) {
//        CharacterRecord characterRecord = characterRecordRepository.getByCharacterNameIgnoreCaseAndRegion(queryName, region);
        DateTime now = DateTime.now();
        CharacterData characterData;
        CharacterRecordMysql record;
        List<LvExp> lvExpList = lvExpRepository.findAll();
        Map<Integer, Long> lvExpMap = lvExpList.stream().collect(Collectors.toMap(LvExp::getLv, e -> e.getExpToNextLevel() == null ? 0L : Long.parseLong(e.getExpToNextLevel())));

        try {
            Optional<CharacterRecordMysql> recordOpt = characterRecordMysqlRepository
                    .findFirstByCharacterNameIgnoreCaseAndRegion(queryName.toLowerCase(), region);
            if (recordOpt.isEmpty()) {
                return Optional.empty();
            }

            record = recordOpt.get();
            characterData = new CharacterData();
            characterData.setName(record.getCharacterName());
            characterData.setCharacterImgUrl(record.getCharacterImgUrl());
            characterData.setJob(record.getJobName());
            Integer worldId = record.getWorldId();
            characterData.setWorldId(worldId);
            characterData.setServer(wordMap.getOrDefault(worldId, String.valueOf(worldId)));
            characterData.setLevel(record.getLevel() == null ? null : record.getLevel().toString());
            if (record.getLevelPercent() != null) {
                characterData.setExpPercent(record.getLevelPercent().stripTrailingZeros().toPlainString());
            }
            if (record.getUpdateTime() != null) {
                characterData.setUpdateTime(DateTime.of(record.getUpdateTime()).toString("yyyy-MM-dd HH:mm:ss"));
            }
            if (record.getQueryTime() != null) {
                characterData.setQueryTime(DateTime.of(record.getQueryTime()).toString("yyyy-MM-dd HH:mm:ss"));
            }
            characterData.setSource("minnan.site");

            if (record.getLegion() != null && record.getLegion() > 0) {
                characterData.setLegionLevel(record.getLegion().toString());
                if (record.getLegionRank() != null) {
                    characterData.setLegionRank(record.getLegionRank().toString());
                }
                if (record.getLegionRaidPower() != null) {
                    characterData.setLegionPower(record.getLegionRaidPower().toString());
                }
            }

            CharacterRecordMysqlRepository.RankProjection rankData = characterRecordMysqlRepository.calcRank(
                    record.getLevel(),
                    record.getLevelPercent(),
                    record.getId(),
                    record.getWorldId(),
                    record.getJobName(),
                    region
            );
            if (rankData != null) {
                characterData.setGlobalLevelRank(rankData.getRankGlobal().toString());
                characterData.setServerLevelRank(rankData.getRankWorld().toString());
                characterData.setGlobalClassRank(rankData.getRankJob().toString());
                characterData.setServerClassRank(rankData.getRankWorldJob().toString());
            } else {
                characterData.setRankEmpty();
            }

            if (characterData.getServerClassRank() != null) {
                Integer rn = Integer.parseInt(characterData.getServerClassRank());
                List<CharacterRecordMysqlRepository.NearRankProjection> nearRankData =
                        characterRecordMysqlRepository.findNearRank(record.getWorldId(), record.getJobName(), rn);
                List<Integer> nearRankCharacterIdList = nearRankData.stream().map(e -> e.getId()).toList();


                LocalDate endTime = DateUtil.toLocalDateTime(now).toLocalDate();
                LocalDate startTime = DateUtil.toLocalDateTime(DateUtil.offsetDay(now, -8)).toLocalDate();
                List<CharacterExpDailyMysql> nearRankExpData = characterExpDailyMysqlRepository.findByIdCharacterIdInAndIdRecordDateBetween(nearRankCharacterIdList,
                        startTime, endTime);
                Map<Integer, List<CharacterExpDailyMysql>> nearRankExpMap = nearRankExpData.stream().collect(Collectors.groupingBy(
                        e -> e.getId().getCharacterId(),
                        Collectors.toList()
                ));

                List<CharacterData> nearRankList = nearRankData.stream()
                        .map(r -> {
                            CharacterData item = new CharacterData();
                            Integer id = r.getId();
                            item.setName(r.getCharacterName());
                            Integer level = r.getLevel();
                            BigDecimal levelPercent = r.getLevelPercent();
                            item.setLevel(level.toString());
                            item.setServerClassRank(r.getRn().toString());
                            if (nearRankExpMap.containsKey(id)) {
                                List<CharacterExpDailyMysql> expRecordList = nearRankExpMap.get(id);
                                List<ExpData> expData = toExpData(expRecordList, lvExpMap);
                                double avgExp = expData.stream().limit(7)
                                        .mapToLong(ExpData::expDifference)
                                        .average()
                                        .orElse(0);
                                item.setAvgExp7((long) avgExp);
                                CharacterExpDailyMysql lastDayExp = expRecordList.get(expRecordList.size() - 1);
                                item.setCurrentExp(lastDayExp.getCurrentExp());
                                Long expNeed = lvExpMap.get(lastDayExp.getLevel());
                                item.setExpNeed(expNeed);
                                item.setExpPercent(lastDayExp.getLevelPercent() + "%");
                            }
                            return item;
                        })
                        .toList();

                characterData.setNearRank(nearRankList);
            }

            characterRecordMysqlRepository.updateQueryTime(record.getId());
        } catch (Exception e) {
            log.error("查询角色信息异常", e);
            return Optional.empty();
        }

        if (characterData == null) {
            return Optional.empty();
        }

        if (characterData.getExpPercent() != null) {
            characterData.setExpPercent(characterData.getExpPercent() + "%");
        }

        if (characterData.getLegionLevel() != null) {
            BigDecimal legionPower = new BigDecimal(characterData.getLegionPower());
            BigDecimal coinsPerDay = legionPower
                    .multiply(BigDecimal.valueOf(60 * 60 * 24))
                    .divide(BigDecimal.valueOf(100_000_000_000L))
                    .divide(new BigDecimal("1.08"), 2, RoundingMode.HALF_UP);
            characterData.setLegionCoinsPerDay(coinsPerDay.toString());
        }

        //处理经验数据
        List<CharacterExpDailyMysql> expRecordList =
                characterExpDailyMysqlRepository.findRecentByCharacterId(record.getId());
        if (expRecordList == null || expRecordList.isEmpty()) {
            characterData.setExpData(Collections.emptyList());
            return Optional.of(characterData);
        }

        String startDate = DateTime.now().offset(DateField.DAY_OF_YEAR, -15).toString("yyyy-MM-dd");
//        List<CharacterExpDaily> expRecordList = characterExpDailyRepository.findAllByCharacterIdAndRecordDateAfter(characterRecord.getId(), startDate);

        List<ExpData> expDataList = toExpData(expRecordList, lvExpMap);
        characterData.setExpData(expDataList);

        CharacterExpDailyMysql lastDayExp = expRecordList.get(expRecordList.size() - 1);
        characterData.setCurrentExp(lastDayExp.getCurrentExp());
        Long expNeed = lvExpMap.get(lastDayExp.getLevel());
        characterData.setExpNeed(expNeed);

        return Optional.of(characterData);
    }

    @Override
    public String createCharacterHtml(CharacterData characterData) {
        DateTime now = DateTime.now();

        Context context = new Context();
        context.setVariable("c", characterData);

        String characterImgUrl = characterData.getCharacterImgUrl();
        //下载角色证件照，直接用浏览器启动不能使用代理，先下载然后转base64可以快一点
        if (StrUtil.isNotBlank(characterImgUrl)) {
            HttpResponse imgRequest = HttpUtil.createGet(characterImgUrl).setProxy(proxy).execute();
            String imgBase64 = Base64.encode(imgRequest.bodyStream());
            context.setVariable("img", "data:image/png;base64,%s".formatted(imgBase64));
        }

        List<ExpData> expData = characterData.getExpData();
        //截取最后14天经验数据
        expData = ListUtil.sub(expData, expData.size() - 14, expData.size());
        List<String> dateList = expData.stream().map(e -> e.dateLabel()).toList();
        BigDecimal billionNumber = BigDecimal.valueOf(billion);
        List<Double> expList = expData.stream()
                .map(e -> BigDecimal.valueOf(e.expDifference()))
//                .map(e -> NumberUtil.round(e / billion, 4))
                .map(e -> e.divide(billionNumber, 4, RoundingMode.HALF_UP))
                .map(e -> e.doubleValue())
                .toList();
        List<Double> processList = expData.stream()
                .map(e -> e.expProcess())
                .toList();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("noteDate", dateList)
                .set("exp", expList)
                .set("process", processList);

        List<ExpData> reverseExpData = ListUtil.reverseNew(expData);

        double sum7 = reverseExpData.stream()
                .limit(7)
                .mapToDouble(e -> (double) e.expDifference() / billion)
                .sum();
        context.setVariable("sum7", "%.4fb".formatted(sum7));
        context.setVariable("avg7", "%.4fb".formatted(BigDecimal.valueOf(sum7).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP).doubleValue()));

        double sum14 = reverseExpData.stream()
                .limit(14)
                .mapToDouble(e -> (double) e.expDifference() / billion)
                .sum();

        context.setVariable("sum14", "%.4fb".formatted(sum14));
        context.setVariable("avg14", "%.4fb".formatted(BigDecimal.valueOf(sum14).divide(BigDecimal.valueOf(14), 4, RoundingMode.HALF_UP).doubleValue()));

        List<List<ExpData>> expDataSplit = ListUtil.split(expData, 5);
        //拆分成三栏进行展示
        context.setVariable("exp1", expDataSplit.get(0));
        context.setVariable("exp2", expDataSplit.get(1));
        context.setVariable("exp3", expDataSplit.get(2));

        //经验预测
        PageRequest page = PageRequest.of(0, 10);
        int lv = Integer.parseInt(characterData.getLevel());
        List<LvExp> stageList = lvExpRepository.findByLvGreaterThanEqual(lv, page);
        if (stageList.isEmpty() || sum7 == 0) {
            context.setVariable("levelPredicate", Collections.emptyList());
        } else {
            //计算7日均经验
            double avg7 = BigDecimal.valueOf(sum7).divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP).doubleValue() * billion;
            Iterator<LvExp> itr = stageList.iterator();
            LvExp currentStage = itr.next();
            //当前等级经验百分比
            double expPercent = Double.parseDouble(characterData.getExpPercent().replace("%", ""));
            //当前等级升级经验
            long expToNextLevel = Long.parseLong(currentStage.getExpToNextLevel());
            //当前刷了多少经验
            long currentExp = (long) (expToNextLevel * (expPercent / 100));
            //需要多少经验升级
            long expNeed = expToNextLevel - currentExp;
            //这一级还需要刷多少天升级
            int dayNeed = NumberUtil.round(expNeed / avg7, 0).intValue();
            ReckonLevel reckonItem = new ReckonLevel(lv + 1, now.offsetNew(DateField.DAY_OF_YEAR, dayNeed).toDateStr());
            ArrayList<ReckonLevel> reckonList = ListUtil.toList(reckonItem);

            while (itr.hasNext()) {
                LvExp lvInfo = itr.next();
                lv = lvInfo.getLv() + 1;
                //等级大于300结束预测
                if (lv > 300) {
                    break;
                }
                expNeed = Long.parseLong(lvInfo.getExpToNextLevel());
                dayNeed = dayNeed + (int) (expNeed / avg7);
                reckonItem = new ReckonLevel(lv, now.offsetNew(DateField.DAY_OF_YEAR, dayNeed).toDateStr());
                //大于10年的结束预测
                if (dayNeed > 365 * 10) {
                    break;
                }
                reckonList.add(reckonItem);
            }
            context.setVariable("levelPredicate", reckonList);
        }

        context.setVariable("expDataString", jsonObject);

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        String html = templateEngine.process("picTemplate/query", context);

        return html;
    }

    private record ReckonLevel(int lv, String dateLabel) {
    }

    private List<ExpData> toExpData(List<CharacterExpDailyMysql> expRecordList, Map<Integer, Long> lvExpMap) {
        expRecordList.sort(Comparator.comparing(e -> e.getId().getRecordDate()));
        List<ExpData> expDataList = new ArrayList<>();
        if (expRecordList.size() < 15) {
            int lackDay = 15 - expRecordList.size();
            CharacterExpDailyMysql d = expRecordList.get(0);
            DateTime firstDate = DateTime.of(d.getId().getRecordDate().toString(), "yyyy-MM-dd");
            int level = d.getLevel();
            double levelPercent = level + d.getLevelPercent().doubleValue() / 100;
            for (int i = 0; i < lackDay; i++) {
                DateTime date = firstDate.offsetNew(DateField.DAY_OF_YEAR, i - lackDay);
                expDataList.add(new ExpData(date.toString("M/dd"), 0L, levelPercent));
            }
        }

        if (expRecordList.size() > 1) {
            for (int i = 0; i < expRecordList.size() - 1; i++) {
                CharacterExpDailyMysql item = expRecordList.get(i);
                int level1 = item.getLevel();
//                Double levelPercent = level1 + Double.parseDouble(item.getLevelPercent()) / 100;
                Long currentExp = item.getCurrentExp();
                CharacterExpDailyMysql nextItem = expRecordList.get(i + 1);
                Long nextExp = nextItem.getCurrentExp();
                int level2 = nextItem.getLevel();
                Double levelPercent = level2 + nextItem.getLevelPercent().doubleValue() / 100;
                BigDecimal expDifference;
                if (level1 == level2) {
                    expDifference = BigDecimal.valueOf(nextExp).subtract(BigDecimal.valueOf(currentExp));
                } else {
                    //升级的情况
                    Long fullExp = lvExpMap.get(level1);
                    expDifference = BigDecimal.valueOf(fullExp).subtract(BigDecimal.valueOf(currentExp)).add(BigDecimal.valueOf(nextExp));
                    while (++level1 < level2) {
                        fullExp = lvExpMap.get(level1);
                        expDifference = expDifference.add(BigDecimal.valueOf(fullExp));
                    }
                }
                DateTime recordDate = DateTime.of(item.getId().getRecordDate().toString(), "yyyy-MM-dd");

                String recordDateStr = recordDate.toString("M/dd");
                long dayDiffer = DateUtil.betweenDay(recordDate, DateTime.of(nextItem.getId().getRecordDate().toString(), "yyyy-MM-dd"), true);
                if (dayDiffer > 1) {
                    double process = expDataList.isEmpty() ? levelPercent : expDataList.get(expDataList.size() - 1).expProcess();
                    for (int j = 0; j < dayDiffer - 1; j++) {
                        String fillDate = recordDate.offsetNew(DateField.DAY_OF_YEAR, j).toString("M/dd");
                        expDataList.add(new ExpData(fillDate, 0L, process));
                    }
                    recordDateStr = recordDate.offsetNew(DateField.DAY_OF_YEAR, (int) dayDiffer - 1).toString("M/dd");
                }
                ExpData expData = new ExpData(recordDateStr, expDifference.longValue(), levelPercent);
                expDataList.add(expData);
            }
        }

        return expDataList;
    }



    /**
     * 初始化角色信息
     *
     * @param queryName
     * @param region
     */
    @Override
    public void initCharacter(String queryName, String region) {

        String infoUrl = "https://www.nexon.com/api/maplestory/no-auth/ranking/v2/%s?type=overall&id=weekly&reboot_index=0&page_index=1&character_name=%s"
                .formatted(region, queryName);
        HttpResponse infoResponse = HttpUtil.createGet(infoUrl).execute();
        String infoResponseJsonString = infoResponse.body();
        JSONObject infoObj = JSONUtil.parseObj(infoResponseJsonString);
        JSONArray ranks = infoObj.getJSONArray("ranks");
        if (ranks.isEmpty()) {
            throw new EntityNotFoundException("角色不存在");
        }

        JSONObject characterJson = ranks.getJSONObject(0);
        CharacterRecord characterRecord = new CharacterRecord();
        characterRecord.setCharacterName(characterJson.getStr("characterName"));
        characterRecord.setRegion(region);
        characterRecord.setWorldId(characterJson.getInt("worldID"));
        Integer level = characterJson.getInt("level");
        characterRecord.setLevel(level);

        String currentExpStr = characterJson.getStr("exp");
        LvExp lvExp = lvExpRepository.findByLv(level);
        BigDecimal expRequired = new BigDecimal(lvExp.getExpToNextLevel());
        BigDecimal currentExp = new BigDecimal(currentExpStr);
        BigDecimal epxPercent = currentExp.divide(expRequired, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        String expPercent = String.valueOf(epxPercent.doubleValue());
        characterRecord.setLevelPercent(expPercent);

//        characterRecord.setJobId(characterJson.getInt("jobID"));
//        characterRecord.setJobDetail(characterJson.getInt("jobDetail"));
        characterRecord.setJobName(characterJson.getStr("jobName"));
        characterRecord.setCharacterImgUrl(characterJson.getStr("characterImgURL"));

        DateTime now = DateTime.now();
        String time = now.toString("yyyy-MM-dd HH:mm:ss");
        characterRecord.setUpdateTime(time);
        characterRecord.setQueryTime(time);
        characterRecordRepository.save(characterRecord);

        CharacterExpDaily expRecord = new CharacterExpDaily(characterRecord, now.toDateStr(), characterJson);
        expRecord.setCreateTime(time);

        expRecord.setLevelPercent(expPercent);
        characterExpDailyRepository.save(expRecord);
    }

    private void getCharacterRank(String url, Consumer<String> setter) {
        HttpResponse rankResponse = HttpUtil.createGet(url).execute();
        String rankResponseJsonString = rankResponse.body();
        JSONObject rankObj = JSONUtil.parseObj(rankResponseJsonString);
        JSONArray ranks = rankObj.getJSONArray("ranks");
        if (!ranks.isEmpty()) {
            JSONObject rankInfo = ranks.getJSONObject(0);
            setter.accept(rankInfo.getStr("rank"));
        } else{
            setter.accept("-");
        }
    }

    private static final Map<Integer, String> wordMap;

    static {
        wordMap = Map.of(45, "Kronos",
                70, "Hyperion",
                1, "Bera",
                19, "Scania",
                30, "Luna",
                46, "Solis");

    }
}

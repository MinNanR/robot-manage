package site.minnan.robotmanage.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.NumberChineseFormatter;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.Nick;
import site.minnan.robotmanage.entity.aggregate.QueryMap;
import site.minnan.robotmanage.entity.dao.NickRepository;
import site.minnan.robotmanage.entity.dao.QueryMapRepository;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.GetQueryMapListDTO;
import site.minnan.robotmanage.entity.dto.UpdateQueryMapDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;
import site.minnan.robotmanage.entity.vo.bot.ExpData;
import site.minnan.robotmanage.infrastructure.exception.EntityAlreadyExistException;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.service.CharacterSupportService;

import java.net.Proxy;
import java.time.Duration;
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

    public static final String RANK_CACHE_KEY_TEMPLATE = "rank:%s:%s:%d";

    private RedisUtil redisUtil;

    private QueryMapRepository queryMapRepository;

    private NickRepository nickRepository;

    @Autowired
    @Qualifier("proxy")
    private Proxy proxy;

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
    @Cacheable(value = "query", keyGenerator = "queryKeyGenerator")
    public CharacterData fetchCharacterInfo(String queryName) {
        String queryUrl = BASE_QUERY_URL + queryName.strip();

        HttpRequest queryRequest = HttpUtil.createGet(queryUrl).setProxy(proxy);
        HttpResponse queryRes = queryRequest.execute();
        String html = queryRes.body();

        Document doc = Jsoup.parse(html, "UTF-8");

        //排名信息第一个元素为查询角色排名，第二个元素为排名附近角色排名信息，第二个元素内的第三个元素为区排名
        JSONArray rankInfo = new JSONArray();
        if (ReUtil.contains("fetch\\('(.*?)'\\)", html)) {
            //排名信息查询链接有有效期，有效期比较短，可能就几秒钟，所以要先查询出排名信息再作后续解析
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
//        String img = getText.apply("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/img");
        String img = doc.selectXpath("/html/body/main/div/div/div[2]/div[2]/div[1]/div[1]/div/img").get(0).attr("src");
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
        } catch (Exception ignored){

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
            long expNumber = NumberUtil.isNumber(exp) ? Long.parseLong(exp) : 0;
            ExpData expData = new ExpData(date, expNumber);
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

        QueryMap queryMap = queryMapRepository.findByQueryContent(queryContent);
        if (queryMap == null) {
            throw new EntityNotFoundException();
        }

        String queryUrl = queryMap.getQueryUrl();
        String fullQueryUrl = StrUtil.format(queryUrl, rank);

        HttpResponse queryResponse = HttpUtil.createGet(fullQueryUrl).execute();
        String responseJsonString = queryResponse.body();
        JSONArray queryResultList = JSONUtil.parseArray(responseJsonString);
        if (CollectionUtil.isEmpty(queryResultList)) {
            throw new EntityNotFoundException();
        }

        queryResultList.stream()
                .map(e -> (JSONObject) e)
                .forEach(e -> {
                    String key = RANK_CACHE_KEY_TEMPLATE.formatted(today, queryContent, e.getInt("Rank"));
                    String value = e.getStr("CharacterName");
                    redisUtil.valueSet(key, value, Duration.ofHours(6));
                });

        JSONObject targetCharacter = queryResultList.getJSONObject(0);
        return targetCharacter.getStr("CharacterName");
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
        QueryMap queryMap = queryMapRepository.findByQueryContent(queryContent);
        if (queryMap != null) {
            throw new EntityAlreadyExistException("快捷查询已存在");
        }
        queryMap = new QueryMap();
        queryMap.setQueryContent(dto.getQueryContent());
        queryMap.setQueryUrl(dto.getQueryUrl());
        queryMapRepository.save(queryMap);
    }
}

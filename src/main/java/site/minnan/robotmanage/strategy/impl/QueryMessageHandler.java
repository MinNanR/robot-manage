package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import site.minnan.robotmanage.entity.aggregate.LvExp;
import site.minnan.robotmanage.entity.dao.LvExpRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.dto.SendMessageDTO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;
import site.minnan.robotmanage.entity.vo.bot.ExpData;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.service.BotService;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 角色信息查询消息处理
 *
 * @author Minnan on 2024/01/15
 */
@Component("query")
@Slf4j
public class QueryMessageHandler implements MessageHandler {


    private CharacterSupportService characterSupportService;

    private LvExpRepository lvExpRepository;

    private BotService botService;

    private RedisUtil redisUtil;

    private static final Integer billion = 1000000000;

    @Value("${query.pythonPath}")
    private String pythonPath;

    @Value("${query.folder}")
    public String folder;

    @Value("${query.baseUrl}")
    private String baseUrl;

    private Proxy proxy;

    private static final ExecutorService queryExecutorPool = Executors.newFixedThreadPool(5);

    /**
     * 记录每个用户正在查询的用户的查询目标
     */
    public static final ConcurrentHashMap<String, MessageDTO> userQueryTaskMap;

    private static final String USER_QUERY_TASK_KEY_TEMPLATE = "query:%s";

    static {
        userQueryTaskMap = new ConcurrentHashMap<>();
    }

    public QueryMessageHandler(CharacterSupportService characterSupportService, LvExpRepository lvExpRepository, BotService botService, Proxy proxy) {
        this.characterSupportService = characterSupportService;
        this.lvExpRepository = lvExpRepository;
        this.botService = botService;
        this.proxy = proxy;
    }

    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        if (queryExecutorPool.isShutdown()) {
            return Optional.of("查询功能已关闭，请稍后再试");
        }
        int waitingCount = ((ThreadPoolExecutor) queryExecutorPool).getQueue().size();
        String groupId = dto.getGroupId();
        if (waitingCount >= 20 && !"122820573".equals(groupId)) {
            return Optional.of("服务器繁忙，请稍后再试");
        }
        String userId = dto.getSender().userId();
        String taskKey = USER_QUERY_TASK_KEY_TEMPLATE.formatted(userId);
        if (redisUtil.hasKey(taskKey)) {
            String queryContent = (String) redisUtil.getValue(taskKey);
            String reply = "查询%s进行中，请勿重复操作".formatted(queryContent);
            return Optional.of(reply);
        }
        String queryTarget = dto.getRawMessage().substring(2).strip();
        redisUtil.valueSet(taskKey, queryTarget, Duration.ofMinutes(3));
        userQueryTaskMap.put(userId, dto);
        queryExecutorPool.submit(() -> {
            Optional<String> queryResultOpt = doQuery(dto);
            SendMessageDTO sendMessageDTO = new SendMessageDTO(dto, queryResultOpt.orElse("查询失败"));
            if (userQueryTaskMap.containsKey(userId)) {
                botService.sendAsyncMessage(sendMessageDTO);
                redisUtil.delete(taskKey);
                userQueryTaskMap.remove(userId);
            }
        });
        return Optional.of("");
    }

    /**
     * 执行查询任务
     *
     * @param dto
     * @return
     */
    public Optional<String> doQuery(MessageDTO dto) {
        String userId = dto.getSender().userId();

        String message = dto.getRawMessage();
        String queryContent = message.substring(2).strip();
        String queryTarget;
        try {
            queryTarget = characterSupportService.parseQueryContent(queryContent, userId);
            log.info("查询内容为[{}]，解析出查询目标为：[{}]", queryContent, queryTarget);
        } catch (EntityNotFoundException e) {
            log.warn("解析查询内容失败，查询内容为{}", queryContent);
            return Optional.of("查询失败");
        }

        String today = DateTime.now().offset(DateField.HOUR, -8).toString("yyyMMdd");
        Supplier<String> getResult = () -> {
            String url = "%s/%s/%s.png".formatted(baseUrl, today, queryTarget.toLowerCase());
            return "[CQ:image,file=%s,subType=0]".formatted(url);
        };

        String pngPath = "%s/%s/%s.png".formatted(folder, today, queryTarget.toLowerCase());
        if (FileUtil.exist(pngPath)) {
            //经验异常的角色可以尝试重新查询，每天可以重试5次，
            //经验异常：昨日经验为0
            String exceptExpKey = exceptExpKey(today, queryTarget.toLowerCase());
            if (!redisUtil.hasKey(exceptExpKey)) {
                return Optional.of(getResult.get());
            } else {
                int countDown = (int) redisUtil.getValue(exceptExpKey);
                countDown -= 1;
                if (countDown == 0) {
                    redisUtil.delete(exceptExpKey);
                } else {
                    redisUtil.valueSet(exceptExpKey, countDown);
                }
            }
        }

        CharacterData c;
        try {
            String server = (String) dto.getPayload().getOrDefault("server", "u");
            c = characterSupportService.fetchCharacterInfo(queryTarget, server);
        } catch (Exception e) {
            log.error("查询角色信息失败，查询目标为" + queryTarget, e);
            return Optional.of("查询失败");
        }

        createPic(c);
        int queryCount = characterSupportService.getQueryCount(queryTarget, dto.getSender().userId());
        if (queryCount >= 3) {
//            sb.append("[CQ:image,file=https://minnan.site:2005/rot/20240118/toomuch.jpg,subType=0]");
            String result = getResult.get();
            result = result + "[CQ:image,file=https://minnan.site:2005/rot/20240118/toomuch.jpg,subType=0]";
            return Optional.of(result);
        } else {
            return Optional.of(getResult.get());
        }


    }

    /**
     * 绘制角色信息图
     *
     * @param characterData 角色信息
     */
    private void createPic(CharacterData characterData) {
        DateTime now = DateTime.now();
        String today = now.offset(DateField.HOUR, -8).toString("yyyyMMdd");

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

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
        //经验异常的角色可以尝试重新查询，每天可以重试5次，
        //经验异常：昨日经验为0
        if (reverseExpData.get(0).expDifference() == 0) {
            redisUtil.setnx(exceptExpKey(today, characterData.getName().toLowerCase()), 5, Duration.ofDays(1));
        } else {
            redisUtil.delete(exceptExpKey(today, characterData.getName().toLowerCase()));
        }

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
        //使用模板引擎，生成html代码
        String html = templateEngine.process("picTemplate/query", context);


        String folderPath = "%s/%s".formatted(folder, today);
        File folderFile = new File(folderPath);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        //html文件路径
        String htmlPath = "%s/%s/%s.html".formatted(folder, today, characterData.getName());
        //png图片文件路径
        String pngPath = "%s/%s/%s.png".formatted(folder, today, characterData.getName().toLowerCase());
        //将html代码写入文件
        FileUtil.writeString(html, htmlPath, Charset.defaultCharset());
        //调用python代码，使用无头浏览器渲染html代码后截图
        RuntimeUtil.execForStr("python3 %s %s %s".formatted(pythonPath, htmlPath, pngPath));
    }

    private record ReckonLevel(int lv, String dateLabel) {
    }

    private static final String TASK_SAVE_KEY = "taskSave";

    /**
     * 应用关闭时等待所有查询任务结束
     */
    public void beforeApplicationShutdown() {
        //关闭线程池
        queryExecutorPool.shutdown();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("线程暂停异常", e);
        }
        if (!queryExecutorPool.isTerminated()) {
            log.info("仍有查询任务进行中，等待查询任务结束");
            try {
                //等待所有查询任务结束，返回true是全部任务正常结束，返回false是超时
                boolean terminated = queryExecutorPool.awaitTermination(5, TimeUnit.MINUTES);
                if (terminated) {
                    log.info("所有查询任务已结束");
                    return;
                }
            } catch (InterruptedException e) {
                log.info("等待查询任务结束时线程异常", e);
            }
        } else {
            log.info("没有查询任务进行中，将正常关闭程序");
            return;
        }

        log.info("等待查询任务结束超时，将保存查询任务到redis中");
        Collection<MessageDTO> queryTasks = userQueryTaskMap.values();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String tasksJson = objectMapper.writeValueAsString(queryTasks);
            log.info("保存查询任务,{}", tasksJson);
            redisUtil.valueSet(TASK_SAVE_KEY, tasksJson, Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            log.error("序列化查询任务失败", e);
        }
    }

    /**
     * 恢复查询任务
     */
    @PostConstruct
    public void continueTask() throws JsonProcessingException {
        if (!redisUtil.hasKey(TASK_SAVE_KEY)) {
            return;
        }
        String taskJson = (String) redisUtil.getValue(TASK_SAVE_KEY);
        log.info("恢复查询任务,{}", taskJson);
        ObjectMapper objectMapper = new ObjectMapper();
        List<MessageDTO> taskList = objectMapper.readValue(taskJson, new TypeReference<>() {
        });
        for (MessageDTO dto : taskList) {
            String userId = dto.getSender().userId();
            String taskKey = USER_QUERY_TASK_KEY_TEMPLATE.formatted(userId);
            userQueryTaskMap.put(userId, dto);
            queryExecutorPool.submit(() -> {
                Optional<String> queryResultOpt = doQuery(dto);
                SendMessageDTO sendMessageDTO = new SendMessageDTO(dto, queryResultOpt.orElse("查询失败"));
                if (userQueryTaskMap.containsKey(userId)) {
                    botService.sendAsyncMessage(sendMessageDTO);
                    redisUtil.delete(taskKey);
                    userQueryTaskMap.remove(userId);
                }
            });
        }
    }

    private String exceptExpKey(String today, String characterName) {
        return "EXCEPT_EXP:%s:%s".formatted(today, characterName);
    }

}

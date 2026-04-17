package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
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
import site.minnan.robotmanage.entity.aggregate.CharacterRecord;
import site.minnan.robotmanage.entity.aggregate.LvExp;
import site.minnan.robotmanage.entity.dao.CharacterRecordRepository;
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

    @Autowired
    private CharacterRecordRepository characterRecordRepository;

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

        Map<String, Object> payload = dto.getPayload();
        String server = MapUtil.getStr(payload, "server", "u");
        boolean personalQuery = "我".equals(queryContent) | MapUtil.getBool(payload, "personalQuery", false);
        String region = "u".equals(server) ? "na" : "eu";
        String pngPath = "%s/%s/%s.png".formatted(folder, today, queryTarget.toLowerCase());


        CharacterData c;
        try {
            Optional<CharacterData> characterDataOpt = characterSupportService.queryCharacterInfoLocal(queryTarget, region, userId);
            c = characterDataOpt.orElseThrow(() -> new EntityNotFoundException("角色不存在"));
            if (!personalQuery && StrUtil.isNotBlank(c.getQueryTime()) && FileUtil.exist(pngPath)) {
                DateTime updateTime = DateTime.of(c.getUpdateTime(), "yyyy-MM-dd HH:mm:ss");
                DateTime queryTime = DateTime.of(c.getQueryTime(), "yyyy-MM-dd HH:mm:ss");
                //如果上次查询时间比更新时间晚，则返回图片
                if (queryTime.isAfter(updateTime)) {
                    return Optional.of(getResult.get());
                }
            }
        } catch (Exception e) {
            log.error("查询角色信息失败，查询目标为" + queryTarget, e);
            return Optional.of("查询失败");
        }

        if (personalQuery) {
            createPersonalPic(c, userId);
            String url = "%s/%s/user/%s.png".formatted(baseUrl, today, userId);
            return Optional.of("[CQ:image,file=%s,subType=0]".formatted(url));
        } else {
            createPic(c);
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

        //使用模板引擎，生成html代码
        String html = characterSupportService.createCharacterHtml(characterData, "picTemplate/query");


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

    private void createPersonalPic(CharacterData characterData, String userId) {
        DateTime now = DateTime.now();
        String today = now.offset(DateField.HOUR, -8).toString("yyyyMMdd");

        //使用模板引擎，生成html代码
        String html = characterSupportService.createCharacterHtml(characterData, "picTemplate/query2");


        String folderPath = "%s/%s".formatted(folder, today);
        File folderFile = new File(folderPath);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        //html文件路径
        String htmlPath = "%s/%s/user/%s.html".formatted(folder, today, userId);
        //png图片文件路径
        String pngPath = "%s/%s/user/%s.png".formatted(folder, today, userId);
        //将html代码写入文件
        FileUtil.writeString(html, htmlPath, Charset.defaultCharset());
        //调用python代码，使用无头浏览器渲染html代码后截图
        RuntimeUtil.execForStr("python3 %s %s %s".formatted(pythonPath, htmlPath, pngPath));
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

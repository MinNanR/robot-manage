package site.minnan.robotmanage;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.config.ProxyConfig;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.MessageHandler;
import site.minnan.robotmanage.strategy.impl.HolidayMessageHandler;

import java.net.Proxy;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest(classes = BotApplication.class)
class DemoApplicationTests {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    @Qualifier("holiday")
    MessageHandler messageHandler;

    @Autowired
    HolidayMessageHandler holidayMessageHandler;

    @Autowired
    CharacterSupportService characterSupportService;

    @Autowired
    RedisUtil redisUtil;

    @Test
    void contextLoads() {
    }

    private MessageDTO initParam(String message) {
        String jsonString = "{'raw_message': '#" + message + "', 'group_id': '667082876', 'sender': {'user_id': '978312456'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        return MessageDTO.fromJson(jsonObject);
    }

    @Test
    public void testQuestion() {
        List<Question> questions = questionRepository.findAllByContentLikeIgnoreCase("火花");
        Console.log(JSONUtil.toJsonPrettyStr(questions));
        String json = "{'raw_message': '# 维护', 'group_id': '123123', 'sender': {'user_id': '1603'}, 'message_id': '46393'}";
        MessageDTO message = JSONUtil.toBean(json, MessageDTO.class);

    }

    @Test
    public void testAsk() {
        String json = "{'raw_message': '#BADELING', 'group_id': '667082876', 'sender': {'user_id': '1603'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(json);
        MessageDTO messageDTO = MessageDTO.fromJson(jsonObject);
        Optional<String> result = messageHandler.handleMessage(messageDTO);
        String s = result.orElse("无答案");
        System.out.println(s);
    }

    @Test
    public void testFlame() {
        String json = "{'raw_message': '#火花期望30', 'group_id': '667082876', 'sender': {'user_id': '1603'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(json);
        MessageDTO messageDTO = MessageDTO.fromJson(jsonObject);
        Optional<String> result = messageHandler.handleMessage(messageDTO);
        System.out.println(result.get());
    }

    @Test
    public void testDivinate() {
        String json = "{'raw_message': '#占卜', 'group_id': '667082876', 'sender': {'user_id': '1603'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(json);
        MessageDTO messageDTO = MessageDTO.fromJson(jsonObject);
        Optional<String> result = messageHandler.handleMessage(messageDTO);
        System.out.println(result.get());
    }

    @Test
    public void testQuery() {
        MessageDTO param = initParam("查询CoderMinnan");
        Optional<String> opt = messageHandler.handleMessage(param);
        System.out.println(opt.get());
    }

    @Test
    public void testParseQueryContent() {
        MessageDTO dto = initParam("查询我");
        Optional<String> s = messageHandler.handleMessage(dto);
        System.out.println(s.orElse("查询失败"));
    }

    @Test
    public void testNickHandle() {
        MessageDTO dto = initParam("查询绑定火毒：CoderMinnan");
        Optional<String> s = messageHandler.handleMessage(dto);
        System.out.println(s.orElse("处理异常"));
    }

    @Test
    public void testStirUp() {
        Optional<String> s = messageHandler.handleMessage(null);
        System.out.println(s.orElse("占卜失败"));
    }

    @Test
    public void testReckonSac() {
        MessageDTO dto = initParam("sac 1岛11级0 2岛10级100 3岛8级100");
        Optional<String> s = messageHandler.handleMessage(dto);
        System.out.println(s.orElse("异常"));
    }

    @Test
    public void testMenu() {
        MessageDTO dto = initParam("吃点什么 肯德基 麦当劳");
        Optional<String> s = messageHandler.handleMessage(dto);
        System.out.println(s.orElse("异常"));
    }

    @Test
    public void testCalculate() {
        MessageDTO dto = initParam("1+(-3*2)");
        Optional<String> s = messageHandler.handleMessage(dto);
        System.out.println(s.orElse("计算错误"));
    }

    @Test
    public void testQuestionHandle() {
//        MessageDTO param = initParam("添加问题测试答[CQ:image,file={E91E5B6B-170F-8FE4-E253-7D3B06F56C57}.image,subType=0,url=https://gchat.qpic.cn/gchatpic_new/931437070/931437070-2233222014-E91E5B6B170F8FE4E2537D3B06F56C57/0?vuin=1527761310&term=0&is_origin=2&is_ntv2=1]");
//        MessageDTO param = initParam("模糊查询问题测");
//        MessageDTO param = initParam("删除答案820");
        MessageDTO param = initParam("删除问题523");
        Optional<String> s = messageHandler.handleMessage(param);
        System.out.println(s.orElse("异常"));
    }

    @Test
    public void testMaintain() {
        MessageDTO param = initParam("");
        Optional<String> s = messageHandler.handleMessage(param);
        System.out.println(s.orElse("11"));
    }

    @Test
    public void testHoliday() throws JsonProcessingException {
        Optional<String> s = holidayMessageHandler.handleMessage(null);
        System.out.println(s.orElse("error"));
    }
}

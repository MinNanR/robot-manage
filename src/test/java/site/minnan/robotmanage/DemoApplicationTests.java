package site.minnan.robotmanage;

import cn.hutool.core.convert.NumberChineseFormatter;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.service.impl.CharacterSupportServiceImpl;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;

@SpringBootTest(classes = BotApplication.class)
class DemoApplicationTests {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    @Qualifier("stirUp")
    MessageHandler messageHandler;

    @Autowired
    CharacterSupportService characterSupportService;

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
    public void testStirUp(){
        Optional<String> s = messageHandler.handleMessage(null);
        System.out.println(s.orElse("占卜失败"));
    }

    public static void main(String[] args) {
        System.out.println(ReUtil.isMatch("^查询设置.*", "查询设置民难"));
    }

}

package site.minnan.robotmanage;

import cn.hutool.core.lang.Console;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;

@SpringBootTest(classes = BotApplication.class)
class DemoApplicationTests {

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    @Qualifier("query")
    MessageHandler messageHandler;

    @Test
    void contextLoads() {
    }

    private MessageDTO initParam(String message) {
        String jsonString = "{'raw_message': '#" + message + "', 'group_id': '667082876', 'sender': {'user_id': '1603'}, 'message_id': '46393'}";
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

}

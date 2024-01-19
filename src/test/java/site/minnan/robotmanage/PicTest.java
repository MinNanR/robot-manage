package site.minnan.robotmanage;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.glassfish.jaxb.runtime.v2.runtime.BinderImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

@SpringBootTest(classes = BotApplication.class)
public class PicTest {

    @Autowired
    @Qualifier("query")
    MessageHandler messageHandler;

    private MessageDTO initParam(String message) {
        String jsonString = "{'raw_message': '#" + message + "', 'group_id': '667082876', 'sender': {'user_id': '978312456'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        return MessageDTO.fromJson(jsonObject);
    }

    @Test
    public void testPic() {
        MessageDTO param = initParam("查询CoderMinnan");
        messageHandler.handleMessage(param);
    }
}

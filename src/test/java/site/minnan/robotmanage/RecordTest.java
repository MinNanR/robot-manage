package site.minnan.robotmanage;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.bot.CharacterData;
import site.minnan.robotmanage.service.CharacterSupportService;
import site.minnan.robotmanage.strategy.impl.QueryMessageHandler;

import java.util.Optional;

@SpringBootTest(classes = BotApplication.class, properties = "spring.profiles.active=dev")
public class RecordTest {

    @Autowired
    private CharacterSupportService characterSupportService;

    @Autowired
    QueryMessageHandler queryMessageHandler;

    private static MessageDTO initParam(String message) {
        String jsonString = "{'raw_message': '#" + message + "', 'group_id': '667082876', 'sender': {'user_id': '978312456','open_id': '123'}, 'message_id': '46393'}";
        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        return MessageDTO.fromJson(jsonObject);
    }

    @Test
    public void fetchCharacterExp() throws JsonProcessingException {
        Optional<CharacterData> characterDataOpt = characterSupportService.queryCharacterInfoLocal("CoderMinnan", "na");
        CharacterData characterData = characterDataOpt.get();
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(characterData));
    }

    @Test
    public void testQuery() {
       queryMessageHandler.doQuery(initParam("查询MinnanLum"));
    }
}

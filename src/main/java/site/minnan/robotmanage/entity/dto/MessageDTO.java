package site.minnan.robotmanage.entity.dto;

import cn.hutool.core.annotation.Alias;
import cn.hutool.json.JSONObject;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    @Alias("raw_message")
    private String rawMessage;

    @Alias("group_id")
    private String groupId;

    private User sender;

    @Alias("message_id")
    private String messageId;

    @Alias("open_id")
    private String openId;

    private Map<String, Object> payload;

    public static MessageDTO fromJson(JSONObject json) {
        MessageDTO dto = new MessageDTO();
        dto.messageId = json.getStr("message_id");
        dto.groupId = json.getStr("group_id");
        dto.rawMessage = json.getStr("raw_message").substring(1).strip();
        dto.sender = new User((String) json.getByPath("sender.user_id"), (String) json.getByPath("sender.open_id"));
        if (json.containsKey("payload")) {
            dto.payload = json.getJSONObject("payload");
        } else {
            dto.payload = new HashMap<>();
        }
        return dto;
    }

}

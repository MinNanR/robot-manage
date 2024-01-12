package site.minnan.robotmanage.entity.dto;

import cn.hutool.core.annotation.Alias;
import cn.hutool.json.JSONObject;
import lombok.*;

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

    public static MessageDTO fromJson(JSONObject json) {
        MessageDTO dto = new MessageDTO();
        dto.messageId = json.getStr("message_id");
        dto.groupId = json.getStr("group_id");
        dto.rawMessage = json.getStr("raw_message").substring(1);
        dto.sender = new User((String) json.getByPath("sender.user_id"));
        return dto;
    }

}

package site.minnan.robotmanage.entity.dto;

import cn.hutool.core.util.ReUtil;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 发送主动消息参数
 */
public class SendMessageDTO {

    @JsonProperty("message")
    private String message;

    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("message_id")
    private String messageId;

    public SendMessageDTO(MessageDTO dto, String content) {
        content = ReUtil.replaceAll(content, "minnan.site:\\d+/", "minnan.site/");
        content = ReUtil.replaceAll(content, ",subType=0", "");
        this.message = content;
        this.groupId = dto.getGroupId();
        this.messageId = dto.getMessageId();
        this.userId = dto.getSender().userId();
    }
}

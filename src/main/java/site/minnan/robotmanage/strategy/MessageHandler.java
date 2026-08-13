package site.minnan.robotmanage.strategy;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ReUtil;
import cn.hutool.json.JSONUtil;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.bot.MarkdownMessage;

import java.util.Optional;

public interface MessageHandler {

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    Optional<String> handleMessage(MessageDTO dto);

    default int getGroupMask() {
        return 0xFFFF_FFFF;
    }


    default String markdownWrapper(String msg) {
        msg = ReUtil.replaceAll(msg, "https", "http");
        msg = ReUtil.replaceAll(msg, "minnan.site:\\d+/", "minnan.site/");
        msg = ReUtil.replaceAll(msg, ",subType=0", "");

        MarkdownMessage markdownMessage = MarkdownMessage.ofContent(msg);
        String markdownJson = JSONUtil.toJsonStr(markdownMessage);
        String encoded = Base64.encode(markdownJson);
        return "[CQ:markdown,data=base64://%s]".formatted(encoded);
    }
}

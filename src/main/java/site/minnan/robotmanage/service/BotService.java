package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.dto.SendMessageDTO;
import site.minnan.robotmanage.entity.vo.bot.BotReply;

public interface BotService {

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    BotReply handleMessage(MessageDTO dto);

    /**
     * 发送异步回复消息
     *
     * @param dto
     */
    void sendAsyncMessage(SendMessageDTO dto);
}

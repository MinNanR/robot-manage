package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.vo.BotReply;

public interface BotService {

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    BotReply handleMessage(MessageDTO dto);

}

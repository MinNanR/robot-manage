package site.minnan.robotmanage.strategy;

import site.minnan.robotmanage.entity.dto.MessageDTO;

/**
 * 消息处理支持服务
 *
 * @author minnan on 2024/01/12
 */
public interface MessageHandlerSupportService {

    /**
     * 判定消息处理器
     *
     * @param dto@return
     */
    MessageHandler judgeMessageHandler(MessageDTO dto);
}

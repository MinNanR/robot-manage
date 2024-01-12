package site.minnan.robotmanage.strategy;

import site.minnan.robotmanage.entity.dto.MessageDTO;

import java.util.Optional;

public interface MessageHandler {

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    Optional<String> handleMessage(MessageDTO dto);

}

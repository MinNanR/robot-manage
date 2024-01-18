package site.minnan.robotmanage.strategy.impl;

import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Optional;

/**
 * 维护消息处理
 *
 * @author Minnan on 2024/01/18
 */
@Service("maintain")
public class MaintainMessageHandler implements MessageHandler {


    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        return Optional.of("功能完善中");
    }
}

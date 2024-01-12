package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Optional;

/**
 * roll点消息处理
 *
 * @author Minnan on 2024/01/12
 */
@Component("roll")
public class RollMessageHandler implements MessageHandler {

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String rawMessage = dto.getRawMessage();
        int randomResult = RandomUtil.randomInt(0, 101);
        if ("roll线".equalsIgnoreCase(rawMessage)) {
            randomResult = randomResult % 40 + 1;
        }
        return Optional.of(String.valueOf(randomResult));
    }
}

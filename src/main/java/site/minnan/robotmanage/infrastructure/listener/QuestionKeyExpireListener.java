package site.minnan.robotmanage.infrastructure.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.entity.dto.SendMessageDTO;
import site.minnan.robotmanage.service.BotService;
import site.minnan.robotmanage.strategy.impl.DefaultMessageHandler;
import site.minnan.robotmanage.strategy.impl.QueryMessageHandler;

@Component
@Slf4j
public class QuestionKeyExpireListener extends KeyExpirationEventMessageListener {

    private DefaultMessageHandler defaultMessageHandler;

    public QuestionKeyExpireListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Autowired
    public void setDefaultMessageHandler(DefaultMessageHandler defaultMessageHandler) {
        this.defaultMessageHandler = defaultMessageHandler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        if (!key.startsWith("question:")) {
            return;
        }
        log.info("词条回复数据已过期，key={}", key);
        defaultMessageHandler.removeContainer(key.substring("question:".length()));
    }
}
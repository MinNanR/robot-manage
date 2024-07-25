package site.minnan.robotmanage.infrastructure.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.infrastructure.utils.BotSessionUtil;
import site.minnan.robotmanage.strategy.impl.DefaultMessageHandler;

@Component
@Slf4j
public class SessionKeyExpireListener extends KeyExpirationEventMessageListener {

    private BotSessionUtil botSessionUtil;

    public SessionKeyExpireListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Autowired
    public void setBotSessionUtil(BotSessionUtil botSessionUtil) {
        this.botSessionUtil = botSessionUtil;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        if (!key.startsWith("session:")) {
            return;
        }
        log.info("会话过期，key={}", key);
        key = key.replace("session:", "");
        String[] keySplit = key.split(":");
        String groupId = keySplit[0];
        String userId= keySplit[1];
        botSessionUtil.expireSession(groupId, userId);
    }
}
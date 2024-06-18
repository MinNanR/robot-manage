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
import site.minnan.robotmanage.strategy.impl.QueryMessageHandler;

@Component
@Slf4j
public class QueryKeyExpireListener extends KeyExpirationEventMessageListener {

    private BotService botService;

    public QueryKeyExpireListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Autowired
    public void setBotService(BotService botService) {
        this.botService = botService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = new String(message.getBody());
        if (!key.startsWith("query:")) {
            return;
        }
        String userId = key.split(":")[1];
        log.info("查询超时，用户:{}", userId);
        if (QueryMessageHandler.userQueryTaskMap.containsKey(userId)) {
            MessageDTO messageDTO = QueryMessageHandler.userQueryTaskMap.get(userId);
            SendMessageDTO dto = new SendMessageDTO(messageDTO, "查询超时");
            botService.sendAsyncMessage(dto);
            QueryMessageHandler.userQueryTaskMap.remove(userId);
        }
    }
}

package site.minnan.robotmanage.infrastructure.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Slf4j
public class BotSessionUtil {

    /**
     * 会话消息处理器代理类
     */
    private static class SessionMessageHandlerProxy implements MessageHandler {

        @Override
        public Optional<String> handleMessage(MessageDTO dto) {
            String message = dto.getRawMessage();
            if ("-1".equals(message)) {
                GroupAndUserPair pair = new GroupAndUserPair(dto.getGroupId(), dto.getSender().userId());
                sessionEnder.accept(pair);
                return Optional.of("");
            }
            return this.handler.handleMessage(dto);
        }

        private final MessageHandler handler;

        private final Consumer<GroupAndUserPair> sessionEnder;

        private SessionMessageHandlerProxy(MessageHandler handler, Consumer<GroupAndUserPair> sessionEnder) {
            this.handler = handler;
            this.sessionEnder = sessionEnder;
        }
    }

    private record GroupAndUserPair(String groupId, String userId) {
    }

    private static final ConcurrentHashMap<GroupAndUserPair, MessageHandler> botSessionMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 新建一个对话记录
     *
     * @param userId  qq号
     * @param groupId 群号
     * @param handler 处理的方法
     */
    public void startSession(String groupId, String userId, MessageHandler handler) {
        startSession(groupId, userId, handler, Duration.ofMinutes(3));
    }

    /**
     * 新建一个对话记录
     *
     * @param userId  qq号
     * @param groupId 群号
     * @param handler 处理的方法
     */
    public void startSession(String groupId, String userId, MessageHandler handler, Duration duration) {
        String redisKey = "session:%s:%s".formatted(groupId, userId);
        GroupAndUserPair pair = new GroupAndUserPair(groupId, userId);
        SessionMessageHandlerProxy handlerProxy = new SessionMessageHandlerProxy(handler, p -> endSession(p.groupId, p.userId));
        botSessionMap.put(pair, handlerProxy);
        redisUtil.valueSet(redisKey, "1", duration);
        log.info("开启对话，群号{}，用户{}", groupId, userId);
    }

    /**
     * 对话超时
     *
     * @param groupId
     * @param userId
     */
    public void endSession(String groupId, String userId) {
        GroupAndUserPair pair = new GroupAndUserPair(groupId, userId);
        botSessionMap.remove(pair);
        log.info("对话结束，群号{}，用户{}", groupId, userId);
    }

    /**
     * 对话超时
     *
     * @param groupId
     * @param userId
     */
    public void expireSession(String groupId, String userId) {
        log.info("对话过期，群号{}，用户{}", groupId, userId);
        endSession(groupId, userId);
    }

    /**
     * 更换会话消息处理器
     *
     * @param groupId 群号
     * @param userId  QQ号
     * @param handler 新的处理器
     */
    public void updateSessionMessageHandler(String groupId, String userId, MessageHandler handler) {
        GroupAndUserPair pair = new GroupAndUserPair(groupId, userId);
        SessionMessageHandlerProxy handlerProxy = new SessionMessageHandlerProxy(handler, p -> endSession(p.groupId, p.userId));
        botSessionMap.put(pair, handlerProxy);
    }

    /**
     * 获取对话
     *
     * @param groupId
     * @param userId
     * @return
     */
    public Optional<MessageHandler> getSession(String groupId, String userId) {
        GroupAndUserPair pair = new GroupAndUserPair(groupId, userId);
        if (botSessionMap.containsKey(pair)) {
            MessageHandler messageHandler = botSessionMap.get(pair);
            String redisKey = "session:%s:%s".formatted(groupId, userId);
            redisUtil.setExpire(redisKey, 3L, TimeUnit.MINUTES);
            return Optional.of(messageHandler);
        } else {
            return Optional.empty();
        }
    }


}

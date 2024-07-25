package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.ReUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.Auth;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.dao.AuthRepository;
import site.minnan.robotmanage.entity.dao.StrategyRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.BotSessionUtil;
import site.minnan.robotmanage.service.StatisticsService;
import site.minnan.robotmanage.strategy.MessageHandler;
import site.minnan.robotmanage.strategy.MessageHandlerSupportService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 消息处理器判定服务
 *
 * @author Minnan on 2024/01/12
 */
@Service
@Slf4j
public class MessageHandlerSupportServiceImpl implements MessageHandlerSupportService, ApplicationContextAware {

    private StrategyRepository strategyRepository;

    private AuthRepository authRepository;

    private StatisticsService statisticsService;

    private BotSessionUtil botSessionUtil;

    private ApplicationContext applicationContext;

    private static final String DEFAULT_HANDLER_NAME = "default";

    public MessageHandlerSupportServiceImpl(StrategyRepository strategyRepository, AuthRepository authRepository, StatisticsService statisticsService, BotSessionUtil botSessionUtil) {
        this.strategyRepository = strategyRepository;
        this.authRepository = authRepository;
        this.statisticsService = statisticsService;
        this.botSessionUtil = botSessionUtil;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 判定消息处理器
     *
     * @param dto@return
     */
    @Override
    public MessageHandler judgeMessageHandler(MessageDTO dto) {
        List<HandlerStrategy> strategyList = strategyRepository.getAllByEnabledIsOrderByOrdinal(1);
        String rawMessage = dto.getRawMessage().toLowerCase();
        String messageId = dto.getMessageId();
        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();
        Auth authObj = authRepository.findByUserIdAndGroupId(userId, groupId);
        int auth = authObj == null ? 0 : authObj.getAuthNumber();
        //用户被禁止使用
        if (((auth >> 6 & 1) ^ 1) == 0) {
            return e -> Optional.of("当前用户无使用权限");
        }

        //当对话存在时不从处理策略中查找处理器
        Optional<MessageHandler> session = botSessionUtil.getSession(dto.getGroupId(), dto.getSender().userId());
        if (session.isPresent()) {
            return session.get();
        }

        for (HandlerStrategy strategy : strategyList) {
            Integer expressionType = strategy.getExpressionType();
            String expression = strategy.getExpression();
            String componentName = strategy.getComponentName();
            Integer authMask = strategy.getAuthMask();

            if (expressionType == 1 && Objects.equals(expression, rawMessage)) {
                if ((authMask != 0) && (auth & authMask) == 0) {
                    continue;
                }
                log.info("消息[{}]全匹配命中处理策略[{}]", messageId, strategy.getStrategyName());
                statisticsService.refer(strategy);
                return applicationContext.getBean(componentName, MessageHandler.class);
            } else if (expressionType == 2 && ReUtil.isMatch(expression, rawMessage)) {
                if ((authMask != 0) && (auth & authMask) == 0) {
                    continue;
                }
                statisticsService.refer(strategy);
                log.info("消息[{}]正则命中处理策略[{}]", messageId, strategy.getStrategyName());
                return applicationContext.getBean(componentName, MessageHandler.class);
            }
        }
        log.info("消息[{}]未命中处理策略，将使用默认处理策略", messageId);
        return applicationContext.getBean(DEFAULT_HANDLER_NAME, MessageHandler.class);
    }


//    @PostConstruct
//    public void initStrategy() throws IOException, ClassNotFoundException {
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        Resource[] resources = resolver.getResources("classpath*:site/minnan/robotmanage/strategy/impl/*.class");
//        for (Resource resource : resources) {
//            String filename = resource.getFilename();
//            String className = filename.split("\\.")[0];
//            Class<?> aClass = Class.forName("site.minnan.robotmanage.strategy.impl." + className);
//            if (MessageHandler.class.isAssignableFrom(aClass)) {
//                Component annotation = aClass.getAnnotation(Component.class);
//            }
//
//        }
//    }
}

package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.dao.StrategyRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;
import site.minnan.robotmanage.strategy.MessageHandlerSupportService;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * 消息处理器判定服务
 *
 * @author Minnan on 2024/01/12
 */
@Service
@Slf4j
public class MessageHandlerSupportServiceImpl implements MessageHandlerSupportService, ApplicationContextAware {

    private StrategyRepository strategyRepository;

    private ApplicationContext applicationContext;

    private static final String DEFAULT_HANDLER_NAME = "default";

    public MessageHandlerSupportServiceImpl(StrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
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

        for (HandlerStrategy strategy : strategyList) {
            Integer expressionType = strategy.getExpressionType();
            String expression = strategy.getExpression();
            String componentName = strategy.getComponentName();
            if (expressionType == 1 && Objects.equals(expression, rawMessage)) {
                log.info("消息[{}]全匹配命中处理策略[{}]", messageId, strategy.getStrategyName());
                return applicationContext.getBean(componentName, MessageHandler.class);
            } else if (expressionType == 2 && ReUtil.isMatch(expression, rawMessage)) {
                log.info("消息[{}]正则命中处理策略[{}]", messageId, strategy.getStrategyName());
                return applicationContext.getBean(componentName, MessageHandler.class);
            }
        }
        log.info("消息[{}]未命中处理策略，将使用默认处理策略" , messageId);
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

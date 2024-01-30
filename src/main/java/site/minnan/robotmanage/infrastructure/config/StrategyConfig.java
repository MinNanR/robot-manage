package site.minnan.robotmanage.infrastructure.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class StrategyConfig {

    @Bean("strategyComponent")
    public List<String> strategyComponentList() throws IOException, ClassNotFoundException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:site/minnan/robotmanage/strategy/impl/*.class");
        List<String> strategyComponentList = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            String className = filename.split("\\.")[0];
            Class<?> aClass = Class.forName("site.minnan.robotmanage.strategy.impl." + className);
            if (MessageHandler.class.isAssignableFrom(aClass)) {
                Component annotation = aClass.getAnnotation(Component.class);
                if (annotation == null) {
                    continue;
                }
                String beanName = annotation.value();
                strategyComponentList.add(beanName);
            }
        }

        return strategyComponentList;
    }
}

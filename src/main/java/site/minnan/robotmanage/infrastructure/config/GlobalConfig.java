package site.minnan.robotmanage.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Roger Liu
 * @date 2024/04/15
 */
@Configuration
public class GlobalConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        return om;
    }
}

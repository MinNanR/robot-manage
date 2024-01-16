package site.minnan.robotmanage.infrastructure.cachekeygen;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import org.hibernate.annotations.Bag;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.minnan.robotmanage.entity.dto.MessageDTO;

/**
 * 缓存键生成键配置
 *
 * @author Minnan on 2024/01/16
 */
@Configuration
public class KeyGeneratorConfig {

    /**
     * 占卜键生成器
     *
     * @return
     */
    @Bean("divinateKeyGenerator")
    public KeyGenerator divinateKeyGenerator() {
        return ((target, method, params) -> {
            if (params.length == 0) {
                return SimpleKey.EMPTY;
            }
            String today = DateTime.now().toString("yyyyMMdd");
            Object param = params[0];
            if (param instanceof MessageDTO dto) {
                String userId = dto.getSender().userId();
                return StrUtil.format("{}:{}", today, userId);
            }
            return "";
        });
    }

    /**
     * 查询键生成器
     *
     * @return
     */
    @Bean("queryKeyGenerator")
    public KeyGenerator queryKeyGenerator() {
        return ((target, method, params) -> {
            if (params.length == 0) {
                return SimpleKey.EMPTY;
            }
            String today = DateTime.now().toString("yyyyMMdd");
            Object param = params[0];
            if (param instanceof String queryName) {
                return "%s:%s".formatted(today, queryName);
            }
            return "";
        });
    }

}

package site.minnan.robotmanage.infrastructure.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;

@Configuration
@Slf4j
public class AliyunOssConfig {


    @Autowired
    private RedisUtil redisUtil;

    @Bean
    public OSS oss(){
        String endpoint = (String) redisUtil.getValue("endpoint_");
        String accessKeyId = (String) redisUtil.getValue("accessKeyId_");
        String accessKeySecret = (String) redisUtil.getValue("accessKeySecret_");
        if (endpoint == null || accessKeyId == null || accessKeySecret == null) {
            log.warn("加载阿里云配置信息失败，请检查redis缓存");
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}

package site.minnan.robotmanage.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * 代理配制
 *
 * @author Minna on 2024/01/15
 */
@Configuration
public class ProxyConfig {

    @Value("${enableProxy:true}")
    private Boolean enableProxy;

    @Bean("proxy")
    public Proxy proxy() {
        if (!enableProxy) {
            return Proxy.NO_PROXY;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 2334));
        } else {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 20171));
        }
    }

}

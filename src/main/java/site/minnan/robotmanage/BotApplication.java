package site.minnan.robotmanage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import site.minnan.robotmanage.strategy.impl.QueryMessageHandler;
import sun.misc.Signal;

@SpringBootApplication
@EnableScheduling
@Slf4j
//@EnableCaching
public class BotApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(BotApplication.class, args);
		Signal.handle(new Signal("TERM"), sig -> {
			log.info("receive signal term");
			QueryMessageHandler queryMessageHandler = context.getBean("query", QueryMessageHandler.class);
			queryMessageHandler.beforeApplicationShutdown();
			context.close();
			System.exit(0);
		});
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
		PropertySourcesPlaceholderConfigurer c = new PropertySourcesPlaceholderConfigurer();
		c.setIgnoreUnresolvablePlaceholders(true);
		return c;
	}
}

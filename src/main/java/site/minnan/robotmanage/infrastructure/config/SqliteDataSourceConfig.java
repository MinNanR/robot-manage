package site.minnan.robotmanage.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "site.minnan.robotmanage.entity.dao",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class SqliteDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.sqlite")
    public DataSourceProperties sqliteDataSourceProperties() {
        return new DataSourceProperties();
    }


    @Bean(name = "dataSource")
    @Primary
    public DataSource sqliteDataSource(
            @Qualifier("sqliteDataSourceProperties") DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean sqliteEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource,
            JpaProperties jpaProperties
    ) {
        Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties());
        properties.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        return builder
                .dataSource(dataSource)
                .packages("site.minnan.robotmanage.entity.aggregate")
                .persistenceUnit("sqlite")
                .properties(properties)
                .build();
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager sqliteTransactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

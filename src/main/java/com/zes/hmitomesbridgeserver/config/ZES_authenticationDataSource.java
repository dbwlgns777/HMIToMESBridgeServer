package com.zes.hmitomesbridgeserver.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
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

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "entityManagerFactory",
        basePackages = {"com.zes.hmitomesbridgeserver.structure"},
        transactionManagerRef = "transactionManager"
)
public class ZES_authenticationDataSource
{
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource()
    {
        try
        {
            HikariConfig config = new HikariConfig("db_config/mysql.config");
            return new HikariDataSource(config);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to initialize DataSource from db_config/mysql.config", e);
        }
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource)
    {
        return builder
                .dataSource(dataSource)
                .packages("com.zes.hmitomesbridgeserver.structure")
                .persistenceUnit("authentication")
                .build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") jakarta.persistence.EntityManagerFactory entityManagerFactory)
    {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

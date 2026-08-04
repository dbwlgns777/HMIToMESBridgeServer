package com.zes.hmitomesbridgeserver.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class ZES_authenticationDataSource
{
    private static final String MYSQL_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource()
    {
        try
        {
            // Load and register Connector/J before Hikari asks DriverManager for the JDBC URL driver.
            Class.forName(MYSQL_DRIVER_CLASS_NAME);
            HikariConfig config = new HikariConfig("db_config/mysql.config");
            config.setDriverClassName(MYSQL_DRIVER_CLASS_NAME);
            return new HikariDataSource(config);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to initialize DataSource from db_config/mysql.config", e);
        }
    }

}

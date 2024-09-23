package com.example.dbswitchingdemo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class H2DataSourceConfig {

    @Bean("kostil")
    public DataSource h2DataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:testdb"); // Встроенная H2 база данных в памяти
        hikariConfig.setUsername("sa"); // Стандартный пользователь H2
        hikariConfig.setPassword(""); // Пароль по умолчанию пустой
        hikariConfig.setDriverClassName("org.h2.Driver");
        hikariConfig.setMaximumPoolSize(10); // Максимум 10 подключений в пуле

        return new HikariDataSource(hikariConfig);
    }
}

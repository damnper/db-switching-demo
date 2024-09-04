package com.example.dbswitchingdemo.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурационный класс для настройки источников данных.
 * Определяет начальный источник данных с использованием пула соединений Hikari и маршрутизатор для управления несколькими источниками данных.
 */
@Getter
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    /**
     * Создает бин для HikariDataSource, который будет использоваться для всех баз данных.
     * @return настроенный HikariDataSource
     */
    @Bean
    @Primary
    public HikariDataSource hikariDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dataSourceProperties.getUrl());
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        dataSource.setMaximumPoolSize(10);
        return dataSource;
    }

    /**
     * Создает HikariDataSource для указанного URL базы данных.
     * @param url URL подключения к базе данных
     * @return настроенный HikariDataSource
     */
    public HikariDataSource createHikariDataSource(String url) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        dataSource.setMaximumPoolSize(10);
        return dataSource;
    }

    /**
     * Создает и настраивает MultiRoutingDataSource, который управляет маршрутизацией между различными источниками данных.
     * Инициализирует его с начальным источником данных.
     *
     * @param initialDataSource начальный источник данных
     * @return настроенный MultiRoutingDataSource
     */
    @Bean
    @Primary
    public MultiRoutingDataSource multiRoutingDataSource(DataSource initialDataSource) {
        MultiRoutingDataSource routingDataSource = new MultiRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("replica_localhost_5433", initialDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(initialDataSource);

        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
}

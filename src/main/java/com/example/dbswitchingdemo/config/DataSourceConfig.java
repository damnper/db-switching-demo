package com.example.dbswitchingdemo.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурационный класс для настройки источников данных.
 * Определяет начальный источник данных и маршрутизатор для управления множественными источниками данных.
 */
@Getter
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    /**
     * Создает и настраивает начальный источник данных (DataSource).
     *
     * @return настроенный DataSource
     */
    @Bean(name = "initialDataSource")
    public DataSource initialDataSource() {
        return DataSourceBuilder.create()
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties.getUsername())
                .password(dataSourceProperties.getPassword())
                .driverClassName(dataSourceProperties.getDriverClassName())
                .build();
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
        targetDataSources.put("dbname_localhost_5433", initialDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(initialDataSource);

        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
}

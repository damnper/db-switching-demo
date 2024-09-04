package com.example.dbswitchingdemo.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Конфигурационный класс для настройки и управления источниками данных в приложении.</p>
 * <p>Этот класс определяет и настраивает пул соединений HikariCP для источников данных,
 * а также предоставляет механизм для маршрутизации запросов к различным базам данных с помощью
 * {@link MultiRoutingDataSource}.</p>
 */
@Getter
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final DataSourceProperties dataSourceProperties;

    /**
     * <p>Создает и настраивает экземпляр {@link HikariDataSource} для подключения к базе данных по указанному URL.</p>
     * <p>Источник данных использует параметры, указанные в {@link DataSourceProperties} для настройки аутентификации,
     * а также драйвер, специфичный для базы данных.</p>
     *
     * @param url URL подключения к базе данных.
     * @return настроенный экземпляр {@link HikariDataSource}.
     */
    public HikariDataSource createHikariDataSource(String url) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(dataSourceProperties.getUsername());
        dataSource.setPassword(dataSourceProperties.getPassword());
        dataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
        return dataSource;
    }

    /**
     * <p>Создает и настраивает {@link MultiRoutingDataSource}, который управляет маршрутизацией запросов
     * между несколькими источниками данных.</p>
     * <p>Метод инициализирует маршрутизатор с "фиктивным" источником данных (H2 in-memory база),
     * который используется по умолчанию до добавления реальных источников данных.</p>
     *
     * @return настроенный {@link MultiRoutingDataSource}, готовый к использованию в приложении.
     */
    @Bean
    @Primary
    public MultiRoutingDataSource multiRoutingDataSource() {
        MultiRoutingDataSource routingDataSource = new MultiRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();

        HikariDataSource fakeDataSource = getHikariDataSourceForFakeDB();

        targetDataSources.put("fakeDataSource", fakeDataSource);

        routingDataSource.setDefaultTargetDataSource(fakeDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    /**
     * <p>Создает HikariDataSource для "фиктивного" источника данных, который представляет собой H2 in-memory базу.</p>
     * <p>Этот источник данных используется по умолчанию до добавления реальных источников данных в систему.</p>
     *
     * @return настроенный {@link HikariDataSource} для H2 in-memory базы данных.
     */
    private HikariDataSource getHikariDataSourceForFakeDB() {
        HikariDataSource fakeDataSource  = new HikariDataSource();
        fakeDataSource.setJdbcUrl("jdbc:h2:mem:fakeDB");
        fakeDataSource.setUsername("fake");
        fakeDataSource.setPassword("fake");
        return fakeDataSource;
    }
}

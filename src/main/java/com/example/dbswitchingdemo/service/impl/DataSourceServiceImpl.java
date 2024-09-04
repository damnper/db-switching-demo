package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.config.DataSourceConfig;
import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.config.DataSourceProperties;
import com.example.dbswitchingdemo.config.MultiRoutingDataSource;
import com.example.dbswitchingdemo.dto.response.CommonDataResponse;
import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.entity.DbSwitchLog;
import com.example.dbswitchingdemo.exception.DataSourceExistException;
import com.example.dbswitchingdemo.repo.DbSwitchLogRepository;
import com.example.dbswitchingdemo.service.DataSourceService;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p> Реализация сервиса для управления источниками данных. </p>
 * <p> Этот сервис обеспечивает создание, переключение и закрытие источников данных.
 * Также выполняет проверку соединения с базой данных перед созданием нового источника. </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {

    private static final String FAKE_DATASOURCE_NAME = "fakeDataSource";
    public static final String JDBC_POSTGRESQL = "jdbc:postgresql://%s:%d/%s_%s_%d";

    private final DataSourceProperties dataSourceProperties;
    private final DataSourceConfig dataSourceConfig;
    private final MultiRoutingDataSource routingDataSource;
    private final DbSwitchLogRepository dbSwitchLogRepository;

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        dataSources.put(FAKE_DATASOURCE_NAME, new HikariDataSource());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse createDataSource(String host, Integer port) {
        String uniqueDbName = createUniqueDbName(host, port);

        if (dataSources.containsKey(uniqueDbName)) {
            throw new DataSourceExistException("DataSource '" + uniqueDbName + "' already exists!");
        }

        try {
            String testUrl = buildJdbcUrl(host, port);
            if (!testDatabaseConnection(testUrl)) {
                return CommonResponse.builder().status(HttpStatus.BAD_REQUEST.name())
                        .message("Failed to connect to the database at '" + host + ":" + port + "'. Please check the database configuration.")
                        .build();
            }

            HikariDataSource newDataSource = dataSourceConfig.createHikariDataSource(testUrl);
            addDataSource(uniqueDbName, newDataSource);

            return CommonResponse.builder().status(HttpStatus.OK.name())
                    .message("DataSource '" + uniqueDbName + "' created successfully!")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create DataSource for host '{}' and port '{}'", host, port, e);
            throw new RuntimeException("Failed to create DataSource '%s'".formatted(uniqueDbName));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse switchDataSource(String name) {
        if (dataSources.containsKey(name)) {
            DataSourceContextHolder.setDataSource(name);
            logDataSourceSwitch();

            return CommonDataResponse.builder()
                    .status(HttpStatus.OK.name())
                    .data(true)
                    .message("Switched to DataSource '" + name + "' successfully.")
                    .build();
        }
        return CommonDataResponse.builder()
                .status(HttpStatus.NOT_FOUND.name())
                .data(false)
                .message("DataSource '" + name + "' not found.")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse closeDataSource(String name) {
        DataSource dataSource = dataSources.remove(name);
        if (dataSource != null) {
            return closeExistingDataSource(name, dataSource);
        }
        return CommonDataResponse.builder()
                .status(HttpStatus.NOT_FOUND.name())
                .data(false)
                .message("DataSource '" + name + "' not found.")
                .build();
    }

    /**
     * Добавляет источник данных в карту источников и настраивает маршрутизацию.
     *
     * @param name       уникальное имя источника данных
     * @param dataSource источник данных
     */
    private void addDataSource(String name, DataSource dataSource) {
        if (dataSources.containsKey(FAKE_DATASOURCE_NAME)) {
            dataSources.remove(FAKE_DATASOURCE_NAME);
            routingDataSource.removeDataSource(FAKE_DATASOURCE_NAME);
            log.info("Removed fake data source after adding the first real one.");
        }

        dataSources.put(name, dataSource);
        routingDataSource.addDataSource(name, dataSource);
    }

    /**
     * Создаёт уникальное имя для источника данных на основе хоста и порта.
     *
     * @param host хост базы данных
     * @param port порт базы данных
     * @return уникальное имя для источника данных
     */
    private String createUniqueDbName(String host, int port) {
        String databaseName = dataSourceProperties.getName();
        return String.format("%s_%s_%d", databaseName, host, port);
    }

    /**
     * Формирует URL JDBC для подключения к базе данных.
     *
     * @param host хост базы данных
     * @param port порт базы данных
     * @return сформированный URL JDBC
     */
    private String buildJdbcUrl(String host, int port) {
        String databaseName = dataSourceProperties.getName();
        return String.format(JDBC_POSTGRESQL, host, port, databaseName, host, port);
    }

    /**
     * Проверяет соединение с базой данных по переданному URL.
     *
     * @param url URL JDBC для подключения к базе данных
     * @return true, если соединение успешно, иначе false
     */
    private boolean testDatabaseConnection(String url) {
        try (Connection ignored = DriverManager.getConnection(
                        url,
                        dataSourceProperties.getUsername(),
                        dataSourceProperties.getPassword())) {
            log.info("Connection is successfully established for '{}'", url);
            return true;
        } catch (SQLException e) {
            log.error("Failed to connect to database at '{}'", url);
            return false;
        }
    }

    /**
     * Логирует переключение источника данных, сохраняет время переключения в базу данных.
     */
    private void logDataSourceSwitch() {
        DbSwitchLog log = new DbSwitchLog();
        log.setSwitchTime(LocalDateTime.now());
        dbSwitchLogRepository.save(log);
    }

    /**
     * Закрывает и удаляет указанный источник данных.
     *
     * @param name        имя источника данных
     * @param dataSource источник данных
     * @return объект {@link CommonResponse} с результатом операции
     */
    private CommonResponse closeExistingDataSource(String name, DataSource dataSource) {
        try {
            if (dataSource instanceof AutoCloseable) {
                ((AutoCloseable) dataSource).close();
            }
            return CommonDataResponse.builder()
                    .status(HttpStatus.OK.name())
                    .data(true)
                    .message("DataSource '" + name + "' closed successfully.")
                    .build();
        } catch (Exception e) {
            log.error("Failed to close DataSource '{}'", name, e);
            return CommonDataResponse.builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                    .data(false)
                    .message("Failed to close DataSource '" + name + "'.")
                    .build();
        }
    }
}

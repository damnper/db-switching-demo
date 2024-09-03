package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.config.DataSourceConfig;
import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.config.MultiRoutingDataSource;
import com.example.dbswitchingdemo.dto.response.CommonDataResponse;
import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.entity.DbSwitchLog;
import com.example.dbswitchingdemo.exception.DataSourceExistException;
import com.example.dbswitchingdemo.repo.DbSwitchLogRepository;
import com.example.dbswitchingdemo.service.DataSourceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {

    public static final String JDBC_POSTGRESQL = "jdbc:postgresql://%s:%d/%s_%s_%d";

    private final DataSourceConfig dataSourceConfig;
    private final MultiRoutingDataSource routingDataSource;
    private final DbSwitchLogRepository dbSwitchLogRepository;

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String defaultHost = "localhost";
        int defaultPort = 5433;

        String uniqueDbName = createUniqueDbName(defaultHost, defaultPort);
        String url = buildJdbcUrl(defaultHost, defaultPort);

        DataSource initialDataSource = createDataSourceInstance(url);
        addDataSource(uniqueDbName, initialDataSource);
    }

    @Override
    public CommonResponse createDataSource(String host, Integer port) {
        String uniqueDbName = createUniqueDbName(host, port);

        if (dataSources.containsKey(uniqueDbName)) {
            throw new DataSourceExistException("DataSource '" + uniqueDbName + "' already exists!");
        }

        try {
            String testUrl = buildJdbcUrl(host, port);
            if (!testDatabaseConnection(testUrl)) {
                return CommonResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.name())
                        .message("Failed to connect to the database at '" + host + ":" + port + "'. Please check the database configuration.")
                        .build();
            }

            DataSource newDataSource = createDataSourceInstance(testUrl);
            addDataSource(uniqueDbName, newDataSource);

            return CommonResponse.builder()
                    .status(HttpStatus.OK.name())
                    .message("DataSource '" + uniqueDbName + "' created successfully!")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create DataSource for host '{}' and port '{}'", host, port, e);
            throw new RuntimeException("Failed to create DataSource.");
        }
    }

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

    private void addDataSource(String name, DataSource dataSource) {
        dataSources.put(name, dataSource);
        routingDataSource.addDataSource(name, dataSource);
    }

    private String createUniqueDbName(String host, int port) {
        String databaseName = dataSourceConfig.getName();
        return String.format("%s_%s_%d", databaseName, host, port);
    }

    private String buildJdbcUrl(String host, int port) {
        String databaseName = dataSourceConfig.getName();
        return String.format(JDBC_POSTGRESQL, host, port, databaseName, host, port);
    }

    private DataSource createDataSourceInstance(String url) {
        return DataSourceBuilder.create()
                .url(url)
                .username(dataSourceConfig.getUsername())
                .password(dataSourceConfig.getPassword())
                .driverClassName(dataSourceConfig.getDriverClassName())
                .build();
    }

    private boolean testDatabaseConnection(String url) {
        try (Connection ignored = DriverManager.getConnection(url, dataSourceConfig.getUsername(), dataSourceConfig.getPassword())) {
            log.info("Connection is successfully established for '{}'", url);
            return true;
        } catch (SQLException e) {
            log.error("Failed to connect to database at '{}'", url);
            return false;
        }
    }

    private void logDataSourceSwitch() {
        DbSwitchLog log = new DbSwitchLog();
        log.setSwitchTime(LocalDateTime.now());
        dbSwitchLogRepository.save(log);
    }

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

package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.dto.LogEntryDTO;
import com.example.dbswitchingdemo.service.LibTest;
import io.tenet.dynamic_datasource.datasource.MultiRouting;
import io.tenet.dynamic_datasource.model.PostgresConnectionInfo;
import io.tenet.dynamic_datasource.service.impl.DynamicDatabaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class LibTestImpl implements LibTest {

    private final DynamicDatabaseServiceImpl dynamicDatabaseService;
    private final MultiRouting multiRoutingDataSource1;
    private final MultiRouting multiRoutingDataSource2;

    private final JdbcTemplate jdbcTemplate1; // Для работы с первой базой данных
    private final JdbcTemplate jdbcTemplate2; // Для работы с второй базой данных

    public LibTestImpl(DynamicDatabaseServiceImpl dynamicDatabaseService,
                       @Qualifier("db1MultiRouting") MultiRouting multiRoutingDataSource1,
                       @Qualifier("db2MultiRouting") MultiRouting multiRoutingDataSource2,
                       @Qualifier("jdbcTemplate1") JdbcTemplate jdbcTemplate1,
                       @Qualifier("jdbcTemplate2") JdbcTemplate jdbcTemplate2) {
        this.dynamicDatabaseService = dynamicDatabaseService;
        this.multiRoutingDataSource1 = multiRoutingDataSource1;
        this.multiRoutingDataSource2 = multiRoutingDataSource2;
        this.jdbcTemplate1 = jdbcTemplate1;
        this.jdbcTemplate2 = jdbcTemplate2;
    }

    /**
     * Планировщик, который каждую минуту вызывает методы close, refresh и change.
     */
    @Scheduled(fixedRate = 6000)
    public void performScheduledTask() {
        try {
            List<PostgresConnectionInfo> postgresConnectionInfos = createTestConnectionInfos();

            dynamicDatabaseService.close(postgresConnectionInfos, multiRoutingDataSource1);
            dynamicDatabaseService.close(postgresConnectionInfos, multiRoutingDataSource2);

            dynamicDatabaseService.refresh(postgresConnectionInfos, multiRoutingDataSource1);
            dynamicDatabaseService.refresh(postgresConnectionInfos, multiRoutingDataSource2);

            dynamicDatabaseService.change();

            logToDatabases();
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    /**
     * Создает тестовый список PostgresConnectionInfo, где лидеры и реплики могут меняться.
     *
     * @return список PostgresConnectionInfo
     */
    private List<PostgresConnectionInfo> createTestConnectionInfos() {
        List<PostgresConnectionInfo> list = new ArrayList<>();

        list.add(new PostgresConnectionInfo("localhost", 5433, "postgres_1", "replica"));
        list.add(new PostgresConnectionInfo("localhost", 5434, "postgres_2", "replica"));
        list.add(new PostgresConnectionInfo("localhost", 5435, "postgres_3", "replica"));

        Collections.shuffle(list, new Random());

        list.get(0).setRole("leader");

        return list;
    }

    /**
     * Метод для логирования в обе базы данных.
     */
    private void logToDatabases() {
        LogEntryDTO logEntry = LogEntryDTO.builder()
                .logTime(LocalDateTime.now())
                .build();

        String insertSQL = "INSERT INTO logs (log_time) VALUES (?)";
        jdbcTemplate1.update(insertSQL, logEntry.getLogTime());
        jdbcTemplate2.update(insertSQL, logEntry.getLogTime());

        log.info("Logged to both databases at {}", logEntry.getLogTime());
    }
}

package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.service.LibTest;
import io.tenet.dynamic_datasource.datasource.MultiRouting;
import io.tenet.dynamic_datasource.model.PostgresConnectionInfo;
import io.tenet.dynamic_datasource.service.impl.DynamicDatabaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class LibTestImpl implements LibTest {

    private final DynamicDatabaseServiceImpl dynamicDatabaseService;
    private final  MultiRouting multiRoutingDataSource1;
    private final MultiRouting multiRoutingDataSource2;

    public LibTestImpl(DynamicDatabaseServiceImpl dynamicDatabaseService,
                       @Qualifier("db1MultiRouting") MultiRouting multiRoutingDataSource1,
                       @Qualifier("db2MultiRouting") MultiRouting multiRoutingDataSource2) {
        this.dynamicDatabaseService = dynamicDatabaseService;
        this.multiRoutingDataSource1 = multiRoutingDataSource1;
        this.multiRoutingDataSource2 = multiRoutingDataSource2;
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
}

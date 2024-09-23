package com.example.dbswitchingdemo.config;

import io.tenet.dynamic_datasource.config.DataSourceRoutingProperties;
import io.tenet.dynamic_datasource.context.DataSourceContextHolder;
import io.tenet.dynamic_datasource.datasource.factory.ActivePoolFactory;
import io.tenet.dynamic_datasource.model.DataSourceDTO;
import io.tenet.dynamic_datasource.service.impl.DynamicDatabaseServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DynamicDatabaseServiceConfig {

    @Bean
    public Map<String, Map<String, DataSourceDTO>> dsActivePools() {
        return ActivePoolFactory.createActivePoolsForDatabases("db1", "db2");
    }

    @Bean
    public DynamicDatabaseServiceImpl dynamicDatabaseService(
            Map<String, Map<String, DataSourceDTO>> dsActivePools,
            Map<String, DataSourceContextHolder> contextHolderMap,
            DataSourceRoutingProperties routingProp) {

        return DynamicDatabaseServiceImpl.builder()
                .dsActivePools(dsActivePools)
                .contextHolderMap(contextHolderMap)
                .leaderName(routingProp.getLeaderName())
                .replicaName(routingProp.getReplicaName())
                .build();
    }
}

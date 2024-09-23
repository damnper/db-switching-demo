package com.example.dbswitchingdemo.config;

import io.tenet.dynamic_datasource.context.DataSourceContextHolder;
import io.tenet.dynamic_datasource.model.DataSourceDTO;
import io.tenet.dynamic_datasource.service.impl.DynamicDatabaseServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class DynamicDatabaseServiceConfig {

    @Bean
    public Map<String, Map<String, DataSourceDTO>> dsActivePools() {
        Map<String, Map<String, DataSourceDTO>> activePools = new ConcurrentHashMap<>();
        activePools.put("db1", new ConcurrentHashMap<>());
        activePools.put("db2", new ConcurrentHashMap<>());
        return activePools;
    }

    @Bean
    public DynamicDatabaseServiceImpl dynamicDatabaseService(
            Map<String, Map<String, DataSourceDTO>> dsActivePools,
            Map<String, DataSourceContextHolder> contextHolderMap) {
        return new DynamicDatabaseServiceImpl(dsActivePools, contextHolderMap);
    }
}

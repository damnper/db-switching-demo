package com.example.dbswitchingdemo.config;

import io.tenet.dynamic_datasource.context.DataSourceContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class DataSourceContextHolderConfig {

    @Bean("db1")
    public DataSourceContextHolder createDb1ContextHolder() {
        return new DataSourceContextHolder();
    }

    @Bean("db2")
    public DataSourceContextHolder createDb2ContextHolder() {
        return new DataSourceContextHolder();
    }

    @Bean("contextHolderMap")
    public Map<String, DataSourceContextHolder> contextHolderMap(
            @Qualifier("db1") DataSourceContextHolder db1ContextHolder,
            @Qualifier("db2") DataSourceContextHolder db2ContextHolder) {
        Map<String, DataSourceContextHolder> contextHolders = new ConcurrentHashMap<>();
        contextHolders.put("db1", db1ContextHolder);
        contextHolders.put("db2", db2ContextHolder);
        return contextHolders;
    }
}

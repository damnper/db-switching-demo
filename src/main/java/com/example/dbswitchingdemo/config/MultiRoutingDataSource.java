package com.example.dbswitchingdemo.config;

import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NonNullApi
@RequiredArgsConstructor
public class MultiRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();


    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSource();
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources.putAll(targetDataSources);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }

    public void addDataSource(String key, DataSource dataSource) {
        this.targetDataSources.put(key, dataSource);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }
}

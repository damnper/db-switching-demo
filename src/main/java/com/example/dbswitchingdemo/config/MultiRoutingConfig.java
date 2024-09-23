package com.example.dbswitchingdemo.config;

import io.tenet.dynamic_datasource.context.DataSourceContextHolder;
import io.tenet.dynamic_datasource.datasource.MultiRouting;
import io.tenet.dynamic_datasource.datasource.factory.MultiRoutingFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@RequiredArgsConstructor
@DependsOn({"db1", "db2"})
public class MultiRoutingConfig {

    private final MultiRoutingFactory multiRoutingFactory;

    @Bean("db1MultiRouting")
    public MultiRouting multiRoutingDataSource1(
            @Qualifier("db1") DataSourceContextHolder db1ContextHolder) {

        return multiRoutingFactory.create("db1", db1ContextHolder);
    }

    @Bean("db2MultiRouting")
    public MultiRouting multiRoutingDataSource2(
            @Qualifier("db2") DataSourceContextHolder db2ContextHolder) {

        return multiRoutingFactory.create("db2", db2ContextHolder);
    }
}

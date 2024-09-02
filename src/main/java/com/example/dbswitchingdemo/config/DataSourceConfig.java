package com.example.dbswitchingdemo.config;

import com.example.dbswitchingdemo.enums.DataSourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${spring.leader.datasource.url}")
    private String leaderUrl;

    @Value("${spring.leader.datasource.username}")
    private String leaderUsername;

    @Value("${spring.leader.datasource.password}")
    private String leaderPassword;

    @Value("${spring.leader.datasource.driver-class-name}")
    private String leaderDriverClassName;

    @Value("${spring.replica.datasource.url}")
    private String replicaUrl;

    @Value("${spring.replica.datasource.username}")
    private String replicaUsername;

    @Value("${spring.replica.datasource.password}")
    private String replicaPassword;

    @Value("${spring.replica.datasource.driver-class-name}")
    private String replicaDriverClassName;

    @Bean(name = "leaderDataSource")
    public DataSource leaderDataSource() {
        return DataSourceBuilder.create()
                .url(leaderUrl)
                .username(leaderUsername)
                .password(leaderPassword)
                .driverClassName(leaderDriverClassName)
                .build();
    }

    @Bean(name = "replicaDataSource")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
                .url(replicaUrl)
                .username(replicaUsername)
                .password(replicaPassword)
                .driverClassName(replicaDriverClassName)
                .build();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("leaderDataSource") DataSource leaderDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource) {

        MultiRoutingDataSource routingDataSource = new MultiRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.LEADER, leaderDataSource);
        targetDataSources.put(DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(leaderDataSource);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }


}

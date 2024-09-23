package com.example.dbswitchingdemo.config;

import com.zaxxer.hikari.HikariDataSource;
import io.tenet.dynamic_datasource.context.DataSourceContextHolder;
import io.tenet.dynamic_datasource.datasource.MultiRouting;
import io.tenet.dynamic_datasource.datasource.factory.MultiRoutingFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static io.tenet.dynamic_datasource.datasource.factory.DataSourceFactory.createDataSource;

@Configuration
public class MultiRoutingConfig {

    @Bean("db1MultiRouting")
    public MultiRouting multiRoutingDataSource1(
            @Qualifier("db1") DataSourceContextHolder db1ContextHolder) {

        HikariDataSource dataSource1 = createDataSource(
                "jdbc:postgresql://localhost:5433/db1",
                "user",
                "pass",
                "org.postgresql.Driver"
        );
        HikariDataSource dataSource2 = createDataSource(
                "jdbc:postgresql://localhost:5434/db1",
                "user",
                "pass",
                "org.postgresql.Driver"
        );
        HikariDataSource dataSource3 = createDataSource(
                "jdbc:postgresql://localhost:5435/db1",
                "user",
                "pass",
                "org.postgresql.Driver"
        );

        Map<String, DataSource> dataSources = new HashMap<>();
        dataSources.put("db1_1", dataSource1);
        dataSources.put("db1_2", dataSource2);
        dataSources.put("db1_3", dataSource3);

        return MultiRoutingFactory.create(
                dataSources,
                "db1",
                "user",
                "pass",
                "org.postgresql.Driver",
                db1ContextHolder);
    }

    @Bean("db2MultiRouting")
    public MultiRouting multiRoutingDataSource2(
            @Qualifier("db2") DataSourceContextHolder db2ContextHolder
    ) {
        HikariDataSource dataSource1 = createDataSource(
                "jdbc:postgresql://localhost:5433/db2",
                "user",
                "pass",
                "org.postgresql.Driver"
        );
        HikariDataSource dataSource2 = createDataSource(
                "jdbc:postgresql://localhost:5434/db2",
                "user",
                "pass",
                "org.postgresql.Driver"
        );
        HikariDataSource dataSource3 = createDataSource(
                "jdbc:postgresql://localhost:5435/db2",
                "user",
                "pass",
                "org.postgresql.Driver"
        );

        Map<String, DataSource> dataSources = new HashMap<>();
        dataSources.put("db2_1", dataSource1);
        dataSources.put("db2_2", dataSource2);
        dataSources.put("db2_3", dataSource3);

        return MultiRoutingFactory.create(
                dataSources,
                "db2",
                "user",
                "pass",
                "org.postgresql.Driver",
                db2ContextHolder
        );
    }
}

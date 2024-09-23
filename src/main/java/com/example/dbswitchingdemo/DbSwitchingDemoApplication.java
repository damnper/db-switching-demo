package com.example.dbswitchingdemo;

import io.tenet.dynamic_datasource.config.DataSourceProperties;
import io.tenet.dynamic_datasource.config.DataSourceRoutingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        DataSourceRoutingProperties.class,
        DataSourceProperties.class
})
@ComponentScan(basePackages = {"com.example.dbswitchingdemo", "io.tenet.dynamic_datasource"})
public class DbSwitchingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbSwitchingDemoApplication.class, args);
    }

}

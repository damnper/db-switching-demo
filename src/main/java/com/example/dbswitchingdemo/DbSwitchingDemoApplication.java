package com.example.dbswitchingdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DbSwitchingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbSwitchingDemoApplication.class, args);
    }

}

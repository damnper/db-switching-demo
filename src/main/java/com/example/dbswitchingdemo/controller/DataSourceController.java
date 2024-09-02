package com.example.dbswitchingdemo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@RestController
@RequestMapping("/api/v1")
public class DataSourceController {

    @Value("${database.username}")
    private String username;

    @Value("${database.password}")
    private String password;

    @Value("${database.name}")
    private String databaseName;

    @PostMapping("/create-data-source")
    public ResponseEntity<String> createDataSource(@RequestParam String host, @RequestParam Integer port) {
        try {
            String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);

            DataSource newDataSource = DataSourceBuilder.create()
                    .url(url)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();

            // Здесь добавляем новый DataSource в контекст приложения
            // Либо сохраняем его в пуле источников данных для последующего использования

            return ResponseEntity.ok("DataSource created successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create DataSource.");
        }
    }
}

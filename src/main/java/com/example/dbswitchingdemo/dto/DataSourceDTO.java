package com.example.dbswitchingdemo.dto;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataSourceDTO {
    private HikariDataSource dataSource;
    private String dataSourceKey;
    private String databaseName;
    private String host;
    private Integer port;
    private String role;
}

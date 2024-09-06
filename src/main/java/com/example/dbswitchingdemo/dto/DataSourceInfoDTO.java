package com.example.dbswitchingdemo.dto;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataSourceInfoDTO {
    private HikariDataSource dataSource;
    private String dataSourceKey;
    private String databaseName;
    private String role;
}

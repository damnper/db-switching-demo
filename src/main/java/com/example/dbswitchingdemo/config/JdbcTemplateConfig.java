package com.example.dbswitchingdemo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    @Bean("jdbcTemplate1")
    public JdbcTemplate jdbcTemplate1(@Qualifier("db1MultiRouting") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("jdbcTemplate2")
    public JdbcTemplate jdbcTemplate2(@Qualifier("db2MultiRouting") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

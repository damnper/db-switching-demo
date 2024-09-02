package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.entity.DbSwitchLog;
import com.example.dbswitchingdemo.enums.DataSourceType;
import com.example.dbswitchingdemo.repo.DbSwitchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/db-switch")
@RequiredArgsConstructor
public class DatabaseSwitchController {

    private final DbSwitchLogRepository dbSwitchLogRepository;

    @PostMapping("/leader")
    public String switchToLeaderDb() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.LEADER);

        DbSwitchLog log = new DbSwitchLog();
        log.setSwitchToDb("leader_db");
        log.setSwitchTimestamp(LocalDateTime.now());
        dbSwitchLogRepository.save(log);

        return "Switched to leader_db and logged the switch.";
    }

    @PostMapping("/replica")
    public String switchToReplicaDb() {
        DataSourceContextHolder.setDataSourceType(DataSourceType.REPLICA);

        DbSwitchLog log = new DbSwitchLog();
        log.setSwitchToDb("replica_db");
        log.setSwitchTimestamp(LocalDateTime.now());
        dbSwitchLogRepository.save(log);

        return "Switched to replica_db and logged the switch.";
    }
}

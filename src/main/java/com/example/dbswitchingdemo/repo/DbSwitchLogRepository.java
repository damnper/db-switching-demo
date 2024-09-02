package com.example.dbswitchingdemo.repo;

import com.example.dbswitchingdemo.entity.DbSwitchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbSwitchLogRepository extends JpaRepository<DbSwitchLog, Long> {
}

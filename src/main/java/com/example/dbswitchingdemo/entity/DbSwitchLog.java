package com.example.dbswitchingdemo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "db_switch_log", schema = "public")
public class DbSwitchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "db_seq_gen")
    @SequenceGenerator(name = "db_seq_gen", sequenceName = "db_seq", allocationSize = 1)
    private Long id;

    @Column(name = "switch_to_db", nullable = false)
    private String switchToDb;

    @Column(name = "switch_timestamp", nullable = false)
    private LocalDateTime switchTimestamp;
}

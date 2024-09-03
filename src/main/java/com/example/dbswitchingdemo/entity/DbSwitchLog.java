package com.example.dbswitchingdemo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * <p> Класс-сущность для хранения информации о переключении источников данных. </p>
 * <p> Таблица в базе данных хранит записи о времени, когда произошло переключение на другой источник данных. </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "database_switch_log", schema = "public")
public class DbSwitchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "switch_time", nullable = false)
    private LocalDateTime switchTime;
}

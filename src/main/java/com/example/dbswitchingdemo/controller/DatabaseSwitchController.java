package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p> Контроллер для обработки запросов на переключение источников данных. </p>
 * <p> Этот контроллер предоставляет конечную точку API для переключения leader источника данных
 * на replica. </p>
 */
@RestController
@RequestMapping("/db-switch")
@RequiredArgsConstructor
public class DatabaseSwitchController {

    private final DataSourceService dataSourceService;

    /**
     * Переключает leader источник данных на replica.
     *
     * @return {@link ResponseEntity} с результатом операции и соответствующим HTTP-статусом
     */
    @PostMapping("/switch")
    public ResponseEntity<CommonResponse> switchDataSource() {
        CommonResponse response = dataSourceService.switchDataSource();
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}

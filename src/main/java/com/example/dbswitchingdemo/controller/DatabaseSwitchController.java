package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p> Контроллер для обработки запросов на переключение источников данных. </p>
 * <p> Этот контроллер предоставляет конечную точку API для переключения текущего источника данных
 * на основе переданного имени. </p>
 */
@RestController
@RequestMapping("/db-switch")
@RequiredArgsConstructor
public class DatabaseSwitchController {

    private final DataSourceService dataSourceService;

    /**
     * Переключает текущий источник данных на указанный.
     * <p>
     * Принимает имя источника данных в качестве параметра запроса, пытается переключиться на него
     * и возвращает соответствующий ответ.
     * </p>
     *
     * @param name имя источника данных, на который нужно переключиться
     * @return {@link ResponseEntity} с результатом операции и соответствующим HTTP-статусом
     */
    @PostMapping("/switch")
    public ResponseEntity<CommonResponse> switchDataSource(@RequestParam String name) {
        CommonResponse response = dataSourceService.switchDataSource(name);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}

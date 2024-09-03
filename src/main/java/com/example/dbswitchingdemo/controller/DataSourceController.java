package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <p> Контроллер для управления источниками данных (DataSource). </p>
 * <p> Этот контроллер предоставляет конечные точки API для создания нового источника данных
 * и закрытия существующего источника данных. </p>
 */
@RestController
@RequestMapping("/data-source/")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * Создает новый источник данных на основе предоставленных параметров хоста и порта.
     * <p>
     * Принимает параметры запроса для хоста и порта, создает соответствующий источник данных
     * и возвращает результат операции.
     * </p>
     *
     * @param host хост базы данных
     * @param port порт базы данных
     * @return {@link ResponseEntity} с результатом операции и соответствующим HTTP-статусом
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResponse> createDataSource(@RequestParam String host,
                                                           @RequestParam Integer port) {
        CommonResponse response = dataSourceService.createDataSource(host, port);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    /**
     * Закрывает существующий источник данных по его имени.
     * <p>
     * Принимает имя источника данных в качестве параметра запроса, закрывает его
     * и возвращает результат операции.
     * </p>
     *
     * @param name имя источника данных, который нужно закрыть
     * @return {@link ResponseEntity} с результатом операции и соответствующим HTTP-статусом
     */
    @DeleteMapping("/close")
    public ResponseEntity<CommonResponse> closeDataSource(@RequestParam String name) {
        CommonResponse response = dataSourceService.closeDataSource(name);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}

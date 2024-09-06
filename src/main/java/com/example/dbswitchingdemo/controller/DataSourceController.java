package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO;
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
     * @param clusterMemberDTO DTO с информацией о членах кластера
     * @return {@link ResponseEntity} с результатом операции и соответствующим HTTP-статусом
     */
    @PostMapping("/create")
    public ResponseEntity<CommonResponse> createDataSource(@RequestBody ClusterMemberDTO clusterMemberDTO) {
        CommonResponse response = dataSourceService.createDataSources(clusterMemberDTO);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }


    @DeleteMapping("/close")
    public ResponseEntity<CommonResponse> closeDataSource(@RequestBody ClusterMemberDTO clusterMemberDTO) {
        CommonResponse response = dataSourceService.closeDataSource(clusterMemberDTO);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}

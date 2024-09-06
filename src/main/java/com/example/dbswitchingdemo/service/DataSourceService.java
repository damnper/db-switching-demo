package com.example.dbswitchingdemo.service;

import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO;
import com.example.dbswitchingdemo.dto.response.CommonResponse;

public interface DataSourceService {

    /**
     * Создаёт новый источник данных.
     * <p>
     * Выполняет предварительное подключение к базе данных для проверки доступности,
     * а затем создаёт и добавляет источник данных в карту источников.
     * </p>
     *
     * @param clusterMemberDTO DTO, содержащее информацию о кластере и его членах
     * @return объект {@link CommonResponse} с результатом операции
     */
    CommonResponse createDataSources(ClusterMemberDTO clusterMemberDTO);


    CommonResponse switchDataSource();


    CommonResponse closeDataSource(ClusterMemberDTO clusterMemberDTO);
}

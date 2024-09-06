package com.example.dbswitchingdemo.service;

import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO;
import com.example.dbswitchingdemo.dto.response.CommonResponse;

/**
 * <p> Интерфейс для динамического управления источниками данных. </p>
 */
public interface DynamicDatabaseService {

    /**
     * Обновляет список источников данных на основе новых данных о кластере.
     *
     * @param clusterMemberDTO DTO с данными о членах кластера
     * @return результат выполнения обновления в виде CommonResponse
     */
    CommonResponse refresh(ClusterMemberDTO clusterMemberDTO);

    /**
     * Переключает текущий источник данных на реплику, если подключен к лидеру.
     *
     * @return результат переключения в виде CommonResponse
     */
    CommonResponse change();

    /**
     * Закрывает и удаляет источники данных, которые не соответствуют текущему списку членов кластера.
     *
     * @param clusterMemberDTO DTO с данными о членах кластера
     * @return результат выполнения операции в виде CommonResponse
     */
    CommonResponse close(ClusterMemberDTO clusterMemberDTO);
}

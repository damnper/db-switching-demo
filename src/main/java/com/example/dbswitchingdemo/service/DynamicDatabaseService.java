package com.example.dbswitchingdemo.service;

import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO;
import com.example.dbswitchingdemo.dto.response.CommonResponse;

/**
 * <p> Интерфейс для динамического управления источниками данных. </p>
 */
public interface DynamicDatabaseService {

    /**
     * Обновляет пул источников данных на основе новых членов кластера.
     *
     * @param clusterMemberDTO DTO, содержащий список новых членов кластера
     * @return объект {@link CommonResponse}, представляющий результат операции
     */
    CommonResponse refresh(ClusterMemberDTO clusterMemberDTO);

    /**
     * Переключает контекст подключения с текущего лидера на реплику, если необходимо.
     *
     * @return объект {@link CommonResponse}, представляющий результат переключения
     */
    CommonResponse change();

    /**
     * Закрывает неиспользуемые источники данных на основе информации о членах кластера.
     *
     * @param clusterMemberDTO DTO, содержащий список членов кластера
     * @return объект {@link CommonResponse}, представляющий результат операции закрытия
     */
    CommonResponse close(ClusterMemberDTO clusterMemberDTO);
}

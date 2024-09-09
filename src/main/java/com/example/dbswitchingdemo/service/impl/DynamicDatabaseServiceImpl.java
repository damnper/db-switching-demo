package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.config.DataSourceConfig;
import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.config.DataSourceProperties;
import com.example.dbswitchingdemo.config.MultiRoutingDataSource;
import com.example.dbswitchingdemo.dto.DataSourceDTO;
import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO;
import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO.MemberDTO;
import com.example.dbswitchingdemo.dto.response.CommonDataResponse;
import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.entity.DbSwitchLog;
import com.example.dbswitchingdemo.exception.DataSourceFailedConnectionException;
import com.example.dbswitchingdemo.exception.LogSwitchFailedException;
import com.example.dbswitchingdemo.repo.DbSwitchLogRepository;
import com.example.dbswitchingdemo.service.DynamicDatabaseService;
import com.example.dbswitchingdemo.util.DataSourceManager;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Реализация сервиса для динамического управления источниками данных.
 * Этот сервис поддерживает создание, переключение и закрытие источников данных в кластере.
 * Также проверяет соединение с базой данных перед созданием нового источника и логирует изменения.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDatabaseServiceImpl implements DynamicDatabaseService {

    private final DataSourceProperties dsProperties;
    private final DataSourceConfig dsConfig;
    private final MultiRoutingDataSource dsMultiRouting;
    private final DbSwitchLogRepository dsRepo;

    private final Map<String, DataSourceDTO> dsActivePool = new ConcurrentHashMap<>();

    /**
     * Инициализация пула источников данных с фейковым источником данных для базовой настройки.
     */
    @PostConstruct
    private void init() {
        Object dsFake = dsMultiRouting.getTargetDataSources().get("fakeDataSourceKey");
        dsActivePool.put("fakeDataSourceKey", new DataSourceDTO(
                (HikariDataSource) dsFake,
                "fakeDataSourceKey",
                "fakeDatabaseName",
                "fakeHost",
                0,
                "fakeRole") );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse refresh(ClusterMemberDTO clusterMemberDTO) {
        List<MemberDTO> members = clusterMemberDTO.getMembers();

        if (members.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.BAD_REQUEST.name())
                    .message("Cluster members list is empty.")
                    .build();
        }

        return handleRefresh(members);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse change() {
        DataSourceDTO dsLeader = DataSourceManager.findLeaderDataSource(dsActivePool);

        String curContext = DataSourceContextHolder.getDataSourceContext()
                .orElse(dsLeader.getDataSourceKey());

        return handleSwitch(dsLeader, curContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse close(ClusterMemberDTO clusterMemberDTO) {
        List<MemberDTO> members = clusterMemberDTO.getMembers();
        Set<String> memberNames = members.stream()
                .map(DataSourceManager::buildUniqueKey)
                .collect(Collectors.toSet());

        return handleClose(memberNames);
    }

    /**
     * Обрабатывает обновление источников данных на основе списка членов кластера.
     *
     * @param members список новых членов кластера
     * @return объект {@link CommonResponse}, представляющий результат операции обновления
     */
    private CommonResponse handleRefresh(List<MemberDTO> members) {
        List<DataSourceDTO> dsNewList = new ArrayList<>();

        members.forEach(member -> {
            String dsKey = DataSourceManager.buildUniqueKey(member);
            if (DataSourceManager.checkStatus(member, dsKey, dsActivePool)) return;

            try {
                HikariDataSource dsNew = DataSourceManager.create(member.getHost(), member.getPort(), dsKey, dsProperties, dsConfig);
                DataSourceDTO dsNewDTO = DataSourceManager.add(
                        member.getHost(), member.getPort(), dsKey, dsProperties.getName(),
                        member.getRole(), dsNew, dsMultiRouting, dsActivePool
                );

                dsNewList.add(dsNewDTO);
            } catch (DataSourceFailedConnectionException e) {
                log.error("Error creating DataSource: {}", e.getMessage());
            }
        });

        if (dsNewList.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.OK.name())
                    .message("Data sources' roles were updated successfully.")
                    .build();
        }

        return CommonDataResponse.builder().status(HttpStatus.CREATED.name())
                .message("Data sources created successfully for all valid cluster members!")
                .data(dsNewList.stream().map(DataSourceDTO::getDataSourceKey).toList())
                .build();
    }

    /**
     * Обрабатывает переключение с лидера на реплику.
     *
     * @param dsLeader текущий лидер источников данных
     * @param curContext текущий контекст подключения
     * @return объект {@link CommonResponse}, представляющий результат переключения
     */
    private CommonResponse handleSwitch(DataSourceDTO dsLeader, String curContext) {
        if (dsLeader != null && curContext.equals(dsLeader.getDataSourceKey()) ) {
            DataSourceDTO dsReplica = DataSourceManager.findReplicaDataSource(dsActivePool);

            DataSourceContextHolder.setDataSourceContext(dsReplica.getDataSourceKey());

            return processSwitchResult(dsReplica.getDataSourceKey(), true, "Switched to replica DataSource");
        }
        return processSwitchResult(curContext, false, "Already connected to replica DataSource");
    }

    /**
     * Обрабатывает результат переключения источника данных.
     *
     * @param dsKey ключ нового источника данных
     * @param switched флаг, указывающий на успешное переключение
     * @param message сообщение о результате переключения
     * @return объект {@link CommonResponse} с результатом операции
     */
    private CommonResponse processSwitchResult(String dsKey, boolean switched, String message) {
        Optional<String> newContext = DataSourceContextHolder.getDataSourceContext();

        if (newContext.isPresent() && switched) { // NOTE: Это не обязательный функционал, используется для теста, что переключение соединения действительно произошло.
            logSwitchRecord(newContext.get());
        }

        return CommonDataResponse.builder().status(HttpStatus.OK.name()).data(switched)
                .message(message + " '" + dsKey + "' successfully.")
                .build();
    }

    /**
     * Логирует переключение источника данных, записывая данные в репозиторий логов.
     *
     * @param dsName имя нового источника данных
     */
    private void logSwitchRecord(String dsName) {
        try {
            dsRepo.save(DbSwitchLog.builder()
                    .switchTime(LocalDateTime.now())
                    .build());
            log.info("Switching data source to: {}", dsName);
        } catch (Exception e) {
            throw new LogSwitchFailedException("Error while switching data source: " + dsName);
        }
    }

    /**
     * Обрабатывает закрытие неиспользуемых источников данных.
     *
     * @param memberNames набор уникальных ключей членов кластера
     * @return объект {@link CommonResponse}, представляющий результат операции закрытия
     */
    private CommonResponse handleClose(Set<String> memberNames) {
        List<DataSourceDTO> dsClosedList = DataSourceManager.remove(memberNames, dsMultiRouting, dsActivePool);

        if (dsClosedList.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.NOT_MODIFIED.name())
                    .build();
        }

        return CommonDataResponse.builder().status(HttpStatus.OK.name())
                .message("DataSources closed successfully.")
                .data(dsClosedList.stream()
                        .map(DataSourceDTO::getDataSourceKey)
                        .toList())
                .build();
    }

}

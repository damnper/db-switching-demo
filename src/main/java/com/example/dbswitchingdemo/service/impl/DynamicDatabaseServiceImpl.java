package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.config.DataSourceConfig;
import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.config.DataSourceProperties;
import com.example.dbswitchingdemo.config.MultiRoutingDataSource;
import com.example.dbswitchingdemo.dto.DataSourceContextDTO;
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
 * <p> Реализация сервиса для динамического управления источниками данных. </p>
 * <p> Этот сервис позволяет выполнять создание, переключение и закрытие источников данных в кластере.
 * Также проверяет соединение с базой данных перед созданием нового источника данных и логирует изменения. </p>
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
     * Инициализация пула источников данных с фейковым источником для базовой настройки.
     */
    @PostConstruct
    private void init() {
        Object dsFake = dsMultiRouting.getTargetDataSources().get("fakeDatabaseName");
        dsActivePool.put("fakeDataSource", new DataSourceDTO(
                (HikariDataSource) dsFake,
                "fakeDataSourceKey",
                "fakeDatabaseName",
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

        DataSourceContextDTO curContext = DataSourceContextHolder.getDataSourceContext()
                .orElse(new DataSourceContextDTO(dsLeader.getDataSourceKey(), dsLeader.getDatabaseName()));

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
     * Обрабатывает обновление источников данных на основе списка новых членов кластера.
     *
     * @param members список членов кластера
     * @return результат выполнения операции в виде CommonResponse
     */
    private CommonResponse handleRefresh(List<MemberDTO> members) {
        List<DataSourceDTO> dsNewList = new ArrayList<>();

        members.forEach(member -> {
            String dsKey = DataSourceManager.buildUniqueKey(member);
            if (DataSourceManager.checkStatus(member, dsKey, dsActivePool)) return;

            try {
                HikariDataSource dsNew = DataSourceManager.create(member, dsKey, dsProperties, dsConfig);
                DataSourceDTO dsNewDTO = DataSourceManager.add(
                        dsKey, dsProperties.getName(), member.getRole(),
                        dsNew, dsMultiRouting, dsActivePool
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
     * @return результат переключения в виде CommonResponse
     */
    private CommonResponse handleSwitch(DataSourceDTO dsLeader, DataSourceContextDTO curContext) {
        if (dsLeader != null && curContext.dataSourceKey().equals(dsLeader.getDataSourceKey()) ) {
            DataSourceDTO dsReplica = DataSourceManager.findReplicaDataSource(dsActivePool);

            DataSourceContextHolder.setDataSourceContext(
                    new DataSourceContextDTO(dsReplica.getDataSourceKey(), dsReplica.getDatabaseName()));

            return processSwitchResult(dsReplica.getDataSourceKey(), true, "Switched to replica DataSource");
        }
        return processSwitchResult(curContext.dataSourceKey(), false, "Already connected to replica DataSource");
    }

    /**
     * Обрабатывает результат переключения источника данных.
     *
     * @param dsKey ключ источника данных
     * @param switched флаг, указывающий, было ли успешное переключение
     * @param message сообщение о результате
     * @return объект CommonResponse с результатом операции
     */
    private CommonResponse processSwitchResult(String dsKey, boolean switched, String message) {
        Optional<DataSourceContextDTO> newContext = DataSourceContextHolder.getDataSourceContext();

        if (newContext.isPresent() && switched) { // NOTE: Это не обязательный функционал, используется для теста, что переключение соединения действительно произошло.
            logSwitchRecord(newContext.get().dataSourceKey());
        }

        return CommonDataResponse.builder().status(HttpStatus.OK.name()).data(switched)
                .message(message + " '" + dsKey + "' successfully.")
                .build();
    }

    /**
     * Логирует переключение источника данных, записывая данные в репозиторий логов.
     *
     * @param dsName имя источника данных
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
     * @param memberNames набор ключей членов кластера
     * @return результат выполнения операции в виде CommonResponse
     */
    private CommonResponse handleClose(Set<String> memberNames) {
        List<DataSourceDTO> dsClosedList = DataSourceManager.remove(memberNames, dsMultiRouting, dsActivePool);

        if (dsClosedList.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.NOT_MODIFIED.name())
                    .message("No DataSources were closed as all are up-to-date.")
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

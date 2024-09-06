package com.example.dbswitchingdemo.service.impl;

import com.example.dbswitchingdemo.config.DataSourceConfig;
import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.config.DataSourceProperties;
import com.example.dbswitchingdemo.config.MultiRoutingDataSource;
import com.example.dbswitchingdemo.dto.DataSourceContextDTO;
import com.example.dbswitchingdemo.dto.DataSourceInfoDTO;
import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO;
import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO.MemberDTO;
import com.example.dbswitchingdemo.dto.response.CommonDataResponse;
import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.entity.DbSwitchLog;
import com.example.dbswitchingdemo.exception.DataSourceFailedConnectionException;
import com.example.dbswitchingdemo.exception.DataSourceNotCloseException;
import com.example.dbswitchingdemo.exception.LogSwitchFailedException;
import com.example.dbswitchingdemo.exception.ResourceNotFound;
import com.example.dbswitchingdemo.repo.DbSwitchLogRepository;
import com.example.dbswitchingdemo.service.DataSourceService;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p> Реализация сервиса для управления источниками данных. </p>
 * <p> Этот сервис обеспечивает создание, переключение и закрытие источников данных.
 * Также выполняет проверку соединения с базой данных перед созданием нового источника. </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {

    private static final String FAKE_DATASOURCE_NAME = "fakeDataSource";
    public static final String JDBC_POSTGRESQL = "jdbc:postgresql://%s:%d/%s";

    private final DataSourceProperties dataSourceProperties;
    private final DataSourceConfig dataSourceConfig;
    private final MultiRoutingDataSource routingDataSource;
    private final DbSwitchLogRepository dbSwitchLogRepository;

    private final Map<String, DataSourceInfoDTO> dataSources = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        dataSources.put(FAKE_DATASOURCE_NAME, new DataSourceInfoDTO(
                new HikariDataSource(),
                "fakeDataSourceKey",
                "fakeDatabaseName",
                "fakeRole") );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse createDataSources(ClusterMemberDTO clusterMemberDTO) {
        List<MemberDTO> members = clusterMemberDTO.getMembers();

        if (members.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.BAD_REQUEST.name())
                    .message("Cluster members list is empty.")
                    .build();
        }

        List<DataSourceInfoDTO> newDataSources = processMembers(members);

        if (newDataSources.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.NOT_MODIFIED.name()).build();
        }

        return CommonDataResponse.builder().status(HttpStatus.OK.name())
                .message("Data sources created successfully for all valid cluster members!")
                .data(newDataSources.stream().map(DataSourceInfoDTO::getDataSourceKey).toList())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse switchDataSource() {
        Map.Entry<String, DataSourceInfoDTO> leaderEntry = dataSources.entrySet().stream()
                .filter(entry -> "leader".equals(entry.getValue().getRole()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFound("Leader not found"));

        String leaderDataSourceKey = leaderEntry.getKey();
        String leaderDatabaseName = leaderEntry.getValue().getDatabaseName();

        DataSourceContextDTO currentContext = DataSourceContextHolder.getDataSourceContext()
                .orElse(new DataSourceContextDTO(leaderDataSourceKey, leaderDatabaseName));

        if (currentContext.dataSourceKey().equals(leaderDataSourceKey)){
            Map.Entry<String, DataSourceInfoDTO> replicaEntry = dataSources.entrySet().stream()
                    .filter(entry -> "replica".equals(entry.getValue().getRole()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFound("No replica found to switch to."));

            String replicaName = replicaEntry.getKey();
            String replicaDatabaseName = replicaEntry.getValue().getDatabaseName();

            DataSourceContextHolder.setDataSourceContext(new DataSourceContextDTO(replicaName, replicaDatabaseName));
            Optional<DataSourceContextDTO> newSourceContext = DataSourceContextHolder.getDataSourceContext();

            newSourceContext.ifPresent(dataSourceContextDTO -> logDataSourceSwitch(dataSourceContextDTO.dataSourceKey()));

            return CommonDataResponse.builder().status(HttpStatus.OK.name()).data(true)
                    .message("Switched to replica DataSource '" + replicaName + "' successfully.")
                    .build();
        }

        return CommonDataResponse.builder().status(HttpStatus.OK.name()).data(true)
                .message("Already connected to replica DataSource '" + currentContext.dataSourceKey() + "'.")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommonResponse closeDataSource(ClusterMemberDTO clusterMemberDTO) {
        List<DataSourceInfoDTO> closedDataSources = new ArrayList<>();
        List<MemberDTO> members = clusterMemberDTO.getMembers();

        Set<String> memberNames = members.stream()
                .map(this::buildUniqueDataSourceKey)
                .collect(Collectors.toSet());

        Iterator<Map.Entry<String, DataSourceInfoDTO>> iterator = dataSources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DataSourceInfoDTO> entry = iterator.next();
            String dataSourceName = entry.getKey();

            if (!memberNames.contains(dataSourceName)) {
                DataSourceInfoDTO removedDataSourceInfo = entry.getValue();
                DataSource removedDataSource = removedDataSourceInfo.getDataSource();
                closeExistingDataSource(removedDataSource);

                closedDataSources.add(removedDataSourceInfo);
                iterator.remove();
            }
        }

        if (closedDataSources.isEmpty()) {
            return CommonResponse.builder().status(HttpStatus.NOT_MODIFIED.name())
                    .message("No DataSources were closed as all are up-to-date.")
                    .build();
        }

        return CommonDataResponse.builder().status(HttpStatus.OK.name())
                .message("DataSources closed successfully.")
                .data(closedDataSources.stream().map(DataSourceInfoDTO::getDataSourceKey).toList())
                .build();
    }

    private List<DataSourceInfoDTO> processMembers(List<MemberDTO> members) {
        List<DataSourceInfoDTO> newDataSources = new ArrayList<>();

        for (MemberDTO member : members) {
            String dataSourceKey = buildUniqueDataSourceKey(member);
            if (dataSources.containsKey(dataSourceKey) &&
                    member.getRole().equals(dataSources.get(dataSourceKey).getRole())) {
                log.info("DataSource '{}' already exists, skipping creation.", dataSourceKey);
                continue;
            }

            try {
                DataSourceInfoDTO newDataSource = createAndAddDataSource(member, dataSourceKey, dataSourceProperties.getName());
                newDataSources.add(newDataSource);
            } catch (DataSourceFailedConnectionException e) {
                log.error("Error creating DataSource: {}", e.getMessage());
            }
        }

        return newDataSources;
    }

    private DataSourceInfoDTO createAndAddDataSource(MemberDTO member, String dataSourceKey, String databaseName) {

        String url = buildJdbcUrl(member.getHost(), member.getPort());

        if (!testDatabaseConnection(url)) {
            log.warn("Failed to connect to the database at '{}:{}', skipping creation for this node.", member.getHost(), member.getPort());
        }

        HikariDataSource newDataSource = dataSourceConfig.createHikariDataSource(url);
        DataSourceInfoDTO dataSourceInfoDTO = addDataSource(dataSourceKey, databaseName, member.getRole(), newDataSource);

        log.info("DataSource '{}' created successfully!", dataSourceKey);
        return dataSourceInfoDTO;
    }

    private String buildUniqueDataSourceKey(MemberDTO member) {
        return member.getHost() + ":" + member.getPort();
    }

    /**
     * Формирует URL JDBC для подключения к базе данных.
     *
     * @param host хост базы данных
     * @param port порт базы данных
     * @return сформированный URL JDBC
     */
    private String buildJdbcUrl(String host, int port) {
        String databaseName = dataSourceProperties.getName();
        return String.format(JDBC_POSTGRESQL, host, port, databaseName);
    }

    /**
     * Проверяет соединение с базой данных по переданному URL.
     *
     * @param url URL JDBC для подключения к базе данных
     * @return true, если соединение успешно, иначе false
     */
    private boolean testDatabaseConnection(String url) {
        try (Connection ignored = DriverManager.getConnection(
                        url,
                        dataSourceProperties.getUsername(),
                        dataSourceProperties.getPassword())) {
            log.info("Connection is successfully established for '{}'", url);
            return true;
        } catch (SQLException e) {
            log.error("Failed to connect to database at '{}'", url);
            return false;
        }
    }

    /**
     * Добавляет источник данных в карту источников и настраивает маршрутизацию.
     *
     * @param dataSourceKey       уникальное имя источника данных
     * @param dataSource источник данных
     */
    private DataSourceInfoDTO addDataSource(String dataSourceKey, String databaseName, String role, HikariDataSource dataSource) {
        dataSources.put(dataSourceKey, new DataSourceInfoDTO(dataSource, dataSourceKey, databaseName, role));
        DataSourceInfoDTO newDataSource = dataSources.get(dataSourceKey);
        routingDataSource.addDataSource(databaseName, dataSource);
        return newDataSource;
    }

    /**
     * Логирует переключение источника данных, сохраняет время переключения в базу данных.
     */
    private void logDataSourceSwitch(String dataSourceName) {
        try {
            dbSwitchLogRepository.save(DbSwitchLog.builder()
                    .switchTime(LocalDateTime.now())
                    .build());
            log.info("Switching data source to: {}", dataSourceName);
        } catch (Exception e) {
            throw new LogSwitchFailedException("Error while switching data source: " + dataSourceName);
        }
    }

    /**
     * Закрывает и удаляет указанный источник данных.
     *
     * @param dataSource источник данных
     */
    private void closeExistingDataSource(DataSource dataSource) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                throw new DataSourceNotCloseException("Error while closing data source: " + e.getMessage());
            }
        }
    }
}

package com.example.dbswitchingdemo.util;

import com.example.dbswitchingdemo.config.DataSourceConfig;
import com.example.dbswitchingdemo.config.DataSourceContextHolder;
import com.example.dbswitchingdemo.config.DataSourceProperties;
import com.example.dbswitchingdemo.config.MultiRoutingDataSource;
import com.example.dbswitchingdemo.dto.DataSourceDTO;
import com.example.dbswitchingdemo.dto.request.ClusterMemberDTO.MemberDTO;
import com.example.dbswitchingdemo.enums.DataSourceStatus;
import com.example.dbswitchingdemo.exception.DataSourceFailedConnectionException;
import com.example.dbswitchingdemo.exception.DataSourceNotCloseException;
import com.example.dbswitchingdemo.exception.ResourceNotFound;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Утилитарный класс для управления источниками данных.
 * Содержит методы для создания, проверки, удаления и закрытия источников данных.
 */
@Slf4j
public class DataSourceManager {

    /**
     * Шаблон URL JDBC для подключения к PostgreSQL.
     */
    public static final String JDBC_POSTGRESQL = "jdbc:postgresql://%s:%d/%s";

    /**
     * Создает новый источник данных для члена кластера.
     *
     * @param member объект MemberDTO с данными члена кластера
     * @param dataSourceKey уникальный ключ источника данных
     * @param dsProperties настройки подключения к базе данных
     * @param dataSourceConfig конфигурация для создания источника данных
     * @return созданный HikariDataSource
     * @throws DataSourceFailedConnectionException если не удалось подключиться к базе данных
     */
    public static HikariDataSource create(MemberDTO member,
                                          String dataSourceKey,
                                          DataSourceProperties dsProperties,
                                          DataSourceConfig dataSourceConfig) {
        try {
            String url = buildJdbcUrl(member.getHost(), member.getPort(), dsProperties.getName());
            testDatabaseConnection(member, url, dsProperties);

            HikariDataSource newDataSource = dataSourceConfig.createHikariDataSource(url);

            log.info("DataSource '{}' created successfully!", dataSourceKey);
            return newDataSource;
        } catch (Exception e) {
            throw new DataSourceFailedConnectionException(e.getMessage());
        }
    }

    /**
     * Генерирует уникальный ключ для источника данных на основе хоста и порта.
     *
     * @param member объект MemberDTO с данными члена кластера
     * @return уникальный ключ источника данных
     */
    public static String buildUniqueKey(MemberDTO member) {
        return member.getHost() + ":" + member.getPort();
    }

    public static boolean checkStatus(MemberDTO member, String dsKey, Map<String, DataSourceDTO> dsActivePool) {
        DataSourceStatus dsStatus = getStatus(member, dsKey, dsActivePool);

        if (dsStatus.equals(DataSourceStatus.EXISTS)) {
            return true;
        } else if (dsStatus.equals(DataSourceStatus.ROLE_CHANGED)) {
            DataSourceManager.updateRole(member, dsKey, dsActivePool);
            return true;
        }
        return false;
    }

    public static void updateRole(MemberDTO member, String dsKey, Map<String, DataSourceDTO> dsActivePool) {
        DataSourceDTO dataSourceDTO = dsActivePool.get(dsKey);

        dataSourceDTO.setRole(member.getRole());
        dsActivePool.put(dsKey, dataSourceDTO);

        log.info("Role for DataSource '{}' has been updated to '{}'.", dsKey, member.getRole());
    }

    /**
     * Добавляет новый источник данных в пул и маршрутизацию.
     *
     * @param dsKey ключ источника данных
     * @param dbName имя базы данных
     * @param role роль источника данных (leader или replica)
     * @param ds объект HikariDataSource для добавления
     * @param dsMultiRouting объект MultiRoutingDataSource для маршрутизации
     * @param dsActivePool пул активных источников данных
     * @return добавленный DataSourceDTO
     */
    public static DataSourceDTO add(String dsKey,
                                    String dbName,
                                    String role,
                                    HikariDataSource ds,
                                    MultiRoutingDataSource dsMultiRouting,
                                    Map<String, DataSourceDTO> dsActivePool) {
        dsActivePool.put(dsKey, new DataSourceDTO(ds, dsKey, dbName, role));
        DataSourceDTO dsNewDTO = dsActivePool.get(dsKey);
        dsMultiRouting.addDataSource(dbName, ds);
        return dsNewDTO;
    }

    /**
     * Находит источник данных с ролью leader.
     *
     * @param dsActivePool пул активных источников данных
     * @return DataSourceDTO с ролью leader или null, если не найдено
     */
    public static DataSourceDTO findLeaderDataSource(Map<String, DataSourceDTO> dsActivePool) {
        return dsActivePool.values().stream()
                .filter(dsDTO -> "leader".equals(dsDTO.getRole()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Находит источник данных с ролью replica.
     *
     * @param dsActivePool пул активных источников данных
     * @return DataSourceDTO с ролью replica
     * @throws ResourceNotFound если реплика не найдена
     */
    public static DataSourceDTO findReplicaDataSource(Map<String, DataSourceDTO> dsActivePool) {
        return dsActivePool.values().stream()
                .filter(dataSourceInfoDTO -> "replica".equals(dataSourceInfoDTO.getRole()))
                .findAny()
                .orElseThrow(() -> new ResourceNotFound("No replica found to switch to."));
    }

    /**
     * Удаляет источники данных, которые больше не используются в текущем состоянии кластера
     * из активного пула и маршрутизации.
     *
     *
     * @param memberNames набор имен активных членов кластера
     * @param dsActivePool пул активных источников данных
     * @return список удаленных источников данных
     */
    public static List<DataSourceDTO> remove(Set<String> memberNames,
                                             MultiRoutingDataSource dsMultiRouting,
                                             Map<String, DataSourceDTO> dsActivePool) {
        List<DataSourceDTO> dsClosedList = new ArrayList<>(); // список закрытых data source
        Iterator<Map.Entry<String, DataSourceDTO>> iterator = dsActivePool.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, DataSourceDTO> entry = iterator.next();
            String dsKey = entry.getKey();

            if (!memberNames.contains(dsKey)) {
                DataSourceDTO dsDTOToRemoved = entry.getValue();
                DataSource dsToRemoved = dsDTOToRemoved.getDataSource();

                // удаление из маршрутизатора и активного пула
                dsMultiRouting.removeDataSource(dsDTOToRemoved.getDatabaseName(), dsDTOToRemoved.getDataSourceKey());
                iterator.remove();

                closeExistingDataSource(dsToRemoved); // закрытие соединения

                clearContextDataSourceIfEqualsRemovedDataSource(dsDTOToRemoved);

                log.info("DataSource '{}' removed from the active pool and routing.", dsKey);
                dsClosedList.add(dsDTOToRemoved);
            }
        }
        return dsClosedList;
    }

    /**
     * Очищает контекст источника данных, если текущий источник совпадает с удаляемым источником данных.
     *
     * <p> Этот метод проверяет, присутствует ли текущий контекст источника данных в {@link DataSourceContextHolder}.
     * Если ключ текущего источника данных совпадает с ключом удаляемого источника, контекст очищается. </p>
     *
     * @param dsDTOToRemoved объект {@link DataSourceDTO}, представляющий удаляемый источник данных
     */
    private static void clearContextDataSourceIfEqualsRemovedDataSource(DataSourceDTO dsDTOToRemoved) {
        if (DataSourceContextHolder.getDataSourceContext().isPresent()) {
            if (Objects.equals(DataSourceContextHolder.getDataSourceContext().get().dataSourceKey(),
                    dsDTOToRemoved.getDataSourceKey())) {
                DataSourceContextHolder.clearDataSourceContext();
            }
        }
    }

    /**
     * Проверяет, существует ли источник данных для указанного члена кластера и
     * определяет его статус (существующий, с изменённой ролью или новый).
     *
     * @param member объект MemberDTO с данными члена кластера
     * @param dsKey ключ источника данных
     * @param dsActivePool пул активных источников данных
     * @return статус источника данных: {@link DataSourceStatus#EXISTS}, если источник уже существует с той же ролью,
     * {@link DataSourceStatus#ROLE_CHANGED}, если роль изменена, или {@link DataSourceStatus#NEW}, если источник данных новый
     */
    private static DataSourceStatus getStatus(MemberDTO member,
                                              String dsKey,
                                              Map<String, DataSourceDTO> dsActivePool) {
        if (dsActivePool.containsKey(dsKey) &&
                !member.getRole().equals(dsActivePool.get(dsKey).getRole())) {
            log.info("DataSource '{}' already exists, but role is changed, need to update datasource.", dsKey);
            return DataSourceStatus.ROLE_CHANGED;
        }

        if (dsActivePool.containsKey(dsKey) &&
                member.getRole().equals(dsActivePool.get(dsKey).getRole())) {
            log.info("DataSource '{}' already exists, skipping creation.", dsKey);
            return DataSourceStatus.EXISTS;
        }
        return DataSourceStatus.NEW;
    }

    /**
     * Формирует URL JDBC для подключения к базе данных.
     *
     * @param host хост базы данных
     * @param port порт базы данных
     * @param databaseName имя базы данных
     * @return сгенерированный URL JDBC
     */
    private static String buildJdbcUrl(String host, int port, String databaseName) {
        return String.format(JDBC_POSTGRESQL, host, port, databaseName);
    }

    /**
     * Тестирует соединение с базой данных.
     *
     * @param member объект MemberDTO с данными члена кластера
     * @param url URL подключения к базе данных
     * @param dsProperties настройки подключения
     */
    private static void testDatabaseConnection(MemberDTO member,
                                               String url,
                                               DataSourceProperties dsProperties) {
        try (Connection ignored = DriverManager.getConnection(url, dsProperties.getUsername(), dsProperties.getPassword())) {
            log.info("Connection is successfully established for '{}'", url);
        } catch (SQLException e) {
            log.warn("Failed to connect to the database at '{}:{}', skipping creation for this node.", member.getHost(), member.getPort());
        }
    }

    /**
     * Закрывает указанный источник данных.
     *
     * @param ds источник данных для закрытия
     * @throws DataSourceNotCloseException если не удалось закрыть источник данных
     */
    private static void closeExistingDataSource(DataSource ds) {
        if (ds instanceof AutoCloseable) {
            try {
                ((AutoCloseable) ds).close();
            } catch (Exception e) {
                throw new DataSourceNotCloseException("Error while closing data source: " + e.getMessage());
            }
        }
    }
}

package com.example.dbswitchingdemo.config;

import com.example.dbswitchingdemo.dto.DataSourceContextDTO;
import io.micrometer.common.lang.NonNullApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Класс {@code MultiRoutingDataSource} представляет собой расширение {@link AbstractRoutingDataSource},
 * которое позволяет динамически управлять источниками данных в зависимости от контекста выполнения.</p>
 *
 * <p>Этот класс предоставляет возможность добавлять новые источники данных в режиме выполнения и
 * автоматически маршрутизировать запросы к правильному источнику данных, исходя из текущего ключа контекста,
 * который хранится в {@link DataSourceContextHolder}.</p>
 *
 * <p>Основное использование данного класса заключается в создании мульти-тенантной архитектуры
 * или других сценариев, где требуется работать с несколькими базами данных в одном приложении.</p>
 */
@Getter
@NonNullApi
@RequiredArgsConstructor
@Slf4j
public class MultiRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    /**
     * Определяет текущий ключ источника данных, который будет использоваться для маршрутизации запросов.
     * <p>Этот метод извлекает имя базы данных из {@link DataSourceContextHolder}, который
     * содержит информацию о текущем контексте источника данных, основанную на контексте выполнения.</p>
     *
     * @return имя текущей базы данных для маршрутизации
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceContext()
                .map(DataSourceContextDTO::databaseName)
                .orElse(null);
    }

    /**
     * Устанавливает целевые источники данных, которые будут использоваться для маршрутизации запросов.
     * <p>Этот метод принимает карту источников данных, где ключ — это имя базы данных, а значение — это
     * {@link DataSource}, который представляет собой соединение с конкретной базой данных.</p>
     *
     * <p>После установки источников данных метод вызывает суперклассовую реализацию для обновления конфигурации
     * маршрутизатора с новыми данными.</p>
     *
     * @param targetDataSources карта целевых источников данных, где ключ — это имя базы данных, а значение — {@link DataSource}
     */
    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources.putAll(targetDataSources);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * Добавляет новый источник данных в карту целевых источников данных и обновляет конфигурацию маршрутизатора.
     * <p>Данный метод позволяет динамически добавлять новые источники данных в существующую конфигурацию.
     * После добавления нового источника данных вызов {@link #afterPropertiesSet()} обновляет настройки
     * маршрутизации для правильной работы с новыми источниками.</p>
     *
     * @param dbName имя новой базы данных
     * @param ds новый {@link DataSource}, который необходимо добавить
     */
    public void addDataSource(String dbName, DataSource ds) {
        this.targetDataSources.put(dbName, ds);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * Удаляет источник данных из карты целевых источников данных и обновляет конфигурацию маршрутизатора.
     * <p>После удаления источника данных, метод вызывает {@link #afterPropertiesSet()} для обновления
     * конфигурации маршрутизации.</p>
     *
     * @param dbName имя базы данных, которую необходимо удалить
     * @param dsKey имя уникального ключа, для логирования удаления
     */
    public void removeDataSource(String dbName,String dsKey) {
        this.targetDataSources.remove(dbName);
        setTargetDataSources(this.targetDataSources);
        afterPropertiesSet();
        log.info("Removed data source by key: {}", dsKey);
    }
}

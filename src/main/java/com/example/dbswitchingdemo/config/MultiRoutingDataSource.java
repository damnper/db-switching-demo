package com.example.dbswitchingdemo.config;

import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p> Класс MultiRoutingDataSource представляет собой расширение {@link AbstractRoutingDataSource},
 * которое поддерживает динамическое переключение между различными источниками данных в зависимости от контекста. </p>
 * <p> Этот класс позволяет добавлять новые источники данных в режиме выполнения и автоматически управлять маршрутизацией
 * запросов к соответствующему источнику данных на основе текущего ключа контекста. </p>
 */
@NonNullApi
@RequiredArgsConstructor
public class MultiRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    /**
     * Определяет текущий ключ источника данных, который будет использоваться для маршрутизации запросов.
     * <p>
     * Этот метод извлекает ключ источника данных из {@link DataSourceContextHolder}.
     * </p>
     *
     * @return текущий ключ источника данных
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSource();
    }

    /**
     * Устанавливает целевые источники данных, которые будут использоваться для маршрутизации.
     * <p>
     * Сохраняет переданную карту целевых источников данных и вызывает методы суперкласса
     * для обновления конфигурации.
     * </p>
     *
     * @param targetDataSources карта целевых источников данных, где ключ - это имя источника, а значение - сам {@link DataSource}
     */
    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources.putAll(targetDataSources);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * Добавляет новый источник данных в карту целевых источников данных и обновляет конфигурацию маршрутизатора.
     *
     * @param key ключ (имя) нового источника данных
     * @param dataSource новый {@link DataSource}, который нужно добавить
     */
    public void addDataSource(String key, DataSource dataSource) {
        this.targetDataSources.put(key, dataSource);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }
}

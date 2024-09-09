package com.example.dbswitchingdemo.config;

import io.micrometer.common.lang.NonNullApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс {@code MultiRoutingDataSource} расширяет {@link AbstractRoutingDataSource} и позволяет динамически
 * управлять маршрутизацией источников данных на основе текущего контекста выполнения.
 * <p>
 * Этот класс предоставляет возможность добавления, удаления и управления источниками данных во время выполнения.
 * Он автоматически маршрутизирует запросы к правильному источнику данных на основе ключа, полученного из
 * {@link DataSourceContextHolder}.
 * <p>
 * Основной сценарий использования — это приложения с мульти-тенантной архитектурой или любые приложения,
 * которые работают с несколькими базами данных, управляя контекстом выполнения.
 */
@Getter
@NonNullApi
@RequiredArgsConstructor
@Slf4j
public class MultiRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    /**
     * Определяет текущий ключ источника данных для маршрутизации.
     * <p>
     * Этот метод использует {@link DataSourceContextHolder}, чтобы получить ключ текущего источника данных,
     * который будет использоваться для маршрутизации запросов.
     * Если контекст не установлен, метод возвращает {@code null}, что означает, что по умолчанию
     * будет использован основной источник данных.
     *
     * @return ключ текущего источника данных или {@code null}, если контекст отсутствует
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceContext().orElse(null);
    }

    /**
     * Устанавливает целевые источники данных для маршрутизации запросов.
     * <p>
     * Этот метод принимает карту, где ключ — это идентификатор источника данных (например, имя базы данных),
     * а значение — объект {@link DataSource}, представляющий подключение к базе данных.
     * После установки целевых источников данных происходит обновление конфигурации маршрутизации.
     *
     * @param targetDataSources карта целевых источников данных
     */
    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources.putAll(targetDataSources);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * Добавляет новый источник данных и обновляет конфигурацию маршрутизации.
     * <p>
     * Этот метод позволяет в режиме выполнения добавлять новые источники данных, которые сразу же становятся
     * доступными для маршрутизации запросов. После добавления источника данных вызывается {@link #afterPropertiesSet()},
     * чтобы обновить настройки маршрутизатора.
     *
     * @param ds    новый источник данных {@link DataSource}
     * @param dsKey уникальный ключ для нового источника данных
     */
    public void addDataSource(DataSource ds, String dsKey) {
        this.targetDataSources.put(dsKey, ds);
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * Удаляет источник данных и обновляет конфигурацию маршрутизации.
     * <p>
     * Этот метод удаляет источник данных, связанный с указанным ключом, и обновляет настройки маршрутизатора
     * после удаления. После этого запросы больше не будут направляться к удаленному источнику данных.
     *
     * @param dsKey ключ источника данных, который необходимо удалить
     */
    public void removeDataSource(String dsKey) {
        this.targetDataSources.remove(dsKey);
        setTargetDataSources(this.targetDataSources);
        afterPropertiesSet();
        log.info("Removed data source by key: {}", dsKey);
    }
}

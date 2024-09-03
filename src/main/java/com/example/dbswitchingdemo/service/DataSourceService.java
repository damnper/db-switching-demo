package com.example.dbswitchingdemo.service;

import com.example.dbswitchingdemo.dto.response.CommonResponse;

public interface DataSourceService {

    /**
     * Создаёт новый источник данных.
     * <p>
     * Выполняет предварительное подключение к базе данных для проверки доступности,
     * а затем создаёт и добавляет источник данных в карту источников.
     * </p>
     *
     * @param host хост базы данных
     * @param port порт базы данных
     * @return объект {@link CommonResponse} с результатом операции
     */
    CommonResponse createDataSource(String host, Integer port);

    /**
     * Переключает текущее подключение на указанный источник данных.
     * <p>
     * Выполняет логирование времени переключения.
     * </p>
     *
     * @param name имя источника данных
     * @return объект {@link CommonResponse} с результатом операции
     */
    CommonResponse switchDataSource(String name);

    /**
     * Закрывает указанный источник данных.
     * <p>
     * Удаляет источник данных из карты и освобождает его ресурсы.
     * </p>
     *
     * @param name имя источника данных
     * @return объект {@link CommonResponse} с результатом операции
     */
    CommonResponse closeDataSource(String name);
}

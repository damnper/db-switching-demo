package com.example.dbswitchingdemo.config;

import com.example.dbswitchingdemo.dto.DataSourceContextDTO;

import java.util.Optional;

/**
 * Класс DataSourceContextHolder управляет текущим контекстом источника данных (DataSource) с использованием {@link ThreadLocal}.
 * Это позволяет динамически переключать источники данных в зависимости от текущего контекста потока.
 * В качестве контекста используется {@link DataSourceContextDTO}, который содержит информацию о ключе источника данных и имени базы данных.
 */
public class DataSourceContextHolder {

    private static DataSourceContextDTO contextHolder = null;

    /**
     * Устанавливает контекст источника данных для текущего потока.
     *
     * @param dataSourceContextDTO объект {@link DataSourceContextDTO}, содержащий ключ источника данных и имя базы данных
     */
    public static void setDataSourceContext(DataSourceContextDTO dataSourceContextDTO) {
        contextHolder = dataSourceContextDTO;
    }

    /**
     * Возвращает текущий контекст источника данных для текущего потока.
     *
     * @return {@link Optional}, содержащий {@link DataSourceContextDTO}, если контекст установлен, или {@code Optional.empty()}, если контекста нет
     */
    public static Optional<DataSourceContextDTO> getDataSourceContext() {
        return Optional.ofNullable(contextHolder);
    }


    /**
     * Очищает контекст источника данных для текущего потока.
     * Это полезно для предотвращения утечек данных и обеспечения корректного завершения работы с потоками.
     */
    public static void clearDataSourceContext() {
        contextHolder = null;
    }
}

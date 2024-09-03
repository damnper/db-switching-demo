package com.example.dbswitchingdemo.config;

/**
 * Класс DataSourceContextHolder управляет текущим контекстом источника данных (DataSource) с использованием {@link ThreadLocal}.
 * Это позволяет динамически переключать источники данных в зависимости от текущего контекста потока.
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    /**
     * Устанавливает текущий источник данных для текущего потока.
     *
     * @param dataSourceKey ключ источника данных, который будет использоваться
     */
    public static void setDataSource(String dataSourceKey) {
        contextHolder.set(dataSourceKey);
    }

    /**
     * Возвращает текущий источник данных для текущего потока.
     *
     * @return ключ текущего источника данных или {@code null}, если не установлен
     */
    public static String getDataSource() {
        return contextHolder.get();
    }
}

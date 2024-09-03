package com.example.dbswitchingdemo.config;

public class DataSourceContextHolder {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setDataSource(String dataSourceKey) {
        contextHolder.set(dataSourceKey);
    }

    public static String getDataSource() {
        return contextHolder.get();
    }
}

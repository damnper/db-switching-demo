package com.example.dbswitchingdemo.service;

import com.example.dbswitchingdemo.dto.response.CommonResponse;

public interface DataSourceService {

    CommonResponse createDataSource(String host, Integer port);

    CommonResponse switchDataSource(String name);

    CommonResponse closeDataSource(String name);
}

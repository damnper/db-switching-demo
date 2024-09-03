package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data-source/")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse> createDataSource(@RequestParam String host,
                                                           @RequestParam Integer port) {
        CommonResponse response = dataSourceService.createDataSource(host, port);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @DeleteMapping("/close")
    public ResponseEntity<CommonResponse> closeDataSource(@RequestParam String name) {
        CommonResponse response = dataSourceService.closeDataSource(name);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}

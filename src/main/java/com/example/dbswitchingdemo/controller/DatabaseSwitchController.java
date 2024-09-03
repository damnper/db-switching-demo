package com.example.dbswitchingdemo.controller;

import com.example.dbswitchingdemo.dto.response.CommonResponse;
import com.example.dbswitchingdemo.service.DataSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/db-switch")
@RequiredArgsConstructor
public class DatabaseSwitchController {

    private final DataSourceService dataSourceService;

    @PostMapping("/switch")
    public ResponseEntity<CommonResponse> switchDataSource(@RequestParam String name) {
        CommonResponse response = dataSourceService.switchDataSource(name);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}

package com.example.dbswitchingdemo.dto.response;

import lombok.experimental.SuperBuilder;

@SuperBuilder
public class CommonDataResponse extends CommonResponse {
    private Object data;
}

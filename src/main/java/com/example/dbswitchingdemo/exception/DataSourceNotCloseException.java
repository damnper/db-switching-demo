package com.example.dbswitchingdemo.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DataSourceNotCloseException extends RuntimeException {
    private final String message;
}

package com.example.dbswitchingdemo.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogSwitchFailedException extends RuntimeException {
    private final String message;
}

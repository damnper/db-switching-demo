package com.example.dbswitchingdemo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CommonResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String status;
    private String message;
}

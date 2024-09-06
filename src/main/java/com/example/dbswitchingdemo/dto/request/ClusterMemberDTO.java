package com.example.dbswitchingdemo.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ClusterMemberDTO {

    private List<MemberDTO> members;

    @Data
    public static class MemberDTO {
        private String name;
        private String role;
        private String state;
        private String api_url;
        private String host;
        private Integer port;
        private Integer timeline;
        private Integer lag; // может быть null для leader
    }
}

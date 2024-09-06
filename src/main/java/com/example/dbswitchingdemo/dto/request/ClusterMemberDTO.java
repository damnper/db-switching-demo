package com.example.dbswitchingdemo.dto.request;

import lombok.Data;

import java.util.List;

/**
 * DTO для представления членов кластера.
 * Содержит список объектов, представляющих каждого участника кластера.
 */
@Data
public class ClusterMemberDTO {

    /**
     * Список участников кластера.
     */
    private List<MemberDTO> members;

    /**
     * Внутренний класс, представляющий отдельного участника кластера.
     */
    @Data
    public static class MemberDTO {

        /**
         * Имя участника кластера.
         */
        private String name;

        /**
         * Роль участника в кластере (leader или replica).
         */
        private String role;

        /**
         * Текущее состояние участника (например, "running").
         */
        private String state;

        /**
         * URL API для доступа к участнику.
         */
        private String api_url;

        /**
         * Хост, на котором запущен участник.
         */
        private String host;

        /**
         * Порт, используемый участником.
         */
        private Integer port;

        /**
         * Текущая временная шкала участника.
         */
        private Integer timeline;

        /**
         * Задержка реплики в сравнении с лидером (может быть null для лидера).
         */
        private Integer lag;
    }
}

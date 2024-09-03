package com.example.dbswitchingdemo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <p> Класс DataSourceProperties содержит настройки для источника данных (DataSource),
 * которые загружаются из конфигурационного файла приложения с префиксом {@code spring.datasource}.</p>
 * <p>Этот класс управляет такими свойствами, как URL подключения, имя пользователя, пароль,
 * класс драйвера и имя базы данных. </p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String name;
}

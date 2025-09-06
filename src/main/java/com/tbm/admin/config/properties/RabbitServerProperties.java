package com.tbm.admin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitServerProperties {
    
    private String host;

    private int port;

    private String username;

    private String password;

}
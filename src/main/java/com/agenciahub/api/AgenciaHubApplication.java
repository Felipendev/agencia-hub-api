package com.agenciahub.api;

import com.agenciahub.api.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(RateLimitProperties.class)
public class AgenciaHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenciaHubApplication.class, args);
    }
}

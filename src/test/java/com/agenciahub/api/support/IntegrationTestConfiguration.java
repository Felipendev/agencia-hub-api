package com.agenciahub.api.support;

import com.agenciahub.api.application.integrations.email.EmailService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class IntegrationTestConfiguration {

    @Bean
    @Primary
    EmailService emailService() {
        return Mockito.mock(EmailService.class);
    }

    @Bean
    IntegrationHttpSupport integrationHttpSupport() {
        return new IntegrationHttpSupport();
    }
}

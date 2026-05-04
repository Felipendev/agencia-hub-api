package com.agenciahub.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AgenciaHubApplicationTests {

    @Test
    void contextLoads() {
        // smoke: Spring context + Flyway + JPA
    }
}

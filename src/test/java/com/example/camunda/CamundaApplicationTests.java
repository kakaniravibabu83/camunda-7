package com.example.camunda;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test verifying the full Spring application context — including the embedded
 * Camunda process engine, JPA/Hibernate, and all auto-configured beans — starts up
 * cleanly against the in-memory H2 test datasource.
 */
@SpringBootTest
class CamundaApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context (Camunda engine + JPA + web layer) fails to start,
        // this test fails automatically.
    }
}

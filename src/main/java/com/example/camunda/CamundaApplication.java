package com.example.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Spring Boot application that embeds the Camunda 7 process engine.
 * <p>
 * Camunda auto-configures itself via {@code camunda-bpm-spring-boot-starter-rest} and
 * {@code camunda-bpm-spring-boot-starter-webapp} found on the classpath, exposing:
 * <ul>
 *     <li>Camunda's own REST API at {@code /engine-rest/**}</li>
 *     <li>Cockpit / Tasklist / Admin web applications at {@code /camunda/app/**}</li>
 *     <li>Our custom, generic REST API at {@code /api/camunda/**} (see the {@code controller} package)</li>
 * </ul>
 */
@SpringBootApplication
public class CamundaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamundaApplication.class, args);
    }
}

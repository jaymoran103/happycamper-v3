package com.echo.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the HappyCamper web module.
 *
 * Exposes the core roster-processing pipeline (import → enhance → export → assert)
 * as a stateless HTTP service. See {@link ProcessController} for the public endpoint.
 */
@SpringBootApplication
public class HappyCamperWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(HappyCamperWebApplication.class, args);
    }
}

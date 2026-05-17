package com.echo.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.echo.assertion.AssertionRegistry;
import com.echo.assertion.AssertionService;
import com.echo.service.ExportService;
import com.echo.service.ImportService;
import com.echo.service.RosterService;

/**
 * Spring wiring for the core HappyCamper services.
 *
 * The core classes are framework-agnostic plain Java objects; this config exposes them
 * as singleton beans so the controller can {@code @Autowire} them. The default
 * {@link RosterService} constructor builds a feature registry from {@code CampConfig.defaults()},
 * which is the headless, web-safe configuration.
 */
@Configuration
public class CoreServicesConfig {

    @Bean
    public ImportService importService() {
        return new ImportService();
    }

    @Bean
    public ExportService exportService() {
        return new ExportService();
    }

    @Bean
    public RosterService rosterService(ImportService importService, ExportService exportService) {
        return new RosterService(importService, exportService);
    }

    @Bean
    public AssertionService assertionService() {
        return new AssertionService(AssertionRegistry.defaults());
    }
}

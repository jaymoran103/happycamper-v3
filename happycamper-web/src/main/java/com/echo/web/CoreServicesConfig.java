package com.echo.web;

import com.echo.domain.CampConfig;
import com.echo.feature.FeatureRegistry;
import com.echo.service.ExportService;
import com.echo.service.ImportService;
import com.echo.service.RosterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the framework-agnostic core services.
 *
 * Follows ADR-002: AssertionService is built internally by RosterService from
 * the injected FeatureRegistry — no separate AssertionService bean.
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
    public FeatureRegistry featureRegistry() {
        return FeatureRegistry.defaults(CampConfig.defaults());
    }

    @Bean
    public RosterService rosterService(ImportService importService,
                                       ExportService exportService,
                                       FeatureRegistry featureRegistry) {
        return new RosterService(importService, exportService, featureRegistry);
    }
}

package com.echo.web;

import com.echo.feature.FeatureRegistry;
import com.echo.service.ExportService;
import com.echo.service.ImportService;
import com.echo.service.RosterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoreServicesConfigIT {

    @Autowired ImportService importService;
    @Autowired ExportService exportService;
    @Autowired FeatureRegistry featureRegistry;
    @Autowired RosterService rosterService;

    @Test
    void contextLoadsAndCoreBeansAreWired() {
        assertThat(importService).isNotNull();
        assertThat(exportService).isNotNull();
        assertThat(featureRegistry).isNotNull();
        assertThat(rosterService).isNotNull();
    }
}

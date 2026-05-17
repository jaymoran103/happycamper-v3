package com.echo.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.echo.assertion.AssertionReport;
import com.echo.assertion.AssertionService;
import com.echo.domain.EnhancedRoster;
import com.echo.feature.FeatureRegistration;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;
import com.echo.service.ExportConfig;
import com.echo.service.ExportService;
import com.echo.service.RosterService;
import com.echo.web.dto.AssertionReportDto;
import com.echo.web.dto.ProcessResponse;

/**
 * Stateless endpoint that runs the full HappyCamper pipeline on two uploaded CSVs.
 *
 * Accepts a camper roster and an activity roster as multipart files plus an optional
 * list of feature toggles, runs the core enhancement pipeline, evaluates the assertion
 * registry, and returns both the enriched CSV and the assertion report as JSON.
 *
 * No persistence, no session state. Every request is independent.
 */
@RestController
public class ProcessController {

    private final RosterService rosterService;
    private final ExportService exportService;
    private final AssertionService assertionService;

    public ProcessController(RosterService rosterService,
                             ExportService exportService,
                             AssertionService assertionService) {
        this.rosterService = rosterService;
        this.exportService = exportService;
        this.assertionService = assertionService;
    }

    /**
     * Processes two uploaded CSVs and returns the enriched roster + assertion report.
     *
     * Feature toggles are optional; when omitted, every non-always-enabled feature in the
     * registry runs (ActivityFeature is always-enabled regardless of input).
     *
     * @param camperFile   the camper-roster CSV
     * @param activityFile the activity-roster CSV
     * @param features     optional list of feature IDs to enable; defaults to "all"
     */
    @PostMapping(path = "/process")
    public ResponseEntity<?> process(
            @RequestParam("camperFile") MultipartFile camperFile,
            @RequestParam("activityFile") MultipartFile activityFile,
            @RequestParam(value = "features", required = false) List<String> features) {

        if (camperFile == null || camperFile.isEmpty()) {
            return ResponseEntity.badRequest().body(error("camperFile is required"));
        }
        if (activityFile == null || activityFile.isEmpty()) {
            return ResponseEntity.badRequest().body(error("activityFile is required"));
        }

        List<String> enabledFeatureIds = resolveFeatureIds(features);

        EnhancedRoster enhancedRoster;
        WarningManager warningManager;
        try (InputStream camperStream = camperFile.getInputStream();
             InputStream activityStream = activityFile.getInputStream()) {
            enhancedRoster = rosterService.createEnhancedRoster(
                    camperStream, safeName(camperFile),
                    activityStream, safeName(activityFile),
                    enabledFeatureIds);
            warningManager = rosterService.getWarningManager();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("Could not read uploaded file: " + e.getMessage()));
        }

        if (enhancedRoster == null) {
            // The pipeline aborted (prerequisite feature failed or post-validation failed).
            // Surface the collected warnings/errors so the caller can diagnose.
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(error("Roster processing failed: " + summarizeErrors(warningManager)));
        }

        String enrichedCsv = exportCsv(enhancedRoster);
        AssertionReport report = assertionService.runAssertions(enhancedRoster);

        ProcessResponse response = new ProcessResponse(
                AssertionReportDto.from(report),
                enrichedCsv,
                flattenWarnings(warningManager));
        return ResponseEntity.ok(response);
    }

    /**
     * When the caller does not supply a {@code features} param, default to enabling every
     * registered feature. ActivityFeature is always-enabled regardless and is filtered out
     * of the optional set if absent — RosterService handles the bookkeeping.
     */
    private List<String> resolveFeatureIds(List<String> requested) {
        if (requested != null && !requested.isEmpty()) {
            return requested;
        }
        List<String> all = new ArrayList<>();
        for (FeatureRegistration reg : rosterService.getFeatureRegistry().all()) {
            all.add(reg.featureId());
        }
        return all;
    }

    private String exportCsv(EnhancedRoster enhancedRoster) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            exportService.exportRosterToCSV(enhancedRoster, null, ExportConfig.defaults(), buffer);
        } catch (Exception e) {
            throw new IllegalStateException("CSV export failed: " + e.getMessage(), e);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private List<String> flattenWarnings(WarningManager warningManager) {
        List<String> out = new ArrayList<>();
        if (warningManager == null) {
            return out;
        }
        warningManager.getWarningLog().forEach((type, list) -> {
            for (RosterWarning w : list) {
                String body = String.join(" | ", Arrays.asList(w.getDisplayData()));
                out.add(type.name() + ": " + body);
            }
        });
        return out;
    }

    private String summarizeErrors(WarningManager warningManager) {
        if (warningManager == null || !warningManager.hasErrors()) {
            return "no error details captured";
        }
        StringBuilder sb = new StringBuilder();
        warningManager.getErrorLog().forEach((type, list) -> {
            for (Exception ex : list) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(type.name()).append(" - ").append(ex.getMessage());
            }
        });
        return sb.toString();
    }

    private static String safeName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && !name.isBlank() ? name : "upload.csv";
    }

    private static java.util.Map<String, String> error(String message) {
        return java.util.Map.of("error", message);
    }
}

package com.echo.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.echo.assertion.AssertionReport;
import com.echo.domain.EnhancedRoster;
import com.echo.feature.FeatureRegistration;
import com.echo.logging.RosterWarning;
import com.echo.logging.WarningManager;
import com.echo.service.ExportConfig;
import com.echo.service.ExportService;
import com.echo.service.RosterService;
import com.echo.web.dto.AssertionReportDto;
import com.echo.web.dto.ProcessResponse;
import com.echo.web.dto.ProcessResponse.ErrorDto;
import com.echo.web.dto.ProcessResponse.WarningDto;

/**
 * Stateless endpoint that runs the full HappyCamper pipeline on two uploaded CSVs.
 *
 * Accepts a camper roster and an activity roster as multipart files plus an optional
 * list of feature toggles, runs the core enhancement pipeline, evaluates the assertion
 * registry, and returns the enriched CSV + assertion report as JSON.
 *
 * Response contract (success and error shapes, status code map): see
 * {@code docs/decisions/003-process-response-contract.md} (ADR-003).
 *
 * No persistence, no session state. Every request is independent.
 */
@RestController
public class ProcessController {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessController.class);

    private final RosterService rosterService;
    private final ExportService exportService;

    public ProcessController(RosterService rosterService, ExportService exportService) {
        this.rosterService = rosterService;
        this.exportService = exportService;
    }

    /**
     * Processes two uploaded CSVs and returns the enriched roster + assertion report.
     *
     * Feature toggles are optional; when omitted, every registered feature in the
     * registry runs. ActivityFeature is always-enabled regardless of the request.
     *
     * @param camperFile   the camper-roster CSV
     * @param activityFile the activity-roster CSV
     * @param features     optional list of feature IDs to enable; defaults to all registered
     */
    @PostMapping(path = "/process")
    public ResponseEntity<?> process(
            @RequestParam(value = "camperFile", required = false) MultipartFile camperFile,
            @RequestParam(value = "activityFile", required = false) MultipartFile activityFile,
            @RequestParam(value = "features", required = false) List<String> features) {

        long startNanos = System.nanoTime();
        LOG.info("POST /process camperFile={} ({} bytes) activityFile={} ({} bytes) features={}",
                safeName(camperFile), sizeOrZero(camperFile),
                safeName(activityFile), sizeOrZero(activityFile),
                features == null ? "<all>" : features);

        if (camperFile == null || camperFile.isEmpty()) {
            LOG.warn("POST /process 400 — camperFile is required ({}ms)", elapsedMs(startNanos));
            return badRequest("camperFile is required");
        }
        if (activityFile == null || activityFile.isEmpty()) {
            LOG.warn("POST /process 400 — activityFile is required ({}ms)", elapsedMs(startNanos));
            return badRequest("activityFile is required");
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
            LOG.warn("POST /process 400 — IO error reading upload ({}ms): {}", elapsedMs(startNanos), e.getMessage());
            return badRequest("Could not read uploaded file: " + e.getMessage());
        }

        if (enhancedRoster == null) {
            List<ErrorDto> errors = mapErrors(warningManager);
            LOG.warn("POST /process 422 — pipeline aborted with {} error(s) ({}ms)", errors.size(), elapsedMs(startNanos));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("errors", errors));
        }

        String enrichedCsv = exportCsv(enhancedRoster);
        AssertionReport report = rosterService.getAssertionReport();
        List<WarningDto> warnings = mapWarnings(warningManager);

        LOG.info("POST /process 200 — assertions total={} passed={} failed={} skipped={} warnings={} ({}ms)",
                report.totalCount(), report.passedCount(), report.failedCount(), report.skippedCount(),
                warnings.size(), elapsedMs(startNanos));

        ProcessResponse response = new ProcessResponse(
                AssertionReportDto.from(report),
                enrichedCsv,
                warnings);
        return ResponseEntity.ok(response);
    }


    /**
     * When the caller does not supply a {@code features} param, default to enabling every
     * registered feature ID. This is the maintainability lever — adding a feature in core
     * auto-includes it in {@code /process} with zero web changes.
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

    private List<WarningDto> mapWarnings(WarningManager warningManager) {
        List<WarningDto> out = new ArrayList<>();
        if (warningManager == null) {
            return out;
        }
        warningManager.getWarningLog().forEach((type, list) -> {
            for (RosterWarning w : list) {
                String message = String.join(" | ", Arrays.asList(w.getDisplayData()));
                out.add(new WarningDto(type.name(), message));
            }
        });
        return out;
    }

    private List<ErrorDto> mapErrors(WarningManager warningManager) {
        List<ErrorDto> out = new ArrayList<>();
        if (warningManager == null || !warningManager.hasErrors()) {
            return out;
        }
        warningManager.getErrorLog().forEach((type, list) -> {
            for (Exception ex : list) {
                out.add(new ErrorDto(type.name(), ex.getMessage()));
            }
        });
        return out;
    }

    private static String safeName(MultipartFile file) {
        if (file == null) return "<missing>";
        String name = file.getOriginalFilename();
        return name != null && !name.isBlank() ? name : "upload.csv";
    }

    private static long sizeOrZero(MultipartFile file) {
        return file == null ? 0L : file.getSize();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}

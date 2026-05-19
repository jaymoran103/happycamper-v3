package com.echo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.echo.assertion.AssertionReport;
import com.echo.assertion.AssertionResult;
import com.echo.domain.EnhancedRoster;
import com.echo.preset.ExpectedAssertion;
import com.echo.preset.ExpectedOutputs;
import com.echo.preset.Preset;
import com.echo.preset.PresetLoader;

/**
 * End-to-end harness: for every preset that declares an {@code expectedAssertions}
 * block, run the full enhancement pipeline and diff the {@link AssertionReport}
 * against the YAML expectations.
 *
 * <p>Adding a new regression fixture is YAML-only — drop a preset into
 * {@code happycamper-core/src/test/resources/presets/} with an
 * {@code expectedOutputs.expectedAssertions} block; this IT picks it up via
 * {@link PresetLoader#available()}.
 *
 * <p>Presets without {@code expectedAssertions} are skipped at parameter-source
 * time (no {@link org.junit.jupiter.api.Assumptions} noise in the report).
 *
 * <p>Failures dump the entire actual report so the fixture author can copy the
 * outcomes back into the YAML.
 */
class RosterServiceAssertionIT {

    static Stream<Preset> presetsWithExpectations() {
        List<Preset> out = new ArrayList<>();
        for (String name : PresetLoader.available()) {
            Preset preset = PresetLoader.load(name);
            ExpectedOutputs expected = preset.getExpectedOutputs();
            if (expected == null) continue;
            Map<String, ExpectedAssertion> assertions = expected.expectedAssertions();
            if (assertions == null || assertions.isEmpty()) continue;
            out.add(preset);
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("presetsWithExpectations")
    void presetAssertionsMatchExpectations(Preset preset) {
        ImportService importService = new ImportService();
        ExportService exportService = new ExportService();
        RosterService service = new RosterService(importService, exportService);

        EnhancedRoster roster = service.createEnhancedRoster(
                preset.getCamperFile(),
                preset.getActivityFile(),
                List.of(preset.getFeatures()));

        if (roster == null) {
            fail("[" + preset.getName() + "] createEnhancedRoster returned null; warnings: "
                    + service.getWarningManager().getWarningLog());
        }

        AssertionReport report = service.getAssertionReport();
        Map<String, ExpectedAssertion> expected = preset.getExpectedOutputs().expectedAssertions();

        // Build an id -> actual map for easy diffing.
        Map<String, AssertionResult> actualById = new LinkedHashMap<>();
        for (AssertionResult result : report.getResults()) {
            actualById.put(result.getAssertionId(), result);
        }

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, ExpectedAssertion> entry : expected.entrySet()) {
            String id = entry.getKey();
            ExpectedAssertion exp = entry.getValue();
            AssertionResult actual = actualById.get(id);
            if (actual == null) {
                mismatches.add(id + ": expected " + exp.status() + " but no result was produced");
                continue;
            }
            if (actual.getStatus() != exp.status()) {
                mismatches.add(id + ": expected " + exp.status() + " but was " + actual.getStatus()
                        + " (summary: " + actual.getSummary() + ")");
                continue;
            }
            if (exp.failureCount() != null && exp.failureCount() != actual.getFailureCount()) {
                mismatches.add(id + ": expected failureCount=" + exp.failureCount()
                        + " but was " + actual.getFailureCount());
            }
        }

        // Surface unexpected extra results too — a YAML that omits an assertion id is
        // declaring "I don't care about this one." But during fixture authoring it's
        // useful to see everything the report produced.
        if (!mismatches.isEmpty()) {
            String presetPath = "happycamper-core/src/test/resources/presets/" + preset.getName() + ".yaml";
            fail("[" + preset.getName() + "] expectation mismatch (" + presetPath + "):\n"
                    + String.join("\n", mismatches)
                    + "\n\nFull report:\n" + dump(report));
        }

        // Sanity: the report should cover every assertion the user pinned (no typos in ids).
        assertEquals(Collections.emptyList(),
                expected.keySet().stream().filter(id -> !actualById.containsKey(id)).toList(),
                "Preset references assertion ids that the service did not produce");
    }

    private static String dump(AssertionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("total=").append(report.totalCount())
          .append(" passed=").append(report.passedCount())
          .append(" failed=").append(report.failedCount())
          .append(" skipped=").append(report.skippedCount())
          .append('\n');
        for (AssertionResult r : report.getResults()) {
            sb.append("  ").append(r.getAssertionId())
              .append(": status=").append(r.getStatus())
              .append(" failureCount=").append(r.getFailureCount())
              .append(" summary=").append(r.getSummary())
              .append('\n');
        }
        return sb.toString();
    }
}

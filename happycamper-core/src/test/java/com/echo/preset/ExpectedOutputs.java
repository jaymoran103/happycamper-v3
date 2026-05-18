package com.echo.preset;

import java.util.List;
import java.util.Map;

/**
 * Optional assertion targets attached to a {@link Preset}. All fields nullable so
 * Phase 1 presets can omit the block entirely; Phase 3 assertion-driven presets
 * will populate them.
 */
public record ExpectedOutputs(
        Integer expectedCamperCount,
        Integer expectedWarningCount,
        List<String> expectedAssertionStatuses,
        Map<String, Integer> expectedHeaderCounts
) {
}

package com.echo.preset;

import java.util.Map;

/**
 * Optional expectations attached to a {@link Preset}. All fields nullable so presets
 * can declare only what they care about; the {@code RosterServiceAssertionIT} ignores
 * presets whose {@link #expectedAssertions} map is null or empty.
 *
 * <p>{@code expectedAssertions} keys are assertion ids (e.g., {@code "no_swim_conflicts"})
 * and values pin status + optional failure count. See {@link ExpectedAssertion}.
 */
public record ExpectedOutputs(
        Integer expectedCamperCount,
        Integer expectedWarningCount,
        Map<String, ExpectedAssertion> expectedAssertions,
        Map<String, Integer> expectedHeaderCounts
) {
}

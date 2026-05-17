package com.echo.web.dto;

import java.util.List;

/**
 * Combined response body for {@code POST /process}.
 *
 * Carries the assertion report and the enriched-roster CSV as a single JSON payload
 * so a curl client can pipe through {@code jq -r .enrichedCsv > out.csv} and a UI
 * client can render assertions inline while offering the CSV as a download.
 *
 * Stateless: no IDs, no resource handles. The caller already has everything they need.
 *
 * @param assertions     the assertion report (summary + per-assertion results)
 * @param enrichedCsv    the enriched roster as CSV text
 * @param warnings       non-fatal warnings collected during import/enhancement
 */
public record ProcessResponse(
        AssertionReportDto assertions,
        String enrichedCsv,
        List<String> warnings
) {}

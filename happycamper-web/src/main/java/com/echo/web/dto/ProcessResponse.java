package com.echo.web.dto;

import java.util.List;

/**
 * Successful (200 OK) response body for {@code POST /process}.
 *
 * Three top-level fields, stateless. Contract is locked in
 * {@code docs/decisions/003-process-response-contract.md} (ADR-003).
 *
 * @param assertions  the assertion report (summary + per-assertion results)
 * @param enrichedCsv the enriched roster as CSV text
 * @param warnings    non-fatal warnings collected during import/enhancement, structured as {type, message}
 */
public record ProcessResponse(
        AssertionReportDto assertions,
        String enrichedCsv,
        List<WarningDto> warnings
) {
    /**
     * Structured view of a {@code com.echo.logging.RosterWarning}. The {@code type} field is
     * the {@code WarningType} enum name (e.g. {@code BAD_DATA_FORMAT}) — enum names are part
     * of the API contract per ADR-003. The {@code message} is a pre-rendered, opaque string
     * (today: {@code RosterWarning.getDisplayData()} joined by {@code " | "}); clients render
     * it as-is and never parse it.
     */
    public record WarningDto(String type, String message) {}

    /**
     * Structured view of a single error captured on {@code WarningManager} when the pipeline
     * aborts. Carried in the 422 abort response body's {@code errors} array (not in this
     * 200-OK response). {@code type} is the {@code ErrorType} enum name; {@code message} is
     * the exception message.
     */
    public record ErrorDto(String type, String message) {}
}

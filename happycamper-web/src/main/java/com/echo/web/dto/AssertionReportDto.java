package com.echo.web.dto;

import java.util.List;

import com.echo.assertion.AssertionReport;

/**
 * JSON-serializable view of an {@link AssertionReport}, including a summary block
 * for clients that only want top-level pass/fail counts.
 */
public record AssertionReportDto(
        Summary summary,
        List<AssertionResultDto> results
) {
    public record Summary(int total, int passed, int failed, int skipped, boolean allPassed) {}

    public static AssertionReportDto from(AssertionReport report) {
        Summary summary = new Summary(
                report.totalCount(),
                report.passedCount(),
                report.failedCount(),
                report.skippedCount(),
                report.allPassed());
        List<AssertionResultDto> results = report.getResults().stream()
                .map(AssertionResultDto::from)
                .toList();
        return new AssertionReportDto(summary, results);
    }
}

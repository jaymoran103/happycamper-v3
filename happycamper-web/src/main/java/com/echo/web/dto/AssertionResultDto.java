package com.echo.web.dto;

import java.util.List;

import com.echo.assertion.AssertionResult;

/**
 * JSON-serializable view of a single {@link AssertionResult}.
 *
 * Mirrors the core type field-for-field so the API can evolve independently of the
 * core class's internal representation.
 */
public record AssertionResultDto(
        String id,
        String name,
        String status,
        String summary,
        int checkedCount,
        int failureCount,
        List<String> failureDetails
) {
    public static AssertionResultDto from(AssertionResult result) {
        return new AssertionResultDto(
                result.getAssertionId(),
                result.getAssertionName(),
                result.getStatus().name(),
                result.getSummary(),
                result.getCheckedCount(),
                result.getFailureCount(),
                result.getFailureDetails());
    }
}

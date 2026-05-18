package com.echo.assertion;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of evaluating a single {@link RosterAssertion}.
 *
 * Immutable value object carrying the assertion's identity, pass/fail status, a short
 * summary suitable for display, and a bounded list of failure details. Counts are
 * informational (e.g., "checked 50 campers, 3 failed") and may be zero when the
 * concept does not apply.
 *
 * Use the static factories ({@link #passed}, {@link #failed}, {@link #skipped}) to
 * construct instances rather than instantiating directly.
 */
public final class AssertionResult {

    public enum Status { PASSED, FAILED, SKIPPED }

    /**
     * Cap on the number of failure detail strings retained per result. Prevents a
     * pathological roster from producing megabytes of report text. Excess failures
     * are still counted in {@link #getFailureCount()}.
     */
    public static final int MAX_FAILURE_DETAILS = 50;

    private final String assertionId;
    private final String assertionName;
    private final Status status;
    private final String summary;
    private final int checkedCount;
    private final int failureCount;
    private final List<String> failureDetails;

    private AssertionResult(String assertionId, String assertionName, Status status, String summary,
                            int checkedCount, int failureCount, List<String> failureDetails) {
        this.assertionId = assertionId;
        this.assertionName = assertionName;
        this.status = status;
        this.summary = summary;
        this.checkedCount = checkedCount;
        this.failureCount = failureCount;
        this.failureDetails = Collections.unmodifiableList(failureDetails);
    }

    public static AssertionResult passed(String assertionId, String assertionName, int checkedCount, String summary) {
        return new AssertionResult(assertionId, assertionName, Status.PASSED, summary,
                checkedCount, 0, List.of());
    }

    public static AssertionResult failed(String assertionId, String assertionName, int checkedCount,
                                         List<String> failureDetails, String summary) {
        int totalFailures = failureDetails.size();
        List<String> bounded = failureDetails.size() > MAX_FAILURE_DETAILS
                ? List.copyOf(failureDetails.subList(0, MAX_FAILURE_DETAILS))
                : List.copyOf(failureDetails);
        return new AssertionResult(assertionId, assertionName, Status.FAILED, summary,
                checkedCount, totalFailures, bounded);
    }

    public static AssertionResult skipped(String assertionId, String assertionName, String reason) {
        return new AssertionResult(assertionId, assertionName, Status.SKIPPED, reason,
                0, 0, List.of());
    }

    public String getAssertionId() { return assertionId; }
    public String getAssertionName() { return assertionName; }
    public Status getStatus() { return status; }
    public String getSummary() { return summary; }
    public int getCheckedCount() { return checkedCount; }
    public int getFailureCount() { return failureCount; }
    public List<String> getFailureDetails() { return failureDetails; }

    public boolean isPassed() { return status == Status.PASSED; }
    public boolean isFailed() { return status == Status.FAILED; }
    public boolean isSkipped() { return status == Status.SKIPPED; }
}

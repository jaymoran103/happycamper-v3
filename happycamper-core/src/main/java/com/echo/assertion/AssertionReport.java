package com.echo.assertion;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated outcome of evaluating every feature's {@link RosterAssertion}s against
 * an {@link com.echo.domain.EnhancedRoster}.
 *
 * Immutable summary used by callers (desktop UI, web response) to display pass/fail
 * status across all known assertions in one place. Skipped results carry assertions
 * whose owning feature was not enabled during the run.
 */
public final class AssertionReport {

    private final List<AssertionResult> results;

    public AssertionReport(List<AssertionResult> results) {
        this.results = Collections.unmodifiableList(List.copyOf(results));
    }

    public List<AssertionResult> getResults() { return results; }

    public int totalCount() { return results.size(); }

    public int passedCount() {
        return (int) results.stream().filter(AssertionResult::isPassed).count();
    }

    public int failedCount() {
        return (int) results.stream().filter(AssertionResult::isFailed).count();
    }

    public int skippedCount() {
        return (int) results.stream().filter(AssertionResult::isSkipped).count();
    }

    /** @return true if no assertion failed (skipped assertions do not count against this). */
    public boolean allPassed() {
        return failedCount() == 0;
    }
}

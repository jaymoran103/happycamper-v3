package com.echo.assertion;

import com.echo.domain.EnhancedRoster;

/**
 * Read-only check that inspects a completed {@link EnhancedRoster} and reports whether
 * the data satisfies an invariant (e.g., no swim conflicts, no unrequested activities).
 *
 * Assertions never mutate the roster. They run after all features have been applied,
 * so they may rely on columns added by those features.
 *
 * Each assertion is owned by exactly one {@link com.echo.feature.RosterFeature}, which
 * exposes it via {@code RosterFeature.getAssertions()}. Feature enablement gates whether
 * an assertion is evaluated against the roster: if the owning feature is enabled, the
 * assertion runs; if not, the service records a SKIPPED result without invoking the
 * assertion. Authors may therefore assume the owning feature's columns are present.
 *
 * If an assertion throws while evaluating, the service captures the throwable and
 * surfaces it as a FAILED result rather than aborting the whole report.
 */
public interface RosterAssertion {

    /** @return stable, unique identifier (e.g., "no_swim_conflicts"). */
    String getAssertionId();

    /** @return short human-readable name for display in reports. */
    String getAssertionName();

    /** @return one-line description of what this assertion verifies. */
    String getDescription();

    /**
     * Evaluate the assertion against the supplied roster.
     *
     * @param roster the roster to evaluate
     * @return the evaluation result; never {@code null}
     */
    AssertionResult evaluate(EnhancedRoster roster);
}

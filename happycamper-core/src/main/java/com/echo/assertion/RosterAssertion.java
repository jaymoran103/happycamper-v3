package com.echo.assertion;

import com.echo.domain.EnhancedRoster;

/**
 * Read-only check that inspects a completed {@link EnhancedRoster} and reports whether
 * the data satisfies an invariant (e.g., no swim conflicts, no unrequested activities).
 *
 * Assertions never mutate the roster. They run after all features have been applied,
 * so they may rely on columns added by those features. Each assertion declares whether
 * it applies to the supplied roster via {@link #isApplicable(EnhancedRoster)}; the
 * {@link AssertionService} skips inapplicable assertions and records them as
 * {@link AssertionResult.Status#SKIPPED}.
 *
 * Applicability typically mirrors feature gating — an assertion that inspects a column
 * added by SwimLevelFeature should require {@code roster.hasFeature("swimlevel")}.
 */
public interface RosterAssertion {

    /** @return stable, unique identifier (e.g., "no_swim_conflicts"). */
    String getAssertionId();

    /** @return short human-readable name for display in reports. */
    String getAssertionName();

    /** @return one-line description of what this assertion verifies. */
    String getDescription();

    /**
     * @param roster the roster to evaluate
     * @return true if the assertion can be meaningfully evaluated against this roster
     *         (e.g., the prerequisite feature has been applied)
     */
    boolean isApplicable(EnhancedRoster roster);

    /**
     * Evaluate the assertion against the supplied roster.
     *
     * Callers must check {@link #isApplicable(EnhancedRoster)} first. Implementations
     * may assume applicability and may produce undefined results when invoked on a roster
     * for which they are not applicable.
     *
     * @param roster the roster to evaluate
     * @return the evaluation result; never null
     */
    AssertionResult evaluate(EnhancedRoster roster);
}

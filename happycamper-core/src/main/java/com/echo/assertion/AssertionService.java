package com.echo.assertion;

import java.util.ArrayList;
import java.util.List;

import com.echo.domain.EnhancedRoster;

/**
 * Evaluates every {@link RosterAssertion} in an {@link AssertionRegistry} against a roster
 * and returns an {@link AssertionReport}.
 *
 * Assertions that report inapplicable via {@link RosterAssertion#isApplicable(EnhancedRoster)}
 * are recorded as {@link AssertionResult.Status#SKIPPED} rather than evaluated. An assertion
 * that throws an unexpected exception is captured as a FAILED result so a single broken
 * assertion does not abort the whole report.
 */
public class AssertionService {

    private final AssertionRegistry registry;

    public AssertionService(AssertionRegistry registry) {
        this.registry = registry;
    }

    public AssertionReport runAssertions(EnhancedRoster roster) {
        List<AssertionResult> results = new ArrayList<>();
        for (AssertionRegistration registration : registry.all()) {
            RosterAssertion assertion = registration.assertion();
            results.add(evaluateSafely(assertion, roster));
        }
        return new AssertionReport(results);
    }

    private AssertionResult evaluateSafely(RosterAssertion assertion, EnhancedRoster roster) {
        try {
            if (!assertion.isApplicable(roster)) {
                return AssertionResult.skipped(
                        assertion.getAssertionId(),
                        assertion.getAssertionName(),
                        "Prerequisite feature data not present.");
            }
            return assertion.evaluate(roster);
        } catch (Exception e) {
            return AssertionResult.failed(
                    assertion.getAssertionId(),
                    assertion.getAssertionName(),
                    0,
                    List.of("Assertion threw " + e.getClass().getSimpleName() + ": " + e.getMessage()),
                    "Assertion failed to evaluate due to an unexpected error.");
        }
    }
}

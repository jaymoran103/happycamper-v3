package com.echo.assertion;

/**
 * Wrapper around a registered {@link RosterAssertion}.
 *
 * Mirrors {@code FeatureRegistration} but is simpler: assertions have no paired filter
 * and no always-enabled flag. All registered assertions are eligible to run; the
 * assertion's own {@link RosterAssertion#isApplicable(com.echo.domain.EnhancedRoster)}
 * decides whether it actually evaluates.
 */
public record AssertionRegistration(RosterAssertion assertion) {
    public String assertionId() {
        return assertion.getAssertionId();
    }
}

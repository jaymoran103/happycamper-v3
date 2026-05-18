package com.echo.assertion;

import java.util.ArrayList;
import java.util.List;

import com.echo.domain.EnhancedRoster;
import com.echo.feature.FeatureRegistration;
import com.echo.feature.FeatureRegistry;
import com.echo.feature.RosterFeature;

/**
 * Evaluates the assertions owned by every {@link RosterFeature} in a
 * {@link FeatureRegistry} against an {@link EnhancedRoster} and returns an
 * {@link AssertionReport}.
 *
 * The service walks the registry in declaration order. For each feature, it asks
 * {@link RosterFeature#getAssertions()} for its checks; an assertion whose owning
 * feature is enabled in the roster is evaluated, and one whose feature is not
 * enabled is recorded as {@link AssertionResult.Status#SKIPPED}. An assertion that
 * throws while evaluating is captured as {@link AssertionResult.Status#FAILED} so
 * a single broken check does not abort the whole report.
 *
 * Assertions never mutate the roster. The service exposes no setters; the registry
 * is supplied once at construction time.
 */
public class AssertionService {

    private static final String SKIPPED_REASON = "Feature not enabled.";

    private final FeatureRegistry featureRegistry;

    public AssertionService(FeatureRegistry featureRegistry) {
        this.featureRegistry = featureRegistry;
    }

    public AssertionReport runAssertions(EnhancedRoster roster) {
        List<AssertionResult> results = new ArrayList<>();
        for (FeatureRegistration registration : featureRegistry.all()) {
            RosterFeature feature = registration.feature();
            boolean enabled = roster.hasFeature(feature.getFeatureId());
            for (RosterAssertion assertion : feature.getAssertions()) {
                if (enabled) {
                    results.add(evaluateSafely(assertion, roster));
                } else {
                    results.add(AssertionResult.skipped(
                            assertion.getAssertionId(),
                            assertion.getAssertionName(),
                            SKIPPED_REASON));
                }
            }
        }
        return new AssertionReport(results);
    }

    private AssertionResult evaluateSafely(RosterAssertion assertion, EnhancedRoster roster) {
        try {
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

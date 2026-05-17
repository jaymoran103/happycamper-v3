package com.echo.assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.echo.assertion.checks.AllCampersHaveMaxRoundsAssertion;
import com.echo.assertion.checks.NoDuplicateRoundEntriesAssertion;
import com.echo.assertion.checks.NoSwimConflictsAssertion;
import com.echo.assertion.checks.NoUnrequestedActivitiesAssertion;
import com.echo.assertion.checks.RoundCountsNumericAssertion;

/**
 * Single source of truth for which {@link RosterAssertion}s exist and in what order they run.
 *
 * Mirrors the design of {@code FeatureRegistry}: the {@link #defaults()} factory builds the
 * canonical core set; consumers (desktop, web) call {@link AssertionService} with this registry.
 * No filter pairing and no always-enabled flag — assertions run when applicable, skip otherwise.
 */
public class AssertionRegistry {

    private final List<AssertionRegistration> registrations = new ArrayList<>();

    public void register(AssertionRegistration registration) {
        registrations.add(registration);
    }

    public List<AssertionRegistration> all() {
        return Collections.unmodifiableList(registrations);
    }

    public Optional<AssertionRegistration> find(String assertionId) {
        for (AssertionRegistration r : registrations) {
            if (r.assertionId().equals(assertionId)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Builds the default registry containing the core assertion set.
     */
    public static AssertionRegistry defaults() {
        AssertionRegistry registry = new AssertionRegistry();
        registry.register(new AssertionRegistration(new AllCampersHaveMaxRoundsAssertion()));
        registry.register(new AssertionRegistration(new RoundCountsNumericAssertion()));
        registry.register(new AssertionRegistration(new NoDuplicateRoundEntriesAssertion()));
        registry.register(new AssertionRegistration(new NoUnrequestedActivitiesAssertion()));
        registry.register(new AssertionRegistration(new NoSwimConflictsAssertion()));
        return registry;
    }
}

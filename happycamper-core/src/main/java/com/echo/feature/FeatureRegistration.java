package com.echo.feature;

import java.util.function.Supplier;

import com.echo.filter.RosterFilter;

/**
 * Declarative pairing of a {@link RosterFeature} with its companion {@link RosterFilter}.
 *
 * The filter factory may be {@code null} for features that have no UI filter. The factory
 * produces a fresh filter instance for each roster so callers do not share filter state.
 *
 * @param feature       the feature instance
 * @param filterFactory factory that builds the paired filter, or {@code null} if none
 * @param alwaysEnabled if {@code true}, the feature runs on every import regardless of UI toggles
 */
public record FeatureRegistration(
    RosterFeature feature,
    Supplier<RosterFilter> filterFactory,
    boolean alwaysEnabled
) {
    public String featureId() {
        return feature.getFeatureId();
    }
}

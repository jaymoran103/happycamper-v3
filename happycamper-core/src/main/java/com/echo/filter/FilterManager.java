package com.echo.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.feature.FeatureRegistration;
import com.echo.feature.FeatureRegistry;

/**
 * Manager for roster filters.
 * Handles applying multiple filters to campers.
 */
public class FilterManager {
    private final Map<String, RosterFilter> filters = new HashMap<>();
    private EnhancedRoster roster;

    /**
     * Adds a filter to the manager.
     *
     * @param filter The filter to add
     */
    public void addFilter(RosterFilter filter) {
        filters.put(filter.getFilterId(), filter);
    }

    /**
     * Removes a filter from the manager.
     *
     * @param filterId The ID of the filter to remove
     */
    public void removeFilter(String filterId) {
        filters.remove(filterId);
    }

    /**
     * Gets a filter by ID.
     *
     * @param filterId The ID of the filter to get
     * @return The filter, or null if not found
     */
    public RosterFilter getFilter(String filterId) {
        return filters.get(filterId);
    }

    /**
     * Applies all filters to a camper.
     *
     * @param camper The camper to filter
     * @return true if the camper passes all filters, false otherwise
     */
    public boolean applyFilters(Camper camper) {
        // If there are no filters, always show the camper
        if (filters.isEmpty()) {
            return true;
        }

        // A camper passes if it passes ALL filters
        for (RosterFilter filter : filters.values()) {
            boolean passes = filter.apply(camper);
            if (!passes) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a filter is registered.
     *
     * @param filterId The ID of the filter to check
     * @return true if the filter is registered
     */
    public boolean hasFilter(String filterId) {
        return filters.containsKey(filterId);
    }

    /**
     * Gets the number of registered filters.
     *
     * @return The number of filters
     */
    public int getFilterCount() {
        return filters.size();
    }

    /**
     * Gets all registered filters.
     *
     * @return Collection of all filters
     */
    public Collection<RosterFilter> getAllFilters() {
        return filters.values();
    }

    /**
     * Sets the roster for this filter manager.
     *
     * @param roster The roster to filter
     */
    public void setRoster(EnhancedRoster roster) {
        this.roster = roster;
    }

    /**
     * Gets the roster for this filter manager.
     *
     * @return The roster
     */
    public EnhancedRoster getRoster() {
        return roster;
    }

    /**
     * Creates filters for enabled features in the roster, driven by the {@link FeatureRegistry}.
     * Each registration with a non-null filter factory contributes a filter when its feature
     * is either always-enabled or present on the roster.
     *
     * @param roster   the roster to create filters for
     * @param registry the registry describing feature/filter pairings
     */
    public void createFiltersForRoster(EnhancedRoster roster, FeatureRegistry registry) {
        setRoster(roster);
        filters.clear();

        for (FeatureRegistration registration : registry.all()) {
            if (registration.filterFactory() == null) {
                continue;
            }
            boolean enabled = registration.alwaysEnabled() || roster.hasFeature(registration.featureId());
            if (enabled) {
                addFilter(registration.filterFactory().get());
            }
        }
    }
}

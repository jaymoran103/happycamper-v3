package com.echo.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;

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
        //System.out.println("FilterManager.addFilter: Adding filter " + filter.getFilterId() + " (" + filter.getFilterName() + ")");
        filters.put(filter.getFilterId(), filter);
        //System.out.println("FilterManager.addFilter: Filter count is now " + filters.size());
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
        //System.out.println("FilterManager.applyFilters: Applying " + filters.size() + " filters to camper " + camper.getId());

        // If there are no filters, always show the camper
        if (filters.isEmpty()) {
            //System.out.println("FilterManager.applyFilters: No filters, showing camper");
            return true;
        }

        // A camper passes if it passes ALL filters
        for (RosterFilter filter : filters.values()) {
            //System.out.println("FilterManager.applyFilters: Applying filter " + filter.getFilterId());
            boolean passes = filter.apply(camper);
            //System.out.println("FilterManager.applyFilters: Filter " + filter.getFilterId() + " returned " + passes);
            if (!passes) {
                //System.out.println("FilterManager.applyFilters: Camper " + camper.getId() + " failed filter " + filter.getFilterId());
                return false;
            }
        }

        //System.out.println("FilterManager.applyFilters: Camper " + camper.getId() + " passed all filters");
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
     * Creates filters for enabled features in the roster.
     *
     * @param roster The roster to create filters for
     */
    public void createFiltersForRoster(EnhancedRoster roster) {
        //System.out.println("FilterManager.createFiltersForRoster: Creating filters for roster");
        setRoster(roster);
        filters.clear();

        // Create filters based on enabled features

        // Always add the assignment filter to show basic round counts
        addFilter(new AssignmentFilter());

        // NOTE: Phase 1: SortedProgramFilter is Swing-coupled and lives in happycamper-desktop.
        // Desktop callers must add it manually after this method (see MainWindow.setRoster).
        // FUTURE: Phase 2 will introduce a proper filter-registration hook to remove this note.
        // addFilter(new CamperRoundsFilter()); Deprecated alternative, keeping in case its helpful for assertion-based validation feature
        if (roster.hasFeature("preference")) {
            addFilter(new PreferenceFilter());
        }
        if (roster.hasFeature("swimlevel")) {
            addFilter(new SwimLevelFilter());
        }
        if (roster.hasFeature("medical")) {
            addFilter(new MedicalFilter());
        }


    }
}

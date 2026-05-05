package com.echo.filter;

import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;

/**
 * Interface for roster filters.
 * Defines methods that all filters must implement.
 */
public interface RosterFilter {
    /**
     * Applies the filter to a camper.
     *
     * @param camper The camper to filter
     * @return true if the camper passes the filter, false otherwise
     */
    boolean apply(Camper camper);

    /**
     * Gets the unique identifier for this filter.
     *
     * @return The filter ID
     */
    String getFilterId();

    /**
     * Gets the display name for this filter.
     *
     * @return The filter display name
     */
    String getFilterName();

    /**
     * Returns a descriptor carrying the data the UI layer needs to build a filter panel for this filter.
     * Default implementation delegates to the no-argument variant.
     * Implementations that require an EnhancedRoster should set it as a field.
     *
     * @param roster The roster (may be used by some filters)
     * @return A {@link FilterPanelDescriptor} describing this filter's panel
     */
    default FilterPanelDescriptor getFilterPanelDescriptor(EnhancedRoster roster) {
        return getFilterPanelDescriptor();
    }

    /**
     * Returns a descriptor carrying the data the UI layer needs to build a filter panel for this filter.
     *
     * @return A {@link FilterPanelDescriptor} describing this filter's panel
     */
    FilterPanelDescriptor getFilterPanelDescriptor();
}

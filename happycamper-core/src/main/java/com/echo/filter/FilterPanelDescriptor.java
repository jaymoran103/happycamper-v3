package com.echo.filter;

import com.echo.filter.option.FilterOption;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Data transfer object to provide the UI layer with the information it needs 
 * to build a filter panel for a {@link RosterFilter}.
 *
 * <p>Core filter implementations construct and return this descriptor from
 * {@link RosterFilter#getFilterPanelDescriptor()}. The desktop module
 * ({@code FilterPanelFactory}) consumes the descriptor to create the actual
 * Swing component without coupling core logic to the UI toolkit.
 */
public class FilterPanelDescriptor {

    private final String title;
    private final Map<? extends FilterOption, Boolean> optionStates;
    private final BiConsumer<FilterOption, Boolean> callback;

    /**
     * @param title        Display title for the collapsible filter panel.
     * @param optionStates Snapshot of each option and its current visibility state.
     * @param callback     Called by the UI when the user toggles an option; receives
     *                     the toggled {@link FilterOption} and the new boolean state.
     */
    public FilterPanelDescriptor(
            String title,
            Map<? extends FilterOption, Boolean> optionStates,
            BiConsumer<FilterOption, Boolean> callback) {
        
        this.title = title;
        this.optionStates = optionStates;
        this.callback = callback;
    }

    public String getTitle() {
        return title;
    }

    public Map<? extends FilterOption, Boolean> getOptionStates() {
        return optionStates;
    }

    public BiConsumer<FilterOption, Boolean> getCallback() {
        return callback;
    }
}

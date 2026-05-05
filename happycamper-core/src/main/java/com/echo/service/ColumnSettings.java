package com.echo.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings for column visibility and sizing.
 * This class encapsulates all settings related to how columns are displayed in tables.
 */
public class ColumnSettings {

    private Map<String, Boolean> columnVisibility = new LinkedHashMap<>();
    /** Name of the sizing mode. Stored as a String to avoid a dependency on the desktop ColumnSizingOption enum. */
    private String sizingMode = "AUTO_SIZE";
    private int customWidth = 100; // Default custom width in pixels

    /**
     * Creates a new ColumnSettings instance with default values.
     */
    public ColumnSettings() {
        // Default constructor
    }

    /**
     * Gets the column visibility map.
     *
     * @return A map of column names to visibility status
     */
    public Map<String, Boolean> getColumnVisibility() {
        return new LinkedHashMap<>(columnVisibility); // Return a copy to prevent modification
    }

    /**
     * Sets the column visibility map.
     *
     * @param columnVisibility A map of column names to visibility status
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings setColumnVisibility(Map<String, Boolean> columnVisibility) {
        this.columnVisibility = new LinkedHashMap<>(columnVisibility); // Store a copy to prevent modification
        return this;
    }

    /**
     * Gets the column sizing option.
     *
     * @return The current column sizing option
     */
    public String getSizingMode() {
        return sizingMode;
    }

    /**
     * Sets the column sizing mode by name (matches the name of the desktop ColumnSizingOption enum constant).
     *
     * @param sizingMode The enum constant name, e.g. "AUTO_SIZE", "EQUAL_WIDTH", "CUSTOM_WIDTH"
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings setSizingMode(String sizingMode) {
        this.sizingMode = sizingMode;
        return this;
    }

    /**
     * Gets the custom column width.
     *
     * @return The custom column width in pixels
     */
    public int getCustomWidth() {
        return customWidth;
    }

    /**
     * Sets the custom column width.
     *
     * @param customWidth The new custom column width in pixels
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings setCustomWidth(int customWidth) {
        this.customWidth = customWidth;
        return this;
    }

    /**
     * Applies these column settings to a roster.
     *
     * @param roster The roster to apply the settings to
     */
    public void applyToRoster(com.echo.domain.EnhancedRoster roster) {
        // Apply column visibility settings
        for (Map.Entry<String, Boolean> entry : columnVisibility.entrySet()) {
            roster.setHeaderVisibility(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Updates this settings object from a roster's current state.
     *
     * @param roster The roster to get settings from
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings updateFromRoster(com.echo.domain.EnhancedRoster roster) {
        // Update column visibility from roster
        Map<String, Boolean> newVisibility = new LinkedHashMap<>();
        for (String header : roster.getAllHeaders()) {
            newVisibility.put(header, roster.isHeaderVisible(header));
        }
        this.columnVisibility = newVisibility;
        return this;
    }

    /**
     * Clears all column visibility settings.
     * This should be called when a new roster is loaded to ensure stale settings are not applied.
     *
     * @return This ColumnSettings instance for method chaining
     */
    public ColumnSettings clearColumnVisibility() {
        this.columnVisibility.clear();
        return this;
    }
}

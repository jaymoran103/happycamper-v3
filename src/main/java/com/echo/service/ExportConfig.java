package com.echo.service;

/**
 * Core-safe configuration object for export operations.
 * 
 * Contains just the three boolean options controlling export behavior, 
 * with no view-specific options or java.io.File references.
 * 
 * This allows it to be used in the core module and shared by both desktop and web layers.
 *
 * The desktop layer uses ExportSettings, which wraps an ExportConfig and additionally carries a destination File.
 */
public class ExportConfig {

    private boolean showAllColumns;
    private boolean showAllRows;
    private boolean useEmptyPlaceholder;

    public ExportConfig() {
        this.showAllColumns = true;
        this.showAllRows = true;
        this.useEmptyPlaceholder = true;
    }

    public ExportConfig(boolean showAllColumns, boolean showAllRows, boolean useEmptyPlaceholder) {
        this.showAllColumns = showAllColumns;
        this.showAllRows = showAllRows;
        this.useEmptyPlaceholder = useEmptyPlaceholder;
    }

    /** 
     * Returns an ExportConfig with default values
     */
    public static ExportConfig defaults() {
        return new ExportConfig();
    }

    public boolean getShowAllColumns() {
        return showAllColumns;
    }

    public ExportConfig setShowAllColumns(boolean showAllColumns) {
        this.showAllColumns = showAllColumns;
        return this;
    }

    public boolean getShowAllRows() {
        return showAllRows;
    }

    public ExportConfig setShowAllRows(boolean showAllRows) {
        this.showAllRows = showAllRows;
        return this;
    }

    public boolean getUseEmptyPlaceholder() {
        return useEmptyPlaceholder;
    }

    public ExportConfig setUseEmptyPlaceholder(boolean useEmptyPlaceholder) {
        this.useEmptyPlaceholder = useEmptyPlaceholder;
        return this;
    }
}

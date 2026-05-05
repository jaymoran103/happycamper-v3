package com.echo.ui.dialog;

/**
 * Enum representing the available column sizing options.
 * This provides a type-safe way to handle column sizing options throughout the application.
 */
public enum ColumnSizingOption {
    AUTO_SIZE("Auto-size"),
    EQUAL_WIDTH("Equal Width"),
    CUSTOM_WIDTH("Custom Width");

    private final String displayName;

    ColumnSizingOption(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ColumnSizingOption fromDisplayName(String displayName) {
        for (ColumnSizingOption option : values()) {
            if (option.displayName.equals(displayName)) {
                return option;
            }
        }
        return null;
    }

    public static String[] getAllDisplayNames() {
        ColumnSizingOption[] options = values();
        String[] displayNames = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            displayNames[i] = options[i].displayName;
        }
        return displayNames;
    }
}

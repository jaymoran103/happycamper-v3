package com.echo.domain;

/**
 * Constants and utility methods for handling data values throughout the application.
 */
public class DataConstants {
    /** Internal representation of empty/missing data */
    public static final String EMPTY_VALUE = null;

    /** Display values for different contexts */
    public static final String DISPLAY_EMPTY = " - ";       // Empty string for UI display
    public static final String DISPLAY_NO_DATA = "No Data"; // Text for fields that explicitly indicate no data
    public static final String EXPORT_EMPTY = "";           // Empty string for CSV export

    /** Default value for the number of rounds when none are assigned */
    public static final String NO_ROUNDS = "0";

    /**
     * Checks if a string value should be considered empty.
     * A value is considered empty if it is null, an empty string after trimming,
     * or equals one of the predefined empty display values.
     *
     * @param value The string value to check
     * @return true if the value is considered empty, false otherwise
     */
    public static boolean isEmpty(String value) {
        //If value is null, it's empty. return true.
        if (value == null) return true;


        // value = value.trim();
        return value.isEmpty()
            || value.equals(DISPLAY_NO_DATA)
            || value.equals(EXPORT_EMPTY)
            || value.equals(DISPLAY_EMPTY)
            || value.trim().isEmpty();
    }

    /**
     * Normalizes a string value to the EMPTY_VALUE constant if it is empty.
     * This method standardizes the representation of empty values throughout the application.
     *
     * @param value The string value to normalize
     * @return EMPTY_VALUE if the value is empty or equals DISPLAY_NO_DATA, otherwise the trimmed value
     */
    public static String normalizeEmpty(String value) {
        if (value == null) return EMPTY_VALUE;
        value = value.trim();
        return value.isEmpty() || value.equals(DISPLAY_NO_DATA) ? EMPTY_VALUE : value;
    }

    /**
     * Returns the appropriate display value for a given string.
     * If the string is empty, returns {@code DISPLAY_NO_DATA} or {@code DISPLAY_EMPTY}
     * based on {@code usePlaceholder}. Otherwise returns the string itself.
     *
     * @param value          The string value to check
     * @param usePlaceholder When {@code true}, empty values are rendered as
     *                       {@link #DISPLAY_NO_DATA}; when {@code false} as
     *                       {@link #DISPLAY_EMPTY}.
     * @return The display value suitable for UI presentation
     */
    public static String getDisplayValue(String value, boolean usePlaceholder) {
        if (isEmpty(value)) {
            return usePlaceholder ? DISPLAY_NO_DATA : DISPLAY_EMPTY;
        }
        return value;
    }

    /**
     * Returns the appropriate display value for a given string, using
     * {@link #DISPLAY_NO_DATA} as the placeholder for empty values.
     *
     * @param value The string value to check
     * @return The display value suitable for UI presentation
     */
    public static String getDisplayValue(String value) {
        return getDisplayValue(value, true);
    }

    /**
     * Returns the appropriate export value for a given string.
     * If the string is empty, returns the EXPORT_EMPTY constant (empty string).
     * Otherwise, returns the string itself. This ensures consistent formatting
     * when exporting data to CSV files.
     *
     * @param value The string value to check
     * @return The export value for the string suitable for CSV export
     */
    public static String getExportValue(String value) {
        return isEmpty(value) ? EXPORT_EMPTY : value;
    }

    /**
     * Returns the appropriate default value for a given header.
     * Different headers have different default values based on their meaning and usage.
     * For example, round assignments default to EMPTY_VALUE, while round count defaults to "0".
     *
     * @param header The header name to check (typically from RosterHeader enum)
     * @return The default value for the header
     */
    public static String getDefaultValue(String header) {
        RosterHeader rosterHeader = RosterHeader.determineHeaderType(header);
        if (rosterHeader == null) {
            return DISPLAY_NO_DATA;
        } else if (RosterHeader.isRound(header)) {
            return EMPTY_VALUE;
        } else if (header.equals(RosterHeader.ROUND_COUNT.standardName)) {
            return NO_ROUNDS;
        } else {
            return DISPLAY_NO_DATA;
        }
    }


}
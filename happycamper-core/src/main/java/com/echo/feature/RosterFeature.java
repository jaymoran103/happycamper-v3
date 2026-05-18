package com.echo.feature;

import java.util.List;
import java.util.Map;

import com.echo.assertion.RosterAssertion;
import com.echo.domain.EnhancedRoster;
import com.echo.logging.WarningManager;

/**
 * Interface for roster features that can be applied to enhance a roster.
 *
 * The RosterFeature interface defines the contract that all roster enhancement features
 * must implement. Features are modular components that add specific functionality to a roster,
 * such as activity tracking, preference evaluation, or program information.
 *
 * Each feature follows a lifecycle pattern:
 * 1. preValidate checks if data prerequisites are met at this step in roster service execution
 * 2. applyFeature modifies the roster with the feature's functionality
 *   2a. many applyFeature implementations will call an applyToCamper method to perform operations for each camper in turn
 * 3. postValidate ensures the feature was applied correctly
 *
 * Each feature has a unique ID, a display name, and may require specific headers to be present
 * in the roster. Features may also add new headers to the roster during application.
 */
public interface RosterFeature {
    /**
     * Gets the unique identifier for this feature.
     *
     * @return The feature ID
     */
    String getFeatureId();


    /**
     * Gets the display name for this feature.
     *
     * @return The feature name
     */
    String getFeatureName();

    /**
     * Gets the list of headers required by this feature.
     *
     * @return List of required header names
     */
    List<String> getRequiredHeaders();

    /**
     * Gets the list of headers added by this feature.
     *
     * @return List of added header names
     */
    List<String> getAddedHeaders();

    /**
     * Gets a map of field names to regex patterns for validation.
     *
     * @return Map of field names to regex patterns
     */
    Map<String, String> getRequiredFormats();

    /**
     * Applies this feature to a roster, using the supplied {@link EnhancementContext}.
     *
     * This is the main method that implements the feature's functionality. 
     * It should add headers, process data, and modify the roster as needed accordingly.
     * Issues encountered should be logged to the warning manager rather than thrown.
     *
     * @param context the enhancement context carrying roster and warning manager
     */
    void applyFeature(EnhancementContext context);



    /**
     * Called before applying the feature to ensure that data prerequisites are met.
     * This method should check that all required headers are present and that the data
     * is in the expected format. Issues should be logged to the warning manager.
     *
     * @param roster Enhanced roster to be validated before modification
     * @param warningManager The warning manager to use for logging encountered issues
     * @return true if prerequisites are met and the feature can be applied; false otherwise.
     *         If false is returned, the feature will be skipped. If the feature is an ActivityFeature
     *         and returns false, the entire process will be aborted.
     */
    boolean preValidate(EnhancedRoster roster, WarningManager warningManager);

    /**
     * Called after applying the feature to ensure that the resulting roster is valid and safe to use.
     * This method should verify that the feature was applied correctly and that the roster
     * is in a consistent state. Issues should be logged to the warning manager.
     *
     * @param roster Enhanced roster that has been modified by the feature
     * @param warningManager The warning manager to use for logging encountered issues
     * @return true if the feature's effect on the roster resulted in a valid roster;
     *         false if there are critical issues that make the roster unsafe to use.
     *         Non-fatal warnings should be logged but shouldn't cause this method to return false.
     */
    boolean postValidate(EnhancedRoster roster, WarningManager warningManager);

    /**
     * Read-only checks that run against the enhanced roster after this feature has been
     * applied successfully. Each assertion verifies an invariant that depends on this
     * feature's columns (e.g., {@code ROUND_COUNT == MAX_ROUNDS}).
     *
     * Default returns an empty list, so features that haven't declared assertions
     * continue to work unchanged.
     *
     * @return the assertions owned by this feature; never {@code null}
     */
    default List<RosterAssertion> getAssertions() {
        return List.of();
    }

}

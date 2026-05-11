package com.echo.service;

import java.io.File;
import java.util.List;

import com.echo.domain.ActivityRoster;
import com.echo.domain.CampConfig;
import com.echo.domain.Camper;
import com.echo.domain.CamperRoster;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.EnhancementContext;
import com.echo.feature.FeatureRegistration;
import com.echo.feature.FeatureRegistry;
import com.echo.feature.RosterFeature;
import com.echo.filter.FilterManager;
import com.echo.logging.RosterException;
import com.echo.logging.WarningManager;

/**
 * RosterService is the core service for managing roster data and applying RosterFeatures.
 *
 * The RosterService coordinates the process of importing, enhancing, and exporting rosters.
 * It manages a collection of available features that can be applied to rosters and handles
 * the application of these features in the correct order with appropriate validation.
 *
 * This service acts as the central coordinator between the UI, import/export services,
 * and the feature implementations.
 */
public class RosterService {
    private final ImportService importService;
    private final ExportService exportService;
    private final FeatureRegistry featureRegistry;
    private WarningManager warningManager; // Created new for each processing operation

    /**
     * Creates a new RosterService backed by the default core feature registry.
     */
    public RosterService(ImportService importService, ExportService exportService) {
        this(importService, exportService, FeatureRegistry.defaults(CampConfig.defaults()));
    }

    /**
     * Creates a new RosterService with an explicit feature registry. Desktop callers
     * pass a registry pre-populated with Swing-coupled filters; web callers use core
     * defaults.
     */
    public RosterService(ImportService importService, ExportService exportService, FeatureRegistry featureRegistry) {
        this.importService = importService;
        this.exportService = exportService;
        this.featureRegistry = featureRegistry;
    }

    /**
     * @return the registry of features and filter pairings used by this service
     */
    public FeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    /**
     * Creates an enhanced roster from camper and activity files.
     * This is the main method that orchestrates the entire roster enhancement process:
     * 1. Validates input files
     * 2. Imports camper and activity data
     * 3. Creates an enhanced roster with camper data
     * 4. Applies each enabled feature in sequence
     * 5. Sorts headers for consistent display
     *
     * @param camperFile The file containing camper data
     * @param activityFile The file containing activity data
     * @param enabledFeatureIds The IDs of features to enable and apply
     * @return A new EnhancedRoster with all features applied, or null if a critical error occurred
     */
    public EnhancedRoster createEnhancedRoster(File camperFile, File activityFile, List<String> enabledFeatureIds){

        //Create a new WarningManager. doesn't need to be cleared if a new one is created for each process
        warningManager = new WarningManager();

        try {
            // Import tosters (with basic validation)
            CamperRoster camperRoster = importService.importCamperRoster(camperFile);
            ActivityRoster activityRoster = importService.importActivityRoster(activityFile);

            // Standardize program names
            camperRoster.normalizePrograms();

            // Validate rosters
            camperRoster.validate(warningManager);
            activityRoster.validate(warningManager);

            // Create enhanced roster, add camper headers and data.
            EnhancedRoster enhancedRoster = new EnhancedRoster();
            for (String header : camperRoster.getHeaderMap().keySet()) {
                enhancedRoster.addHeader(header);
            }

            // Copy camper data
            for (Camper camper : camperRoster.getCampers()) {
                enhancedRoster.addCamper(camper);
            }

            // Apply each enabled feature via a single EnhancementContext, which carries
            // the activity roster for features that need it (ActivityFeature) and is
            // ignored by features that don't.
            EnhancementContext context = new EnhancementContext(enhancedRoster, activityRoster, warningManager);

            for (String featureId : enabledFeatureIds) {
                FeatureRegistration registration = featureRegistry.find(featureId).orElse(null);
                if (registration == null) {
                    continue;
                }
                RosterFeature feature = registration.feature();

                // Prevalidate feature, skipping (or aborting) on failure.
                // Always-enabled features (like ActivityFeature) are prerequisites for the rest of the pipeline, 
                // so their failure aborts the whole import.
                boolean preValidated = feature.preValidate(enhancedRoster, warningManager);
                if (!preValidated) {
                    if (registration.alwaysEnabled()) {
                        return null;
                    }
                    continue;
                }

                try {
                    feature.applyFeature(context);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

                // Post-validation: if the feature left the roster in an invalid state,
                // scrap the whole process.
                boolean postValidated = feature.postValidate(enhancedRoster, warningManager);
                if (!postValidated) {
                    return null;
                }
            }

            // Sort headers by order once they're all added for consistent display
            // RosterHeader.updateHeaderMapOrder ensures headers appear in a logical order
            // regardless of the order in which features were applied
            RosterHeader.updateHeaderMapOrder(enhancedRoster.getHeaderMap());



            return enhancedRoster;
        }
        catch (RosterException e){
            warningManager.logError(e);
            return null;
        }
        catch (Exception e) {
            RosterException exceptionWrapper = RosterException.create_normalWrapper("An error occurred while merging rosters: " + e.getMessage(), e);
            warningManager.logError(exceptionWrapper);
            return null;
        }
    }

    /**
     * Finds a feature by its unique ID.
     * Searches through the list of available features to find one with a matching ID.
     *
     * @param featureId The ID of the feature to find
     * @return The matching RosterFeature instance, or null if not found
     */

    /**
     * Gets all available features that can be applied to rosters.
     * Returns a defensive copy to prevent modification of the internal list.
     *
     * @return A new list containing all available RosterFeature instances
     */
    public List<RosterFeature> getAvailableFeatures() {
        return featureRegistry.getFeatures();
    }

    /**
     * Gets the current warning manager.
     * The warning manager contains all warnings and errors encountered during processing.
     *
     * @return The current WarningManager instance
     * @throws IllegalStateException if called before processing has started
     */
    public WarningManager getWarningManager() {
        if (warningManager == null) {
            throw new IllegalStateException("Warning manager is null. New instance should be created whenever a service starts.");
        }
        return warningManager;
    }

    /**
     * Exports a roster using the specified export settings.
     * This method provides a unified interface for all export options.
     *
     * @param roster The enhanced roster to export
     * @param filterManager The filter manager to apply (can be null for non-filtered exports)
     * @param settings The export settings to use
     * @throws RosterException if an error occurs during the export process
     */
    public void exportRoster(EnhancedRoster roster, FilterManager filterManager, ExportSettings settings) throws RosterException {
        try {
            // Use the ExportService to handle the export with the specified settings
            exportService.exportRosterToCSV(roster, filterManager, settings);
        } catch (Exception e) {
            throw RosterException.create_normalWrapper("An error occurred while exporting: " + e.getClass().getName(), e);
        }
    }

}



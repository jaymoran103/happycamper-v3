package com.echo.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.echo.assertion.AssertionReport;
import com.echo.assertion.AssertionService;
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

    private static final Logger LOG = LoggerFactory.getLogger(RosterService.class);

    private final ImportService importService;
    private final ExportService exportService;
    private final FeatureRegistry featureRegistry;
    private final AssertionService assertionService;
    private WarningManager warningManager; // Created new for each processing operation
    private AssertionReport assertionReport; // Populated at the end of a successful run

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
        this.assertionService = new AssertionService(featureRegistry);
    }

    /**
     * @return the registry of features and filter pairings used by this service
     */
    public FeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    /**
     * Creates an enhanced roster from camper and activity files.
     * Imports both files, then routes into {@link #applyFeaturesAndFinalize}.
     *
     * @param camperFile The file containing camper data
     * @param activityFile The file containing activity data
     * @param enabledFeatureIds The IDs of features to enable and apply
     * @return A new EnhancedRoster with all features applied, or null if a critical error occurred
     */
    public EnhancedRoster createEnhancedRoster(File camperFile, File activityFile, List<String> enabledFeatureIds){
        warningManager = new WarningManager();
        assertionReport = null;
        try {
            CamperRoster camperRoster = importService.importCamperRoster(camperFile);
            ActivityRoster activityRoster = importService.importActivityRoster(activityFile);
            return applyFeaturesAndFinalize(camperRoster, activityRoster, enabledFeatureIds);
        } catch (RosterException e) {
            warningManager.logError(e);
            return null;
        } catch (Exception e) {
            warningManager.logError(RosterException.create_normalWrapper(
                    "An error occurred while merging rosters: " + e.getMessage(), e));
            return null;
        }
    }

    /**
     * Stream-based overload used by the web layer (MultipartFile.getInputStream).
     * Mirrors the File overload's contract; sourceName strings are used for error messages
     * since streams don't carry filenames.
     *
     * Stream ownership: {@code ImportUtils.parseStream} closes the streams internally —
     * callers may also wrap in try-with-resources (servlet containers tolerate double-close).
     *
     * @param camperStream stream of camper-roster CSV data
     * @param camperSourceName display label for error messages (typically the original filename)
     * @param activityStream stream of activity-roster CSV data
     * @param activitySourceName display label for error messages
     * @param enabledFeatureIds The IDs of features to enable and apply
     * @return A new EnhancedRoster with all features applied, or null if a critical error occurred
     */
    public EnhancedRoster createEnhancedRoster(InputStream camperStream, String camperSourceName,
                                               InputStream activityStream, String activitySourceName,
                                               List<String> enabledFeatureIds){
        warningManager = new WarningManager();
        assertionReport = null;
        try {
            CamperRoster camperRoster = importService.importCamperRoster(camperStream, camperSourceName);
            ActivityRoster activityRoster = importService.importActivityRoster(activityStream, activitySourceName);
            return applyFeaturesAndFinalize(camperRoster, activityRoster, enabledFeatureIds);
        } catch (RosterException e) {
            warningManager.logError(e);
            return null;
        } catch (Exception e) {
            warningManager.logError(RosterException.create_normalWrapper(
                    "An error occurred while merging rosters: " + e.getMessage(), e));
            return null;
        }
    }

    /**
     * Shared pipeline body for both public overloads. Operates on already-imported rosters
     * and runs the normalize → validate → enhance → features → finalize → assertions sequence.
     * Throws on hard failures; the caller's try/catch converts those into a null return with
     * a logged error.
     */
    private EnhancedRoster applyFeaturesAndFinalize(CamperRoster camperRoster,
                                                   ActivityRoster activityRoster,
                                                   List<String> enabledFeatureIds) throws RosterException {
        camperRoster.normalizePrograms();
        camperRoster.validate(warningManager);
        activityRoster.validate(warningManager);

        EnhancedRoster enhancedRoster = new EnhancedRoster();
        for (String header : camperRoster.getHeaderMap().keySet()) {
            enhancedRoster.addHeader(header);
        }
        for (Camper camper : camperRoster.getCampers()) {
            enhancedRoster.addCamper(camper);
        }

        EnhancementContext context = new EnhancementContext(enhancedRoster, activityRoster, warningManager);

        for (String featureId : enabledFeatureIds) {
            FeatureRegistration registration = featureRegistry.find(featureId).orElse(null);
            if (registration == null) {
                continue;
            }
            RosterFeature feature = registration.feature();

            boolean preValidated = feature.preValidate(enhancedRoster, warningManager);
            if (!preValidated) {
                if (registration.alwaysEnabled()) {
                    return null;
                }
                continue;
            }

            feature.applyFeature(context);

            boolean postValidated = feature.postValidate(enhancedRoster, warningManager);
            if (!postValidated) {
                return null;
            }
        }

        RosterHeader.updateHeaderMapOrder(enhancedRoster.getHeaderMap());

        assertionReport = assertionService.runAssertions(enhancedRoster);
        LOG.info("Assertions: total={} passed={} failed={} skipped={}",
                assertionReport.totalCount(),
                assertionReport.passedCount(),
                assertionReport.failedCount(),
                assertionReport.skippedCount());

        return enhancedRoster;
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
     * Gets the assertion report from the most recent successful enhancement run.
     *
     * @return the report produced by the last successful {@code createEnhancedRoster} call
     * @throws IllegalStateException if no successful run has completed yet (either
     *         {@code createEnhancedRoster} was never called, or the last call aborted early)
     */
    public AssertionReport getAssertionReport() {
        if (assertionReport == null) {
            throw new IllegalStateException(
                    "Assertion report is null. Call createEnhancedRoster successfully first.");
        }
        return assertionReport;
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



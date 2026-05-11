package com.echo;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import java.io.File;

import com.echo.domain.CampConfig;
import com.echo.feature.FeatureRegistration;
import com.echo.feature.FeatureRegistry;
import com.echo.feature.ProgramFeature;
import com.echo.filter.SortedProgramFilter;
import com.echo.service.ExportService;
import com.echo.service.ImportService;
import com.echo.service.RosterService;
import com.echo.ui.MainWindow;

/**
 * Main entry point for the Happy Camper application.
 */
public class HappyCamper {
    public static final String NAME = "HappyCamper";
    public static final String VERSION = "2.2";
    public static String NAME_VERSION = NAME+" "+VERSION;

    public static boolean WAIT_TO_AUTOMATE = false;
    public static boolean PRINT_LOGGED_ERRORS = true;

    private static MainWindow mainWindowInstance;


    public static MainWindow setupApp(boolean andStart){
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        // Create services
        ImportService importService = new ImportService();
        ExportService exportService = new ExportService();
        RosterService rosterService = new RosterService(importService, exportService, buildDesktopFeatureRegistry());

        // Create UI immediately instead of using invokeLater
        createSingleWindow(rosterService);

        if (andStart) {
            // Only make it visible on EDT
            SwingUtilities.invokeLater(() -> {
                mainWindowInstance.setVisible(true);
            });
        }

        return mainWindowInstance;
    }

    private static MainWindow createSingleWindow(RosterService rosterService){
        if (mainWindowInstance==null){
            mainWindowInstance = new MainWindow(rosterService);
            mainWindowInstance.setTitle(NAME+" "+VERSION);
            mainWindowInstance.setDefaultCloseOperation(MainWindow.EXIT_ON_CLOSE);
        }
        return mainWindowInstance;
    }
    public static MainWindow accessSingleWindow(){
        if (mainWindowInstance==null){
            throw new Error("Main window has not been created yet");
        }
        return mainWindowInstance;
    }

    

    /**
     * Main method.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        setupApp(true);
    }

    public static void mainTest(File camperFile, File activityFile, String[] features) {
        MainWindow window = setupApp(true);
        window.automateImport(camperFile, activityFile, features);
    }

    /**
     * Builds a {@link FeatureRegistry} configured for the desktop module. 
     * The core defaults have no filter for {@code ProgramFeature} because {@code SortedProgramFilter} depends
     * on Swing; the desktop registry swaps in that pairing here.
     */
    public static FeatureRegistry buildDesktopFeatureRegistry() {
        FeatureRegistry registry = FeatureRegistry.defaults(CampConfig.defaults());
        registry.replace("program", new FeatureRegistration(new ProgramFeature(), SortedProgramFilter::new, false));
        return registry;
    }

}
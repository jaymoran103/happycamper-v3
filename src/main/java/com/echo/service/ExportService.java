package com.echo.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.filter.FilterManager;
import com.echo.logging.RosterException;

/**
 * Service for exporting roster data to files in various formats.
 *
 * The ExportService helps write roster data to CSV files with
 * options for filtering and selecting which headers to include.
 *
 * This service works with the FilterManager to allow exporting only campers
 * that match specific criteria, and supports exporting either all headers
 * or only those marked as visible in the roster.
 */
public class ExportService {

    /**
     * Exports a roster to a CSV file using the specified export settings.
     * This is the main export method that handles all export options.
     *
     * @param roster The roster containing the data to export
     * @param filterManager The filter manager to apply (can be null for no filtering)
     * @param settings The export settings to use
     * @throws RosterException if an error occurs during the export process
     */
    public void exportRosterToCSV(EnhancedRoster roster, FilterManager filterManager, ExportSettings settings) throws RosterException {
        if (settings == null || settings.getDestinationFile() == null) {
            throw new IllegalArgumentException("Export settings and destination file must not be null");
        }

        List<String> headers = settings.getShowAllColumns() ? roster.getAllHeaders() : roster.getVisibleHeaders();
        FilterManager effectiveFilterManager = settings.getShowAllRows() ? null : filterManager;

        File destination = settings.getDestinationFile();
        try (FileOutputStream fos = new FileOutputStream(destination)) {
            exportToStream(roster, effectiveFilterManager, headers, fos, settings.getUseEmptyPlaceholder(), destination.getName());
        } catch (IOException e) {
            throw RosterException.create_normalWrapper("Error exporting to CSV with file " + destination.getName(), e);
        }
    }

    /**
     * Exports a roster to an OutputStream using the specified ExportConfig.
     * This is the stream-based entry point, to be used by the web layer.
     *
     * @param roster The roster containing the data to export
     * @param filterManager The filter manager to apply (can be null for no filtering)
     * @param config The export configuration (core-safe, no File references)
     * @param outputStream The stream to write CSV data to; this method closes the stream when done
     * @throws RosterException if an error occurs during the export process
     */
    public void exportRosterToCSV(EnhancedRoster roster, FilterManager filterManager, ExportConfig config, OutputStream outputStream) throws RosterException {
        if (config == null) {
            throw new IllegalArgumentException("ExportConfig must not be null");
        }
        List<String> headers = config.getShowAllColumns() ? roster.getAllHeaders() : roster.getVisibleHeaders();
        FilterManager effectiveFilterManager = config.getShowAllRows() ? null : filterManager;
        exportToStream(roster, effectiveFilterManager, headers, outputStream, config.getUseEmptyPlaceholder(), "stream");
    }

    /**
     * Core export logic shared by the File-based and OutputStream-based public methods.
     * Writes the roster data to the provided OutputStream as UTF-8 encoded CSV.
     *
     * @param roster The roster containing the data to export
     * @param filterManager The filter manager to apply (can be null for no filtering)
     * @param visibleHeaders The list of headers to include in the export
     * @param outputStream The stream to write to; the caller manages closing
     * @param useEmptyPlaceholder Whether to use a placeholder for empty values
     * @param sourceName A display name used in error messages
     * @throws RosterException if an error occurs during the export process
     */
    private void exportToStream(EnhancedRoster roster, FilterManager filterManager, List<String> visibleHeaders, OutputStream outputStream, boolean useEmptyPlaceholder, String sourceName) throws RosterException {
        try (Writer writer = new OutputStreamWriter(outputStream, java.nio.charset.StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer,
                 CSVFormat.DEFAULT.builder()
                     .setHeader(visibleHeaders.toArray(new String[0]))
                     .setQuoteMode(QuoteMode.ALL)  // Force quotes around all values
                     .build())) {

            // Process each camper, applying filters if a filter manager is provided
            for (Camper camper : roster.getCampers()) {
                // Only include campers that pass all filters (or all campers if no filter manager)
                if (filterManager == null || filterManager.applyFilters(camper)) {
                    // Build a row with values for each requested header
                    List<String> rowData = new ArrayList<>();
                    for (String header : visibleHeaders) {
                        String value = camper.getValue(header);

                        // Handle empty values based on settings
                        if (value == null || value.trim().isEmpty()) {
                            value = useEmptyPlaceholder ? DataConstants.DISPLAY_NO_DATA : "";
                        }
                        rowData.add(value);
                    }
                    // Write the row to the CSV file
                    printer.printRecord(rowData);
                }
            }
        } catch (IOException e) {
            throw RosterException.create_normalWrapper("Error exporting to CSV (" + sourceName + ")", e);
        }
    }

}

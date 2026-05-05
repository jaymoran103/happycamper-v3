package com.echo.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.echo.logging.RosterException;
import com.echo.service.ParsedCSV;


/**
 * Centralized validation service for files used throughout the application.
 *
 * Provides a comprehensive set of validation methods for checking files before they are processed by the application.
 * Uses a validation chain pattern that allows multiple validation steps to be combined, terminating early if a step fails.
 */
public class ImportFileValidator {

    /**
     * Enum defining the different types of file validation issues.
     * Each issue type has a user-friendly text description that can be
     * displayed in error messages and dialogs.
     */
    public enum FileIssueSummary {
        INVALID_FILE("Invalid File"),
        FILE_NOT_FOUND("File Not Found"),
        INVALID_FILE_EXTENSION("Invalid File Extension"),
        CANNOT_READ_FILE("Cannot Read File"),
        MISSING_DATA("Missing Data"),
        MALFORMED_DATA("Malformed Data"),
        MISSING_HEADERS("Missing Headers");

        public final String text;

        FileIssueSummary(String _text) {
            text = _text;
        }
    }

    
    private static ValidationResult<File> validateFileExists(File file) {
        if (file == null) {
            return ValidationResult.failure(
                FileIssueSummary.INVALID_FILE.text,
                "The file path is null"
            );
        }

        if (!file.exists()) {
            return ValidationResult.failure(
                FileIssueSummary.FILE_NOT_FOUND.text,
                String.format("Couldn't find file '%s'\nIt may have been moved or deleted.\nFull file path:\n%s",
                              file.getName(), file.getPath())
            );
        }

        return ValidationResult.success(file);
    }

    private static ValidationResult<File> validateFileReadable(File file) {
        if (!file.canRead()) {
            return ValidationResult.failure(
                FileIssueSummary.CANNOT_READ_FILE.text,
                String.format("Cannot read file '%s'\nMake sure another application isn't using it and you have permission to access it.",
                              file.getName())
            );
        }
        return ValidationResult.success(file);
    }

    private static ValidationResult<File> validateFileExtension(File file) {
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            return ValidationResult.failure(
                FileIssueSummary.INVALID_FILE_EXTENSION.text,
                String.format("The file '%s' is not a valid CSV file.\nPlease provide a file with the .csv extension.",
                              file.getName())
            );
        }
        return ValidationResult.success(file);
    }


    /**
     * Performs basic validation of a file's properties.
     * This method checks that the file exists, is readable, and has the correct extension.
     * 
     * Intended as a first step before more specific validations.
     *
     * @param file The file to validate
     * @throws RosterException if the file is null, doesn't exist, can't be read, or has an invalid extension
     */
    public static void validateBasicFile(File file) throws RosterException {
        ValidationResult<File> result = validateFileExists(file)
            .andThen(ImportFileValidator::validateFileReadable)
            .andThen(ImportFileValidator::validateFileExtension);

        if (!result.isValid()) {
            throw RosterException.fileException(result.getErrorSummary(), result.getErrorMessage());
        }
    }

    /**
     * Combines individual validation steps to validate a CSV file's already parsed contents.
     * - Validates file content (headers and data rows)
     * - Ensures all required headers are present (if specified)
     * - That all rows have consistent lengths
     *
     * @param file The file to validate
     * @param parsedCSV The parsed CSV data
     * @param requiredHeaders List of required headers (optional, can be null for no header requirements)
     * @throws RosterException if any validation step fails, with a specific error message
     */
    public static void validateCSVFile(File file, ParsedCSV parsedCSV, List<String> requiredHeaders) throws RosterException {
        validateCSVContent(parsedCSV, requiredHeaders, file.getName());
    }

    /**
     * Validates parsed CSV content using a source name string instead of a File reference.
     * This is the stream-compatible validation entry point used by the web layer.
     *
     * @param parsedCSV The parsed CSV data
     * @param requiredHeaders List of required headers (can be null for no header requirements)
     * @param sourceName A display name for the source used in error messages (e.g. the original filename)
     * @throws RosterException if content is empty, missing required headers, or has inconsistent rows
     */
    public static void validateCSVContent(ParsedCSV parsedCSV, List<String> requiredHeaders, String sourceName) throws RosterException {
        validateHasContent(sourceName, parsedCSV);
        if (requiredHeaders != null && !requiredHeaders.isEmpty()) {
            validateHeaders(sourceName, parsedCSV, requiredHeaders);
        }
        validateRowConsistency(sourceName, parsedCSV);
    }

    /**
     * Checks that the given source has content (headers and data rows).
     */
    private static void validateHasContent(String sourceName, ParsedCSV parsedCSV) throws RosterException {
        if (parsedCSV.getHeaderNames().isEmpty()) {
            throw RosterException.noData(sourceName, false);
        }
        if (parsedCSV.isEmpty()) {
            throw RosterException.noData(sourceName, true);
        }
    }

    /**
     * Validates that a CSV source contains all the required headers.
     */
    private static void validateHeaders(String sourceName, ParsedCSV parsedCSV, List<String> requiredHeaders) throws RosterException {
        Set<String> fileHeaders = new HashSet<>(parsedCSV.getHeaderNames());
        List<String> missingHeaders = new ArrayList<>();
        for (String header : requiredHeaders) {
            if (!fileHeaders.contains(header)) {
                missingHeaders.add(header);
            }
        }
        if (!missingHeaders.isEmpty()) {
            throw RosterException.missingHeaders(sourceName, missingHeaders);
        }
    }

    /**
     * Validates that all rows in a CSV source have consistent lengths.
     */
    private static void validateRowConsistency(String sourceName, ParsedCSV parsedCSV) throws RosterException {
        int headerCount = parsedCSV.getHeaderNames().size();
        int rowNumber = 1;
        for (Map<String, String> record : parsedCSV.getRows()) {
            rowNumber++;
            int cellCount = record.size();
            if (cellCount != headerCount) {
                throw RosterException.create_malformedRowException(sourceName, headerCount, cellCount, rowNumber);
            }
        }
    }

    public static ValidationResult validateImportFile(File file){
        try {
            validateBasicFile(file);
            return ValidationResult.success(file);
        } catch (RosterException e) {
            return ValidationResult.failure("Error in file '"+file.getName()+"': "+e.getSummary(), e.getExplanation());
        }
    }
}
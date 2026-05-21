package com.echo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.echo.logging.RosterException;

/**
 * Utility class intended to simplify file and importing process while sorting out file processing bugs
 */
public class ImportUtils {

    private static final Logger log = LoggerFactory.getLogger(ImportUtils.class);


    /**
     * Core parsing logic shared by file and stream entry points.
     * @param reader A BufferedReader over the CSV content
     * @param sourceName A display name used in error messages
     * @return ParsedCSV object containing the parsed data
     * @throws IOException if an I/O error occurs
     * @throws RosterException if the content is malformed
     */
    private static ParsedCSV parseReader(BufferedReader reader, String sourceName) throws IOException, RosterException {
        StringReader cleanedDataReader = new StringReader(ContentCleaner.cleanFileContent(reader));
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().build();
        try (CSVParser parser = CSVParser.parse(cleanedDataReader, format)) {
            List<String> headers = parser.getHeaderNames();
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                checkMalformedRow(record, headers, sourceName);
                for (String header : headers) {
                    row.put(header, record.get(header));
                }
                rows.add(row);
            }
            return new ParsedCSV(rows, headers);
        }
    }

    /**
     * Parses a file into a ParsedCSV object.
     * @param file The file to parse
     * @return ParsedCSV object containing the parsed data
     * @throws RosterException if the file is invalid or an error occurs during parsing
     */
    public static ParsedCSV parseFile(File file) throws RosterException{
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return parseReader(reader, file.getName());
        } catch (IOException e) {
            throw RosterException.create_normalWrapper("Error in parsing file '"+file.getName()+"': IOException", e);
        }
    }

    /**
     * Parses an InputStream into a ParsedCSV object.
     * This is the stream-based entry point, to be used by the web layer.
     *
     * <p><b>Stream ownership:</b> this method takes ownership of {@code inputStream} and closes
     * it before returning (the underlying try-with-resources on the {@code BufferedReader}).
     * Callers that also wrap their stream in try-with-resources will trigger a double-close;
     * this is safe with {@code FileInputStream} and with Spring/Tomcat {@code MultipartFile}
     * implementations (their {@code close()} is a no-op on the second call). Resolved as
     * documented behavior — see {@code -PLANNING/known-issues.md} ISSUE-02.
     *
     * @param inputStream The stream to parse (closed by this method)
     * @param sourceName A display name for the source, used in error messages
     * @return ParsedCSV object containing the parsed data
     * @throws RosterException if an error occurs during parsing
     */
    public static ParsedCSV parseStream(InputStream inputStream, String sourceName) throws RosterException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return parseReader(reader, sourceName);
        } catch (IOException e) {
            throw RosterException.create_normalWrapper("Error in parsing stream '"+sourceName+"': IOException", e);
        }
    }
    /**
     * Checks if a CSVRecord has the same number of cells as the expected number of headers.
     * @param record The CSVRecord to check
     * @param headers The list of headers
     * @param sourceName The source name (file name or stream label) for error messages
     * @throws RosterException if the record has a different number of cells than the number of headers
     */
    private static void checkMalformedRow(CSVRecord record, List<String> headers, String sourceName) throws RosterException {
        if (record.size() != headers.size()) {
            throw RosterException.create_malformedRowException(sourceName, headers.size(), record.size(), (int)record.getRecordNumber());
        }
    }
}

/**
 * ContentCleaner taken from version 1.
 * Utility class used to clean problematic characters from CSV files, including junk leading/trailing characters and uneven quotes.
 */
class ContentCleaner {
    
    // Logger for debugging and warnings about content issues
    private static final Logger log = LoggerFactory.getLogger(ContentCleaner.class);

    /** Uses given reader to iterate through a file and clean its content. Used by setupFromCSV()
     * @param reader BufferedReader for a CSV file
     * @return StringBuilder containing cleaned content
     * @throws IOException
     */
    public static String cleanFileContent(Reader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder builder = new StringBuilder();
        String line;
        
        while ((line = bufferedReader.readLine()) != null) {
            String cleanedLine = cleanLine(line);
            if (cleanedLine != null) {
                builder.append(cleanedLine).append("\n");
            }
        }
        return builder.toString();
    }

    /**
     * Trims problematic characters from CSV line, ensuring every line starts and ends with quote marks, and has an even number of quotes.
     * @param line The line to clean
     * @return Cleaned line or null if line should be ignored
     */
    private static String cleanLine(String line) {
        // Skip empty lines or lines that are just whitespace
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        // Skip lines that start with a comment character
        if (line.charAt(0) == '#') {
            return null;
        }

        // Clean start of line
        line = cleanLineStart(line);
        
        // Clean end of line
        line = cleanLineEnd(line);

        // Check for valid quote count
        if (!hasValidQuoteCount(line)) {
            log.warn("Filtered a line with uneven quote count: {}", line);
            return null;
        }

        return line.trim();
    }

    /**
     * Removes problematic characters from the start of a line until it begins with a quote.
     * @param line The line to clean
     * @return Line with clean start
     */
    private static String cleanLineStart(String line) {
        while (!line.startsWith("\"") && !line.isEmpty()) {
            log.debug("Caught a junk character at start: '{}'", line.charAt(0));
            line = line.substring(1);
        }
        return line;
    }

    /**
     * Removes problematic characters from the end of a line until it ends with a quote.
     * @param line The line to clean
     * @return Line with clean end
     */
    private static String cleanLineEnd(String line) {
        while (!line.endsWith("\"") && !line.isEmpty()) {
            log.debug("Caught a junk character at end: '{}'", line.charAt(line.length()-1));
            line = line.substring(0, line.length()-1);
        }
        return line;
    }

    /**
     * Checks if a line has a valid number of quotes (even number).
     * @param line The line to check
     * @return true if the line has an even number of quotes
     */
    private static boolean hasValidQuoteCount(String line) {
        return line.chars().filter(ch -> ch == '"').count() % 2 == 0;
    }

}
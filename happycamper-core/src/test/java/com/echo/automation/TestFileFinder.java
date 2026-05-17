package com.echo.automation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple utility class to find test files by their names.
 * This class uses recursion to find old test files without complex path resolution.
 */
public class TestFileFinder {

    // Cache of file locations to avoid repeated file system lookups
    private static final Map<String, File> fileCache = new HashMap<>();

    // Candidate roots for test resources, in priority order. Covers running from
    // the core module dir, the repo root, and from a sibling module (e.g.
    // happycamper-desktop) where core's tests live one level up.
    private static final List<String> TEST_RESOURCES_DIRS = List.of(
            "src/test/resources/testRosters",
            "happycamper-core/src/test/resources/testRosters",
            "../happycamper-core/src/test/resources/testRosters"
    );

    /**
     * Find a file by its name in the test resources directory.
     *
     * @param fileName The name of the file to find
     * @return The File object if found, or null if not found
     */
    public static File findFile(String fileName) {
        // Check cache first
        if (fileCache.containsKey(fileName)) {
            return fileCache.get(fileName);
        }

        for (String root : TEST_RESOURCES_DIRS) {
            File file = findFileInDirectory(new File(root), fileName);
            if (file != null) {
                fileCache.put(fileName, file);
                return file;
            }
        }

        return null;
    }
    
    /**
     * Recursively search for a file in a directory.
     * 
     * @param directory The directory to search in
     * @param fileName The name of the file to find
     * @return The File object if found, or null if not found
     */
    private static File findFileInDirectory(File directory, String fileName) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileInDirectory(file, fileName);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().equals(fileName)) {
                return file;
            }
        }
        
        return null;
    }
    
    /**
     * Clear the file cache.
     */
    public static void clearCache() {
        fileCache.clear();
    }
}

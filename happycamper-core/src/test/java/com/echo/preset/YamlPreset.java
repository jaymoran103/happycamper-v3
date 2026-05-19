package com.echo.preset;

import com.echo.assertion.AssertionResult;
import com.echo.automation.TestFileFinder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Preset} backed by a YAML manifest under {@code presets/*.yaml}.
 *
 * <p>File resolution uses the same fallback chain as {@code TestPreset}:
 * {@link TestFileFinder#findFile(String)} (basename lookup) first, then the
 * declared relative path against the candidate {@code testRosters/} roots.
 */
public final class YamlPreset implements Preset {

    private static final List<String> TEST_RESOURCES_ROOTS = List.of(
            "src/test/resources/testRosters/",
            "happycamper-core/src/test/resources/testRosters/",
            "../happycamper-core/src/test/resources/testRosters/"
    );

    private final String name;
    private final String description;
    private final String camperFilePath;
    private final String activityFilePath;
    private final int session;
    private final String[] features;
    private final ExpectedOutputs expectedOutputs;

    YamlPreset(String name,
               String description,
               String camperFilePath,
               String activityFilePath,
               int session,
               String[] features,
               ExpectedOutputs expectedOutputs) {
        this.name = name;
        this.description = description;
        this.camperFilePath = camperFilePath;
        this.activityFilePath = activityFilePath;
        this.session = session;
        this.features = features == null ? new String[0] : features;
        this.expectedOutputs = expectedOutputs;
    }

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public int getSession() { return session; }
    @Override public String[] getFeatures() { return features; }
    @Override public ExpectedOutputs getExpectedOutputs() { return expectedOutputs; }

    /** Used by {@code @ParameterizedTest(name = "{0}")} so IDE test runners and
     *  JUnit display names show the preset id instead of {@code YamlPreset@hash}.
     *  (Failsafe's XML still uses positional indices unless its statelessTestsetReporter
     *  is configured for phrased names.) */
    @Override public String toString() { return name; }

    @Override
    public File getCamperFile() {
        return resolve(camperFilePath, "camperFile");
    }

    @Override
    public File getActivityFile() {
        return resolve(activityFilePath, "activityFile");
    }

    private static File resolve(String relativePath, String fieldName) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalStateException("Preset field '" + fieldName + "' is missing");
        }
        // First: filename-only lookup via TestFileFinder (matches TestPreset behavior)
        String basename = new File(relativePath).getName();
        File byBasename = TestFileFinder.findFile(basename);
        if (byBasename != null) {
            return byBasename;
        }
        // Then: relative-path resolution against each candidate root
        for (String root : TEST_RESOURCES_ROOTS) {
            File candidate = new File(root + relativePath);
            if (candidate.exists()) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not resolve preset " + fieldName + ": '" + relativePath
                        + "'. Tried TestFileFinder + roots " + TEST_RESOURCES_ROOTS);
    }

    /** Map → YamlPreset. Used by {@link PresetLoader} after SnakeYAML parses the file. */
    @SuppressWarnings("unchecked")
    static YamlPreset fromMap(Map<String, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("Preset YAML is empty");
        }
        String name = stringField(map, "name", true);
        String description = stringField(map, "description", false);
        String camperFile = stringField(map, "camperFile", true);
        String activityFile = stringField(map, "activityFile", true);
        Object sessionObj = map.get("session");
        if (!(sessionObj instanceof Number)) {
            throw new IllegalArgumentException("Preset '" + name + "' missing or non-numeric 'session'");
        }
        int session = ((Number) sessionObj).intValue();

        List<String> featuresList = (List<String>) map.getOrDefault("features", List.of());
        String[] features = featuresList == null ? new String[0] : featuresList.toArray(new String[0]);

        ExpectedOutputs expected = null;
        Object expectedObj = map.get("expectedOutputs");
        if (expectedObj instanceof Map) {
            Map<String, Object> e = (Map<String, Object>) expectedObj;
            expected = new ExpectedOutputs(
                    intOrNull(e.get("expectedCamperCount")),
                    intOrNull(e.get("expectedWarningCount")),
                    parseExpectedAssertions(e.get("expectedAssertions")),
                    (Map<String, Integer>) e.get("expectedHeaderCounts")
            );
        }

        return new YamlPreset(name, description, camperFile, activityFile, session, features, expected);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ExpectedAssertion> parseExpectedAssertions(Object raw) {
        if (!(raw instanceof Map)) {
            return null;
        }
        Map<String, Object> entries = (Map<String, Object>) raw;
        Map<String, ExpectedAssertion> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            String id = entry.getKey();
            if (!(entry.getValue() instanceof Map)) {
                throw new IllegalArgumentException(
                        "expectedAssertions['" + id + "'] must be a map with at least a 'status' key");
            }
            Map<String, Object> fields = (Map<String, Object>) entry.getValue();
            Object statusObj = fields.get("status");
            if (statusObj == null) {
                throw new IllegalArgumentException(
                        "expectedAssertions['" + id + "'] is missing required 'status' field");
            }
            AssertionResult.Status status;
            try {
                status = AssertionResult.Status.valueOf(statusObj.toString().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "expectedAssertions['" + id + "'].status '" + statusObj + "' is not PASSED, FAILED, or SKIPPED");
            }
            Integer failureCount = intOrNull(fields.get("failureCount"));
            result.put(id, new ExpectedAssertion(status, failureCount));
        }
        return result;
    }

    private static String stringField(Map<String, Object> map, String key, boolean required) {
        Object v = map.get(key);
        if (v == null) {
            if (required) {
                throw new IllegalArgumentException("Preset missing required field '" + key + "'");
            }
            return null;
        }
        return v.toString();
    }

    private static Integer intOrNull(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : null;
    }
}

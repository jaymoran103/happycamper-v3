package com.echo.assertion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.CampConfig;
import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.FeatureRegistration;
import com.echo.feature.FeatureRegistry;
import com.echo.feature.RosterFeature;
import com.echo.logging.WarningManager;
import com.echo.feature.EnhancementContext;

class AssertionServiceTest {

    @Test
    @DisplayName("Empty roster: every assertion is SKIPPED because no feature is enabled")
    void disabledFeaturesYieldSkipped() {
        EnhancedRoster roster = new EnhancedRoster();
        AssertionService service = new AssertionService(FeatureRegistry.defaults(CampConfig.defaults()));

        AssertionReport report = service.runAssertions(roster);

        assertEquals(5, report.totalCount(),
                "Default registry should expose all five core assertions (3 activity + 1 preference + 1 swimlevel)");
        assertEquals(0, report.passedCount());
        assertEquals(0, report.failedCount());
        assertEquals(5, report.skippedCount());
        assertTrue(report.allPassed(), "allPassed() should be true when nothing failed");
        for (AssertionResult result : report.getResults()) {
            assertTrue(result.isSkipped());
            assertEquals("Feature not enabled.", result.getSummary());
        }
    }

    @Test
    @DisplayName("Clean roster with all relevant features enabled: every assertion PASSES")
    void cleanRosterPassesAllAssertions() {
        EnhancedRoster roster = buildCleanRoster();

        AssertionReport report = new AssertionService(FeatureRegistry.defaults(CampConfig.defaults()))
                .runAssertions(roster);

        assertEquals(5, report.totalCount());
        assertEquals(5, report.passedCount());
        assertEquals(0, report.failedCount());
        assertEquals(0, report.skippedCount());
    }

    @Test
    @DisplayName("Only activity feature enabled: activity-owned assertions evaluate; others SKIPPED")
    void partialEnablementMixesEvaluatedAndSkipped() {
        EnhancedRoster roster = buildCleanRoster();
        // buildCleanRoster enables all three; disable preference and swimlevel by rebuilding.
        EnhancedRoster activityOnly = new EnhancedRoster();
        for (String h : roster.getHeaderMap().keySet()) {
            activityOnly.addHeader(h);
        }
        for (Camper c : roster.getCampers()) {
            activityOnly.addCamper(c);
        }
        activityOnly.enableFeature("activity");

        AssertionReport report = new AssertionService(FeatureRegistry.defaults(CampConfig.defaults()))
                .runAssertions(activityOnly);

        assertEquals(5, report.totalCount());
        // 3 activity assertions evaluated and passed; 2 (preference, swimlevel) skipped.
        assertEquals(3, report.passedCount());
        assertEquals(2, report.skippedCount());
        assertEquals(0, report.failedCount());
        AssertionResult unrequested = findResult(report, "no_unrequested_activities");
        assertTrue(unrequested.isSkipped(),
                "Preference assertion should be SKIPPED when preference feature is not enabled");
        AssertionResult swim = findResult(report, "no_swim_conflicts");
        assertTrue(swim.isSkipped());
    }

    @Test
    @DisplayName("Failing data on an enabled feature surfaces a FAILED result; unrelated assertions still pass")
    void failedAssertionDoesNotMaskOthers() {
        EnhancedRoster roster = buildCleanRoster();
        Camper alice = roster.getCampers().get(0);
        alice.setValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName, "Archery");

        AssertionReport report = new AssertionService(FeatureRegistry.defaults(CampConfig.defaults()))
                .runAssertions(roster);

        AssertionResult unrequested = findResult(report, "no_unrequested_activities");
        assertTrue(unrequested.isFailed());
        assertEquals(1, unrequested.getFailureCount());
        assertTrue(findResult(report, "all_campers_have_max_rounds").isPassed());
        assertTrue(findResult(report, "no_swim_conflicts").isPassed());
        assertFalse(report.allPassed());
    }

    @Test
    @DisplayName("An assertion that throws is reported as FAILED, not propagated")
    void throwingAssertionConvertsToFailed() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(new FeatureRegistration(new BoomFeature(), null, false));

        EnhancedRoster roster = new EnhancedRoster();
        roster.enableFeature(BoomFeature.ID);

        AssertionReport report = new AssertionService(registry).runAssertions(roster);

        assertEquals(1, report.totalCount());
        AssertionResult result = report.getResults().get(0);
        assertTrue(result.isFailed());
        assertNotNull(result.getFailureDetails());
        assertEquals(1, result.getFailureDetails().size());
        assertTrue(result.getFailureDetails().get(0).contains("RuntimeException"),
                "Failure detail should include the exception type");
        assertTrue(result.getFailureDetails().get(0).contains("boom"),
                "Failure detail should include the exception message");
    }

    private static AssertionResult findResult(AssertionReport report, String id) {
        return report.getResults().stream()
                .filter(r -> r.getAssertionId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for assertion id: " + id));
    }

    private static EnhancedRoster buildCleanRoster() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.ROUND_1.standardName);
        roster.addHeader(RosterHeader.ROUND_2.standardName);
        roster.addHeader(RosterHeader.ROUND_3.standardName);
        roster.addHeader(RosterHeader.ROUND_COUNT.standardName);
        roster.addHeader(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);
        roster.addHeader(RosterHeader.SWIMCONFLICTS.standardName);

        roster.addCamper(makeCamper("Alice", "Apple", "Sailing", "Archery", "Crafts"));
        roster.addCamper(makeCamper("Bob", "Berry", "Soccer", "Sailing", "Archery"));

        roster.enableFeature("activity");
        roster.enableFeature("preference");
        roster.enableFeature("swimlevel");
        return roster;
    }

    private static Camper makeCamper(String firstName, String lastName, String r1, String r2, String r3) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.standardName, firstName);
        data.put(RosterHeader.LAST_NAME.standardName, lastName);
        data.put(RosterHeader.ROUND_1.standardName, r1);
        data.put(RosterHeader.ROUND_2.standardName, r2);
        data.put(RosterHeader.ROUND_3.standardName, r3);
        data.put(RosterHeader.ROUND_COUNT.standardName, "3");
        data.put(RosterHeader.UNREQUESTED_ACTIVITIES.standardName, "");
        data.put(RosterHeader.SWIMCONFLICTS.standardName, "");
        return new Camper(firstName + "_" + lastName, data);
    }

    /**
     * Fake feature with a single assertion that always throws — exercises the
     * exception-to-FAILED branch in AssertionService.evaluateSafely.
     */
    private static class BoomFeature implements RosterFeature {
        static final String ID = "boom";

        @Override public String getFeatureId() { return ID; }
        @Override public String getFeatureName() { return "Boom"; }
        @Override public List<String> getRequiredHeaders() { return List.of(); }
        @Override public List<String> getAddedHeaders() { return List.of(); }
        @Override public Map<String, String> getRequiredFormats() { return Map.of(); }
        @Override public void applyFeature(EnhancementContext context) {}
        @Override public boolean preValidate(EnhancedRoster roster, WarningManager warningManager) { return true; }
        @Override public boolean postValidate(EnhancedRoster roster, WarningManager warningManager) { return true; }

        @Override
        public List<RosterAssertion> getAssertions() {
            return List.of(new RosterAssertion() {
                @Override public String getAssertionId() { return "boom_assertion"; }
                @Override public String getAssertionName() { return "Boom"; }
                @Override public String getDescription() { return "Always throws."; }
                @Override
                public AssertionResult evaluate(EnhancedRoster roster) {
                    throw new RuntimeException("boom");
                }
            });
        }
    }
}

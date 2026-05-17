package com.echo.assertion;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;

class AssertionServiceTest {

    @Test
    @DisplayName("Service returns one result per registered assertion, regardless of applicability")
    void assertionServiceCoversAllRegisteredAssertions() {
        EnhancedRoster roster = new EnhancedRoster();
        // No features enabled — every assertion should be SKIPPED but still reported.
        AssertionService service = new AssertionService(AssertionRegistry.defaults());

        AssertionReport report = service.runAssertions(roster);

        assertEquals(5, report.totalCount(), "Default registry should expose all five core assertions");
        assertEquals(0, report.passedCount());
        assertEquals(0, report.failedCount());
        assertEquals(5, report.skippedCount());
        assertTrue(report.allPassed(), "allPassed() should be true when there are no failures");
    }

    @Test
    @DisplayName("Clean roster with all features enabled passes every applicable assertion")
    void cleanRosterPassesAllAssertions() {
        EnhancedRoster roster = buildCleanRoster();

        AssertionReport report = new AssertionService(AssertionRegistry.defaults()).runAssertions(roster);

        assertEquals(5, report.totalCount());
        assertEquals(5, report.passedCount(), "Every assertion should pass on a clean roster");
        assertEquals(0, report.failedCount());
        assertTrue(report.allPassed());
    }

    @Test
    @DisplayName("Missing round and unrequested activity surface as separate failures")
    void problemsAreReportedSeparately() {
        EnhancedRoster roster = buildCleanRoster();
        Camper alice = roster.getCampers().get(0);
        // Alice is short one round and was assigned something she didn't request.
        alice.setValue(RosterHeader.ROUND_3.standardName, DataConstants.EMPTY_VALUE);
        alice.setValue(RosterHeader.ROUND_COUNT.standardName, "2");
        alice.setValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName, "Archery");

        AssertionReport report = new AssertionService(AssertionRegistry.defaults()).runAssertions(roster);

        AssertionResult roundsResult = findResult(report, "all_campers_have_max_rounds");
        AssertionResult unrequestedResult = findResult(report, "no_unrequested_activities");

        assertTrue(roundsResult.isFailed(), "Round-count assertion should fail when a camper is short a round");
        assertEquals(1, roundsResult.getFailureCount());

        assertTrue(unrequestedResult.isFailed(), "Unrequested-activity assertion should fail");
        assertEquals(1, unrequestedResult.getFailureCount());

        // Unrelated assertions should remain PASSED.
        assertTrue(findResult(report, "no_swim_conflicts").isPassed());
        assertTrue(findResult(report, "round_counts_numeric").isPassed());
        assertFalse(report.allPassed());
    }

    @Test
    @DisplayName("Duplicate activity across rounds is detected")
    void duplicateActivityDetected() {
        EnhancedRoster roster = buildCleanRoster();
        Camper alice = roster.getCampers().get(0);
        alice.setValue(RosterHeader.ROUND_2.standardName, "Sailing"); // same as ROUND_1

        AssertionReport report = new AssertionService(AssertionRegistry.defaults()).runAssertions(roster);

        AssertionResult dup = findResult(report, "no_duplicate_round_entries");
        assertTrue(dup.isFailed());
        assertEquals(1, dup.getFailureCount());
        assertTrue(dup.getFailureDetails().get(0).contains("Sailing"));
    }

    @Test
    @DisplayName("Non-numeric round count is flagged")
    void nonNumericRoundCountFlagged() {
        EnhancedRoster roster = buildCleanRoster();
        roster.getCampers().get(1).setValue(RosterHeader.ROUND_COUNT.standardName, "three");

        AssertionReport report = new AssertionService(AssertionRegistry.defaults()).runAssertions(roster);

        AssertionResult numeric = findResult(report, "round_counts_numeric");
        assertTrue(numeric.isFailed());
        assertEquals(1, numeric.getFailureCount());
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
}

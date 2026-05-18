package com.echo.assertion.checks;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.echo.assertion.AssertionResult;
import com.echo.domain.Camper;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;

class NoDuplicateRoundEntriesAssertionTest {

    private final NoDuplicateRoundEntriesAssertion assertion = new NoDuplicateRoundEntriesAssertion();

    @Test
    @DisplayName("Passes when every round has a distinct activity per camper")
    void passesWhenAllRoundsAreDistinct() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "Sailing", "Archery", "Crafts"));
        roster.addCamper(makeCamper("Bob", "Berry", "Soccer", "Sailing", "Archery"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed());
        assertEquals(2, result.getCheckedCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("Fails when a camper has the same activity in two rounds, case-insensitive")
    void failsOnCaseInsensitiveDuplicate() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "Sailing", "sailing", "Crafts"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(1, result.getFailureCount());
        String detail = result.getFailureDetails().get(0);
        assertTrue(detail.contains("Alice"));
        assertTrue(detail.toLowerCase().contains("sailing"));
        assertTrue(detail.contains("1") && detail.contains("2"),
                "Failure detail should call out both round numbers");
    }

    @Test
    @DisplayName("Passes when an empty round is present (empty cells do not count as duplicates)")
    void passesWhenARoundIsEmpty() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Charlie", "Cherry", "Sailing", "", "Archery"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed(),
                "Empty round entries should not trigger duplicate detection");
    }

    private static EnhancedRoster buildRoster() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.ROUND_1.standardName);
        roster.addHeader(RosterHeader.ROUND_2.standardName);
        roster.addHeader(RosterHeader.ROUND_3.standardName);
        return roster;
    }

    private static Camper makeCamper(String firstName, String lastName, String r1, String r2, String r3) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.standardName, firstName);
        data.put(RosterHeader.LAST_NAME.standardName, lastName);
        data.put(RosterHeader.ROUND_1.standardName, r1);
        data.put(RosterHeader.ROUND_2.standardName, r2);
        data.put(RosterHeader.ROUND_3.standardName, r3);
        return new Camper(firstName + "_" + lastName, data);
    }
}

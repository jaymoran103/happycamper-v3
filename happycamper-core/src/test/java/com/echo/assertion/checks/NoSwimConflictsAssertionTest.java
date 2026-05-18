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

class NoSwimConflictsAssertionTest {

    private final NoSwimConflictsAssertion assertion = new NoSwimConflictsAssertion();

    @Test
    @DisplayName("Passes when every camper has an empty SWIMCONFLICTS value")
    void passesWhenAllEmpty() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", ""));
        roster.addCamper(makeCamper("Bob", "Berry", ""));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed());
        assertEquals(2, result.getCheckedCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("Fails when any camper has a non-empty SWIMCONFLICTS value")
    void failsWhenAnyCamperHasConflict() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "Sailing"));
        roster.addCamper(makeCamper("Bob", "Berry", ""));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(2, result.getCheckedCount());
        assertEquals(1, result.getFailureCount());
        String detail = result.getFailureDetails().get(0);
        assertTrue(detail.contains("Alice"));
        assertTrue(detail.contains("Sailing"));
    }

    private static EnhancedRoster buildRoster() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.SWIMCONFLICTS.standardName);
        return roster;
    }

    private static Camper makeCamper(String firstName, String lastName, String conflicts) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.standardName, firstName);
        data.put(RosterHeader.LAST_NAME.standardName, lastName);
        data.put(RosterHeader.SWIMCONFLICTS.standardName, conflicts);
        return new Camper(firstName + "_" + lastName, data);
    }
}

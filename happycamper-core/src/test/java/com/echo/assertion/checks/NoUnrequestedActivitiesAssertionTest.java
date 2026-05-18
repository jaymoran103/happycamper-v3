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

class NoUnrequestedActivitiesAssertionTest {

    private final NoUnrequestedActivitiesAssertion assertion = new NoUnrequestedActivitiesAssertion();

    @Test
    @DisplayName("Passes when every camper has an empty UNREQUESTED_ACTIVITIES value")
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
    @DisplayName("Fails when any camper has a non-empty UNREQUESTED_ACTIVITIES value")
    void failsWhenAnyCamperHasUnrequested() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "Archery"));
        roster.addCamper(makeCamper("Bob", "Berry", ""));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(2, result.getCheckedCount());
        assertEquals(1, result.getFailureCount());
        String detail = result.getFailureDetails().get(0);
        assertTrue(detail.contains("Alice"));
        assertTrue(detail.contains("Archery"));
    }

    private static EnhancedRoster buildRoster() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);
        return roster;
    }

    private static Camper makeCamper(String firstName, String lastName, String unrequested) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.standardName, firstName);
        data.put(RosterHeader.LAST_NAME.standardName, lastName);
        data.put(RosterHeader.UNREQUESTED_ACTIVITIES.standardName, unrequested);
        return new Camper(firstName + "_" + lastName, data);
    }
}

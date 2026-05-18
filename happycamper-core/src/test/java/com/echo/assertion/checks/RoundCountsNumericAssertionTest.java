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

class RoundCountsNumericAssertionTest {

    private final RoundCountsNumericAssertion assertion = new RoundCountsNumericAssertion();

    @Test
    @DisplayName("Passes when every ROUND_COUNT is a valid non-negative integer")
    void passesOnNumericCounts() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "3"));
        roster.addCamper(makeCamper("Bob", "Berry", "0"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed());
        assertEquals(2, result.getCheckedCount());
    }

    @Test
    @DisplayName("Passes when ROUND_COUNT is empty (deferred, not an error here)")
    void passesOnEmptyCount() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", ""));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("Fails when ROUND_COUNT is non-numeric")
    void failsOnNonNumericCount() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Charlie", "Cherry", "three"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).contains("three"));
    }

    @Test
    @DisplayName("Fails when ROUND_COUNT is a negative integer")
    void failsOnNegativeCount() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Diana", "Date", "-1"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).contains("-1"));
    }

    private static EnhancedRoster buildRoster() {
        EnhancedRoster roster = new EnhancedRoster();
        roster.addHeader(RosterHeader.FIRST_NAME.standardName);
        roster.addHeader(RosterHeader.LAST_NAME.standardName);
        roster.addHeader(RosterHeader.ROUND_COUNT.standardName);
        return roster;
    }

    private static Camper makeCamper(String firstName, String lastName, String roundCount) {
        Map<String, String> data = new HashMap<>();
        data.put(RosterHeader.FIRST_NAME.standardName, firstName);
        data.put(RosterHeader.LAST_NAME.standardName, lastName);
        data.put(RosterHeader.ROUND_COUNT.standardName, roundCount);
        return new Camper(firstName + "_" + lastName, data);
    }
}

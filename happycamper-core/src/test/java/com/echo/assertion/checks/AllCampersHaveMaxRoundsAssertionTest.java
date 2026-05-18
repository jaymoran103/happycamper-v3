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

class AllCampersHaveMaxRoundsAssertionTest {

    private final AllCampersHaveMaxRoundsAssertion assertion = new AllCampersHaveMaxRoundsAssertion();

    @Test
    @DisplayName("Passes when every camper has the full round count")
    void passesWhenAllCampersHaveMaxRounds() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "3"));
        roster.addCamper(makeCamper("Bob", "Berry", "3"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed());
        assertEquals(2, result.getCheckedCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("Fails when a camper has fewer than MAX_ROUNDS rounds assigned")
    void failsWhenCamperHasFewerRounds() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Alice", "Apple", "2"));
        roster.addCamper(makeCamper("Bob", "Berry", "3"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(2, result.getCheckedCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).contains("Alice"));
    }

    @Test
    @DisplayName("Fails when a camper is missing ROUND_COUNT entirely (empty string)")
    void failsWhenRoundCountIsEmpty() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Charlie", "Cherry", ""));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isFailed());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getFailureDetails().get(0).contains("no rounds assigned"));
    }

    @Test
    @DisplayName("Defers non-numeric ROUND_COUNT to RoundCountsNumericAssertion (passes here)")
    void deferNonNumericToOtherAssertion() {
        EnhancedRoster roster = buildRoster();
        roster.addCamper(makeCamper("Diana", "Date", "three"));

        AssertionResult result = assertion.evaluate(roster);

        assertTrue(result.isPassed(),
                "Non-numeric values are out of scope here; this check should pass when no campers are short on rounds");
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

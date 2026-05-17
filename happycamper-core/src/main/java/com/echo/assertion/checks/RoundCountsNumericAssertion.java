package com.echo.assertion.checks;

import java.util.ArrayList;
import java.util.List;

import com.echo.assertion.AssertionResult;
import com.echo.assertion.RosterAssertion;
import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;

/**
 * Verifies that every camper's {@link RosterHeader#ROUND_COUNT} value either is empty
 * or parses cleanly as a non-negative integer. A non-numeric value would indicate
 * that ActivityFeature wrote unexpected data (or that downstream code corrupted it),
 * which would cascade into UI sorting and assertion logic.
 */
public class RoundCountsNumericAssertion implements RosterAssertion {

    private static final String ID = "round_counts_numeric";
    private static final String NAME = "Round counts are numeric";

    @Override
    public String getAssertionId() { return ID; }

    @Override
    public String getAssertionName() { return NAME; }

    @Override
    public String getDescription() {
        return "Every camper's '" + RosterHeader.ROUND_COUNT.standardName + "' value should be a non-negative integer.";
    }

    @Override
    public boolean isApplicable(EnhancedRoster roster) {
        return roster.hasFeature("activity");
    }

    @Override
    public AssertionResult evaluate(EnhancedRoster roster) {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Camper camper : roster.getCampers()) {
            checked++;
            String value = camper.getValue(RosterHeader.ROUND_COUNT.standardName);
            if (DataConstants.isEmpty(value)) {
                continue;
            }
            try {
                int count = Integer.parseInt(value.trim());
                if (count < 0) {
                    failures.add(describe(camper) + ": negative round count '" + value + "'");
                }
            } catch (NumberFormatException e) {
                failures.add(describe(camper) + ": non-numeric round count '" + value + "'");
            }
        }
        if (failures.isEmpty()) {
            return AssertionResult.passed(ID, NAME, checked,
                    "All " + checked + " campers have numeric round counts.");
        }
        return AssertionResult.failed(ID, NAME, checked, failures,
                failures.size() + " of " + checked + " campers have non-numeric round counts.");
    }

    private static String describe(Camper camper) {
        String first = camper.getValue(RosterHeader.FIRST_NAME.standardName);
        String last = camper.getValue(RosterHeader.LAST_NAME.standardName);
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? camper.getId() : name;
    }
}

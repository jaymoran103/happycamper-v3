package com.echo.assertion.checks;

import java.util.ArrayList;
import java.util.List;

import com.echo.assertion.AssertionResult;
import com.echo.assertion.RosterAssertion;
import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.ActivityFeature;

/**
 * Verifies that every camper has been assigned the full set of activity rounds
 * ({@link ActivityFeature#MAX_ROUNDS}). Fails when any camper's
 * {@link RosterHeader#ROUND_COUNT} value is missing or below the maximum.
 */
public class AllCampersHaveMaxRoundsAssertion implements RosterAssertion {

    private static final String ID = "all_campers_have_max_rounds";
    private static final String NAME = "All campers have full round assignments";

    @Override
    public String getAssertionId() { return ID; }

    @Override
    public String getAssertionName() { return NAME; }

    @Override
    public String getDescription() {
        return "Every camper should have an assignment in each of the " + ActivityFeature.MAX_ROUNDS + " rounds.";
    }

    @Override
    public AssertionResult evaluate(EnhancedRoster roster) {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Camper camper : roster.getCampers()) {
            checked++;
            String value = camper.getValue(RosterHeader.ROUND_COUNT.standardName);
            int count;
            if (DataConstants.isEmpty(value)) {
                failures.add(describe(camper) + ": no rounds assigned");
                continue;
            }
            try {
                count = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // Numeric format is the concern of RoundCountsNumericAssertion; skip here.
                continue;
            }
            if (count < ActivityFeature.MAX_ROUNDS) {
                failures.add(describe(camper) + ": " + count + "/" + ActivityFeature.MAX_ROUNDS + " rounds assigned");
            }
        }
        if (failures.isEmpty()) {
            return AssertionResult.passed(ID, NAME, checked,
                    "All " + checked + " campers have " + ActivityFeature.MAX_ROUNDS + " rounds assigned.");
        }
        return AssertionResult.failed(ID, NAME, checked, failures,
                failures.size() + " of " + checked + " campers are missing one or more round assignments.");
    }

    private static String describe(Camper camper) {
        String first = camper.getValue(RosterHeader.FIRST_NAME.standardName);
        String last = camper.getValue(RosterHeader.LAST_NAME.standardName);
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? camper.getId() : name;
    }
}

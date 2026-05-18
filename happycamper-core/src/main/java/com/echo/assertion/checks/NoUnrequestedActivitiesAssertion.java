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
 * Verifies that no camper has been assigned an activity they did not request.
 * Reads the {@link RosterHeader#UNREQUESTED_ACTIVITIES} column produced by
 * PreferenceFeature — a non-empty value indicates one or more unrequested activities.
 */
public class NoUnrequestedActivitiesAssertion implements RosterAssertion {

    private static final String ID = "no_unrequested_activities";
    private static final String NAME = "No unrequested activities";

    @Override
    public String getAssertionId() { return ID; }

    @Override
    public String getAssertionName() { return NAME; }

    @Override
    public String getDescription() {
        return "Every assigned activity should appear in the camper's preference list.";
    }

    @Override
    public AssertionResult evaluate(EnhancedRoster roster) {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Camper camper : roster.getCampers()) {
            checked++;
            String unrequested = camper.getValue(RosterHeader.UNREQUESTED_ACTIVITIES.standardName);
            if (!DataConstants.isEmpty(unrequested)) {
                failures.add(describe(camper) + ": " + unrequested);
            }
        }
        if (failures.isEmpty()) {
            return AssertionResult.passed(ID, NAME, checked,
                    "No unrequested activities across " + checked + " campers.");
        }
        return AssertionResult.failed(ID, NAME, checked, failures,
                failures.size() + " of " + checked + " campers have unrequested activities.");
    }

    private static String describe(Camper camper) {
        String first = camper.getValue(RosterHeader.FIRST_NAME.standardName);
        String last = camper.getValue(RosterHeader.LAST_NAME.standardName);
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? camper.getId() : name;
    }
}

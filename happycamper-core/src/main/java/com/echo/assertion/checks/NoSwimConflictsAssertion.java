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
 * Verifies that no camper has been assigned a water activity that exceeds their swim level.
 * Reads the {@link RosterHeader#SWIMCONFLICTS} column produced by SwimLevelFeature — a
 * non-empty value indicates one or more swim-restricted activities.
 */
public class NoSwimConflictsAssertion implements RosterAssertion {

    private static final String ID = "no_swim_conflicts";
    private static final String NAME = "No swim-level conflicts";

    @Override
    public String getAssertionId() { return ID; }

    @Override
    public String getAssertionName() { return NAME; }

    @Override
    public String getDescription() {
        return "No camper should be assigned a water activity exceeding their swim level.";
    }

    @Override
    public AssertionResult evaluate(EnhancedRoster roster) {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Camper camper : roster.getCampers()) {
            checked++;
            String conflicts = camper.getValue(RosterHeader.SWIMCONFLICTS.standardName);
            if (!DataConstants.isEmpty(conflicts)) {
                failures.add(describe(camper) + ": " + conflicts);
            }
        }
        if (failures.isEmpty()) {
            return AssertionResult.passed(ID, NAME, checked,
                    "No swim conflicts across " + checked + " campers.");
        }
        return AssertionResult.failed(ID, NAME, checked, failures,
                failures.size() + " of " + checked + " campers have swim-level conflicts.");
    }

    private static String describe(Camper camper) {
        String first = camper.getValue(RosterHeader.FIRST_NAME.standardName);
        String last = camper.getValue(RosterHeader.LAST_NAME.standardName);
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? camper.getId() : name;
    }
}

package com.echo.assertion.checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.echo.assertion.AssertionResult;
import com.echo.assertion.RosterAssertion;
import com.echo.domain.Camper;
import com.echo.domain.DataConstants;
import com.echo.domain.EnhancedRoster;
import com.echo.domain.RosterHeader;
import com.echo.feature.ActivityFeature;

/**
 * Verifies that no camper has been assigned the same activity to two different rounds.
 * A duplicate suggests the activity-roster CSV contained conflicting entries or that the
 * ActivityFeature merge logic mis-assigned a row.
 */
public class NoDuplicateRoundEntriesAssertion implements RosterAssertion {

    private static final String ID = "no_duplicate_round_entries";
    private static final String NAME = "No duplicate round assignments";

    private static final RosterHeader[] ROUND_HEADERS = {
            RosterHeader.ROUND_1,
            RosterHeader.ROUND_2,
            RosterHeader.ROUND_3,
    };

    @Override
    public String getAssertionId() { return ID; }

    @Override
    public String getAssertionName() { return NAME; }

    @Override
    public String getDescription() {
        return "No camper should be assigned the same activity in more than one round.";
    }

    @Override
    public AssertionResult evaluate(EnhancedRoster roster) {
        // Defensive: if MAX_ROUNDS ever exceeds the hardcoded ROUND_1/2/3 enum constants,
        // the check would silently skip later rounds. Phase 6's HDR-05 makes round headers
        // dynamic; until then this is a runtime safeguard.
        int roundsToCheck = Math.min(ActivityFeature.MAX_ROUNDS, ROUND_HEADERS.length);

        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Camper camper : roster.getCampers()) {
            checked++;
            Map<String, Integer> activityToRound = new HashMap<>();
            for (int i = 0; i < roundsToCheck; i++) {
                String activity = camper.getValue(ROUND_HEADERS[i].standardName);
                if (DataConstants.isEmpty(activity)) {
                    continue;
                }
                String key = activity.trim().toLowerCase();
                Integer earlierRound = activityToRound.put(key, i + 1);
                if (earlierRound != null) {
                    failures.add(describe(camper) + ": '" + activity + "' assigned in rounds "
                            + earlierRound + " and " + (i + 1));
                }
            }
        }
        if (failures.isEmpty()) {
            return AssertionResult.passed(ID, NAME, checked,
                    "No duplicate round assignments across " + checked + " campers.");
        }
        return AssertionResult.failed(ID, NAME, checked, failures,
                failures.size() + " duplicate round assignment(s) detected.");
    }

    private static String describe(Camper camper) {
        String first = camper.getValue(RosterHeader.FIRST_NAME.standardName);
        String last = camper.getValue(RosterHeader.LAST_NAME.standardName);
        String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
        return name.isEmpty() ? camper.getId() : name;
    }
}

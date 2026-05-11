package com.echo.feature;

import com.echo.domain.ActivityRoster;
import com.echo.domain.EnhancedRoster;
import com.echo.logging.WarningManager;

/**
 * Bundle of inputs passed to {@link RosterFeature#applyFeature(EnhancementContext)}.
 *
 * Carries the enhanced roster, the optional activity roster, and the warning manager so
 * a single interface method serves every feature. This includes ActivityFeature, which
 * previously required a special-cased overload and an {@code instanceof} branch in
 * {@code RosterService}.
 *
 * The activity roster is nullable. Features that do not need it should ignore it.
 * ActivityFeature requires it and validates accordingly.
 */
public final class EnhancementContext {
    private final EnhancedRoster roster;
    private final ActivityRoster activityRoster;
    private final WarningManager warningManager;

    public EnhancementContext(EnhancedRoster roster, ActivityRoster activityRoster, WarningManager warningManager) {
        this.roster = roster;
        this.activityRoster = activityRoster;
        this.warningManager = warningManager;
    }

    public EnhancedRoster getRoster() {
        return roster;
    }

    public ActivityRoster getActivityRoster() {
        return activityRoster;
    }

    public WarningManager getWarningManager() {
        return warningManager;
    }
}

package com.echo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable configuration object for the core processing pipeline.
 * This class has no framework dependencies and is safe to use across
 * both the desktop and web modules.
 * 
 * Carries all feature-level configuration that was previously spread across
 * mutable static fields in feature classes and DataConstants. 
 * 
 * Instances are constructed via {@link Builder} or via the {@link #defaults()} factory.
 *
 */
public final class CampConfig {

    // ----- Core config definitions -----
    private final List<ProgramDefinition> programDefinitions;

    // ----- ActivityFeature config -----
    private final boolean includeOrphans;

    // ----- PreferenceFeature config -----
    private final List<String> exemptActivities;

    // ----- MedicalFeature config -----
    private final boolean warnOnMissingMedical;

    // ----- SwimLevelFeature config -----
    // NOTE: Not currently implemented, but will be added when full config implementation and persistence are worked out.
    // Current swim level config maps integers to level names used in source data. 
    // String array should be sufficient as long as the input system validates the given structure
    // private final String[] swimLevelMappings;

    private CampConfig(Builder builder) {
        this.includeOrphans        = builder.includeOrphans;
        this.warnOnMissingMedical  = builder.warnOnMissingMedical;
        this.exemptActivities      = Collections.unmodifiableList(new ArrayList<>(builder.exemptActivities));
        this.programDefinitions    = Collections.unmodifiableList(new ArrayList<>(builder.programDefinitions));
    }

    /**
     * Returns a {@code CampConfig} populated with factory defaults.
     * Suitable for all current use-cases; callers that need non-default
     * settings should use {@link Builder} directly.
     */
    public static CampConfig defaults() {
        return new Builder().build();
    }

    /** Whether unmatched activity rows should be added to the roster as orphaned campers. */
    public boolean isIncludeOrphans() {
        return includeOrphans;
    }

    /**
     * Activities that are exempt from preference scoring (e.g. assigned by
     * sign-up rather than through the preference system).
     */
    public List<String> getExemptActivities() {
        return exemptActivities;
    }

    /**
     * Program name definitions used by {@code ProgramNameAdjuster}.
     * Will be populated once FEAT-06 is implemented in Phase 7.
     */
    public List<ProgramDefinition> getProgramDefinitions() {
        return programDefinitions;
    }


    /** Whether a warning should be logged when a camper has no medical notes. */
    public boolean isWarnOnMissingMedical() {
        return warnOnMissingMedical;
    }
    
    /**
     * Builder for constructing custom {@link CampConfig} instances.
     *
     * <pre>{@code
     * CampConfig config = new CampConfig.Builder()
     *     .includeOrphans(false)
     *     .warnOnMissingMedical(true)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private boolean includeOrphans       = true;
        private boolean warnOnMissingMedical = false;
        private List<String> exemptActivities = new ArrayList<>(List.of("Swimming", "Horseback Riding"));
        private List<ProgramDefinition> programDefinitions = new ArrayList<>();

        public Builder includeOrphans(boolean val) {
            this.includeOrphans = val;
            return this;
        }

        public Builder exemptActivities(List<String> val) {
            this.exemptActivities = new ArrayList<>(val);
            return this;
        }

        public Builder programDefinitions(List<ProgramDefinition> val) {
            this.programDefinitions = new ArrayList<>(val);
            return this;
        }
        
        public Builder warnOnMissingMedical(boolean val) {
            this.warnOnMissingMedical = val;
            return this;
        }

        public CampConfig build() {
            return new CampConfig(this);
        }
    }


    /**
     * Defines a named camp program and its known aliases.
     *
     * Used by {@code ProgramNameAdjuster} to standardize programs that
     * appear with different name variants across source data.
     * 
     * This class is a placeholder for the later migration of {@code ReplacementPairs} 
     * from a hard-coded enum to config-driven definitions.
     * 
     * FUTURE: Consider offering regex-based name matching for more flexible alias 
     * definitions, matching current approach
     */
    public static final class ProgramDefinition {
        private final String canonicalName;
        private final List<String> aliases;


        public ProgramDefinition(String canonicalName, List<String> aliases) {
            this.canonicalName = canonicalName;
            this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
        }


        /*
        * Get the authoritative name for this program, used internally across the program
        */ 
        public String getCanonicalName() {
            return canonicalName;
        }

        /**
         * Known alternate names or spellings that should be normalized to
         * {@link #getCanonicalName()}.
         */
        public List<String> getAliases() {
            return aliases;
        }
    }
}

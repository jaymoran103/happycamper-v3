package com.echo.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.echo.domain.CampConfig;
import com.echo.filter.AssignmentFilter;
import com.echo.filter.MedicalFilter;
import com.echo.filter.PreferenceFilter;
import com.echo.filter.RosterFilter;
import com.echo.filter.SwimLevelFilter;

/**
 * Single source of truth for which features exist, in what order they run, and which
 * filter (if any) each feature pairs with.
 *
 * Replaces feature registration logic with hardcoded feature lists passed and compared between 
 * {@code RosterService} (feature construction), {@code FilterManager} (filter construction), and
 * {@code ImportDialog} (toggle list). All three consult now consult this registry instead, making
 * feature additions a single-location change.
 *
 * The registry is constructed once per {@code RosterService}. Desktop callers may extend
 * the core defaults via {@link #replace(String, FeatureRegistration)} to plug in
 * Swing-coupled filters (SortedProgramFilter) that cannot live in core.
 */
public class FeatureRegistry {
    private final List<FeatureRegistration> registrations = new ArrayList<>();

    public void register(FeatureRegistration registration) {
        registrations.add(registration);
    }

    /**
     * Replace the registration for the given feature id. Used by the desktop module
     * to attach the Swing-coupled SortedProgramFilter to the core ProgramFeature.
     */
    public void replace(String featureId, FeatureRegistration registration) {
        for (int i = 0; i < registrations.size(); i++) {
            if (registrations.get(i).featureId().equals(featureId)) {
                registrations.set(i, registration);
                return;
            }
        }
        throw new IllegalArgumentException("No registration found for featureId: " + featureId);
    }

    public List<FeatureRegistration> all() {
        return Collections.unmodifiableList(registrations);
    }

    public List<RosterFeature> getFeatures() {
        List<RosterFeature> features = new ArrayList<>(registrations.size());
        for (FeatureRegistration r : registrations) {
            features.add(r.feature());
        }
        return features;
    }

    public Optional<FeatureRegistration> find(String featureId) {
        for (FeatureRegistration r : registrations) {
            if (r.featureId().equals(featureId)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Builds the default registry containing all core features and their core filters.
     * ProgramFeature has no filter factory in core — desktop replaces this entry with
     * one that supplies SortedProgramFilter.
     */
    public static FeatureRegistry defaults(CampConfig config) {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(new FeatureRegistration(new ActivityFeature(config), (Supplier<RosterFilter>) AssignmentFilter::new, true));
        registry.register(new FeatureRegistration(new ProgramFeature(), null, false));
        registry.register(new FeatureRegistration(new PreferenceFeature(config), (Supplier<RosterFilter>) PreferenceFilter::new, false));
        registry.register(new FeatureRegistration(new SwimLevelFeature(), (Supplier<RosterFilter>) SwimLevelFilter::new, false));
        registry.register(new FeatureRegistration(new MedicalFeature(config), (Supplier<RosterFilter>) MedicalFilter::new, false));
        return registry;
    }
}

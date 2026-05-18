package com.echo.preset;

import java.io.File;

/**
 * A reusable launchable scenario: paired CSV inputs + session + feature toggles
 * + optional expected outputs. Implementations include {@link YamlPreset} (loaded
 * from {@code presets/*.yaml}) and {@link TestPresetAdapter} (wrapping the legacy
 * {@code TestPreset} enum).
 */
public interface Preset {

    String getName();

    /** Human-readable description. May be {@code null}. */
    String getDescription();

    /** Resolved camper CSV. Implementations must throw if the file does not exist. */
    File getCamperFile();

    /** Resolved activity CSV. Implementations must throw if the file does not exist. */
    File getActivityFile();

    int getSession();

    /** Feature IDs to enable. Never {@code null}; may be empty. */
    String[] getFeatures();

    /** Optional assertion targets. May be {@code null} (Phase 1 presets typically omit). */
    ExpectedOutputs getExpectedOutputs();
}

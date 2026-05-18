package com.echo.preset;

import com.echo.automation.TestPreset;

import java.io.File;

/**
 * Adapts the legacy {@link TestPreset} enum to the new {@link Preset} interface.
 * Keeps the 93 existing {@code TestPreset} references untouched while letting new
 * code treat enum-based and YAML-based presets uniformly.
 */
public final class TestPresetAdapter implements Preset {

    private final TestPreset delegate;

    public TestPresetAdapter(TestPreset delegate) {
        this.delegate = delegate;
    }

    @Override public String getName() { return delegate.name(); }
    @Override public String getDescription() { return delegate.getDescription(); }
    @Override public File getCamperFile() { return delegate.getCamperFile(); }
    @Override public File getActivityFile() { return delegate.getActivityFile(); }
    @Override public int getSession() { return delegate.getSession(); }
    @Override public String[] getFeatures() { return delegate.getFeatures(); }
    @Override public ExpectedOutputs getExpectedOutputs() { return null; }
}

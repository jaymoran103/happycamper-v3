package com.echo.preset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetLoaderTest {

    @Test
    @DisplayName("loads the demo-small reference preset with all expected fields")
    void loadsDemoSmall() {
        Preset p = PresetLoader.load("demo-small");
        assertEquals("demo-small", p.getName());
        assertNotNull(p.getDescription());
        assertEquals(6, p.getSession());
        assertNotNull(p.getCamperFile());
        assertTrue(p.getCamperFile().exists(), "camper file should resolve to an existing file");
        assertNotNull(p.getActivityFile());
        assertTrue(p.getActivityFile().exists(), "activity file should resolve to an existing file");
        assertEquals(List.of("activity", "program", "preference"), List.of(p.getFeatures()));
        assertNull(p.getExpectedOutputs(), "demo-small omits expectedOutputs");
    }

    @Test
    @DisplayName("loads the bug-repro-template preset (empty features list)")
    void loadsBugReproTemplate() {
        Preset p = PresetLoader.load("bug-repro-template");
        assertEquals("bug-repro-template", p.getName());
        assertEquals(1, p.getSession());
        assertEquals(0, p.getFeatures().length, "bug-repro-template uses empty features list");
        assertTrue(p.getCamperFile().exists());
        assertTrue(p.getActivityFile().exists());
    }

    @Test
    @DisplayName("unknown preset name throws with helpful message listing available presets")
    void unknownPresetThrowsWithAvailableList() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PresetLoader.load("does-not-exist"));
        assertTrue(ex.getMessage().contains("does-not-exist"), "message should name the missing preset");
        assertTrue(ex.getMessage().contains("Available:"), "message should list available presets");
        assertTrue(ex.getMessage().contains("demo-small"), "available list should include shipped presets");
    }

    @Test
    @DisplayName("available() lists all .yaml files under presets/")
    void availableIncludesAllShippedPresets() {
        List<String> names = PresetLoader.available();
        assertTrue(names.contains("demo-small"), "available() should include demo-small");
        assertTrue(names.contains("bug-repro-template"), "available() should include bug-repro-template");
    }

    @Test
    @DisplayName("null or blank preset name is rejected")
    void nullOrBlankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> PresetLoader.load(null));
        assertThrows(IllegalArgumentException.class, () -> PresetLoader.load(""));
        assertThrows(IllegalArgumentException.class, () -> PresetLoader.load("   "));
    }
}

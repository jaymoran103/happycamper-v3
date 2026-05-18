package com.echo.preset;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads {@link Preset} definitions from {@code presets/*.yaml} on the test classpath.
 *
 * <p>Used by {@code HappyCamperPresetLauncher} (test-source entry point in the
 * desktop module) and by {@link PresetLoader#load(String)} callers in tests.
 */
public final class PresetLoader {

    private static final String PRESETS_DIR = "presets";

    private PresetLoader() {}

    /**
     * Loads {@code presets/<name>.yaml} from the classpath.
     *
     * @throws IllegalArgumentException if the preset does not exist; the message
     *         lists available preset names.
     */
    public static Preset load(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Preset name is required");
        }
        String resourcePath = PRESETS_DIR + "/" + name + ".yaml";
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException(
                    "Unknown preset: '" + name + "'. Available: " + available());
        }
        try (InputStream in = url.openStream()) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Map<String, Object> raw = yaml.load(in);
            YamlPreset preset = YamlPreset.fromMap(raw);
            if (!preset.getName().equals(name)) {
                throw new IllegalStateException(
                        "Preset name field '" + preset.getName() + "' does not match filename '" + name + "'");
            }
            return preset;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read preset " + resourcePath, e);
        }
    }

    /** Lists preset names (filename without {@code .yaml}) discoverable on the test classpath. */
    public static List<String> available() {
        List<String> names = new ArrayList<>();
        // Scan target/test-classes/presets/ — the only classpath entry that backs
        // test resources in the Maven build. Resilient to running from either the
        // core module dir or the repo root.
        for (String root : List.of("target/test-classes", "happycamper-core/target/test-classes")) {
            Path dir = Path.of(root, PRESETS_DIR);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> entries = Files.list(dir)) {
                entries.filter(p -> p.toString().endsWith(".yaml"))
                       .forEach(p -> {
                           String fname = p.getFileName().toString();
                           names.add(fname.substring(0, fname.length() - ".yaml".length()));
                       });
                break;
            } catch (IOException ignored) {
                // try the next candidate root
            }
        }
        Collections.sort(names);
        return names;
    }
}

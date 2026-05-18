# ADR-001: Preset loader lives in core test-jar; CLI launcher lives in desktop test sources

**Status:** Accepted (2026-05-17, Sprint 1 Phase 1)

## Context

Phase 1 introduces a YAML-backed preset abstraction so the desktop app can be launched with a named scenario (`-Dhappycamper.preset=demo-small`). A preset declares a camper CSV, an activity CSV, a session number, feature toggles, and (optionally, Phase 3) expected outputs.

The CSV fixtures already live under `happycamper-core/src/test/resources/testRosters/`. The new preset YAMLs naturally belong alongside them under `presets/`. But the launch hook needs to wire the resolved files into `HappyCamper.mainTest(File, File, String[])`, which lives in `happycamper-desktop`.

The constraint that drives this ADR: the production desktop fat-jar (`mvn package -DskipTests`) **must not** ship preset-loader code or YAML fixtures. End users have no use for either, and bundling them mixes test concerns into production deliverables.

## Decision

- `Preset` interface, `YamlPreset`, `ExpectedOutputs`, `TestPresetAdapter`, and `PresetLoader` live in `happycamper-core/src/test/java/com/echo/preset/`. They are published in the existing `tests` classifier jar (`happycamper-core-2.2-tests.jar`) via the already-configured `maven-jar-plugin` test-jar goal.
- Reference preset YAMLs live in `happycamper-core/src/test/resources/presets/` and ride along in the same test-jar.
- The CLI entry point `HappyCamperPresetLauncher` lives in `happycamper-desktop/src/test/java/com/echo/`. It reads `-Dhappycamper.preset=<name>`, loads the preset via `PresetLoader.load(name)`, and delegates to the existing `HappyCamper.mainTest`.
- Production `HappyCamper.main` is unchanged.
- Preset-driven launches require the test classpath. Two equivalent entry points are wired in this phase:
    - `./verify-desktop.sh -j -p <name>` (shell wrapper, the muscle-memory path).
    - `mvn -pl happycamper-desktop exec:java -Dhappycamper.preset=<name>` (the Maven exec target; what CI will use).

## Consequences

- The production fat-jar is byte-identical to today. End users see no preset code or YAML.
- Adding a new preset is a one-file change in `happycamper-core/src/test/resources/presets/`; no recompile of production code.
- The desktop module's test-scope dependency on `happycamper-core` (`<classifier>tests</classifier>`) is the load-bearing wire that puts both `PresetLoader` and the YAMLs on the launcher's classpath.
- Future code reuse for an eventual web preset endpoint (Phase 5+) will require either lifting `Preset` and `PresetLoader` into core main sources, or duplicating the loader in web. That migration is deferred — Phase 1 explicitly keeps web a stub.

## Rejected alternatives

- **Reflection probe in production `HappyCamper.setupApp`** that conditionally invokes the preset loader if it's on the classpath. Rejected: adds production code that exists solely to support tests; risks accumulating dependencies on the probe over time; violates the "desktop is the resilient fallback" principle by mixing test-driven concerns into the main path.
- **Move presets and loader to core main sources.** Rejected: ships dead resources (CSV fixtures, YAML manifests) inside the production fat-jar. Wasteful, and arguably worse — gives end users a partially-functional feature they cannot meaningfully use without bundled test data.

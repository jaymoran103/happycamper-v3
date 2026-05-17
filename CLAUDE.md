# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project shape

HappyCamper is a Maven multi-module Java 22 project (`groupId: com.echo`, version `2.2`) that helps camp leaders analyze CSV roster data. Three modules:

- `happycamper-core` — framework-agnostic plain Java. Domain, features, filters, services, assertions, validation, logging. No Swing, no Spring. Publishes a `tests` classifier jar (`maven-jar-plugin` test-jar goal) so other modules can reuse `automation/*` test fixtures and `ReflectionUtils`.
- `happycamper-desktop` — Swing UI (`com.echo.HappyCamper` main), packaged as a fat-jar with `lib/` sidecar via `maven-dependency-plugin`. Depends on core (+ core's `tests` jar in test scope) and `logback-classic` for the SLF4J runtime.
- `happycamper-web` — Spring Boot 3.3.5 REST layer (`com.echo.web.HappyCamperWebApplication`). Thin transport over core; repackaged via `spring-boot-maven-plugin`.

**Architectural principle: desktop is the resilient fallback.** Shared logic lives in core; web is a thin transport over it. If web breaks or is deferred, desktop still works. When adding a shared feature, put it in core and wire desktop first; web is opportunistic.

Active roadmap lives in `ROADMAP.md` (Sprint 1: desktop test wiring + presets, then CI, then ship the rescued assertion feature to web). Per-phase work is scoped in `-PLANNING/sprint-N/phase-N/` — the kickoff `prompt.md` is the contract for the session; `plan.md`, `notes.md`, and `retro.md` are written as the phase progresses. `-PLANNING/PHASE-PLAN.md` is superseded but kept for historical context. Non-obvious decisions go in `docs/decisions/NNN-title.md` (directory is created on the first ADR). Sprint outcomes are tracked as GitHub Milestones (one per phase).

## Common commands

All commands run from the repo root unless noted. The Maven reactor is rooted at `pom.xml`.

```bash
# Full build (all modules, all tests)
mvn clean verify

# Build a single module with its dependencies (preferred for fast loops)
mvn -pl happycamper-core -am clean verify
mvn -pl happycamper-desktop -am clean verify
mvn -pl happycamper-web -am clean verify

# Skip tests
mvn -pl happycamper-desktop -am clean package -DskipTests

# Run a single test class (Surefire picks up *Test.java)
mvn -pl happycamper-core test -Dtest=RosterServiceTest

# Run a single test method
mvn -pl happycamper-core test -Dtest=RosterServiceTest#someMethodName

# Run the desktop app after packaging
java -jar happycamper-desktop/target/happycamper-desktop-2.2.jar

# Run the Spring Boot web app
mvn -pl happycamper-web -am spring-boot:run
```

Convenience wrappers:
- `./verify-desktop.sh` — builds the desktop fat-jar. `-t` adds `mvn clean test`; `-j` launches the built jar after packaging. `./full-verify-desktop.sh` is `verify-desktop.sh -t -j`.

Note: `README.md` references `cd redo` and a `roster-manager-…` artifact name — both are pre-module-split and stale. Use the multi-module commands above.

## Architecture — what spans multiple files

**Registry-based feature wiring.** The system is built around two parallel single-source-of-truth registries built once per `RosterService`:

- `FeatureRegistry` (`happycamper-core/.../feature/FeatureRegistry.java`) pairs each `RosterFeature` with an optional filter factory (`Supplier<RosterFilter>`) and an `alwaysEnabled` flag. `FeatureRegistry.defaults(CampConfig)` builds the canonical core set (Activity, Program, Preference, SwimLevel, Medical). `ActivityFeature` is `alwaysEnabled=true`; a pre-validation failure there aborts the whole import.
- `AssertionRegistry` (`.../assertion/AssertionRegistry.java`) lists the `RosterAssertion`s that run after enhancement. `AssertionService.runAssertions` evaluates every registered assertion safely (exceptions become FAILED results, inapplicable assertions become SKIPPED).

**Core is Swing-free; the desktop module patches the registry.** `ProgramFeature` is registered in core with `null` filter factory. `HappyCamper.buildDesktopFeatureRegistry()` calls `FeatureRegistry.replace("program", …)` to inject `SortedProgramFilter` (Swing-coupled). The web module uses the default registry as-is via `CoreServicesConfig`. If you add a feature whose filter touches Swing, follow the same pattern — register it in core with a `null` filter and swap in the UI-coupled filter from the desktop module.

**The pipeline.** `RosterService.createEnhancedRoster(...)` exists in both `File` and `InputStream` overloads (web uses the stream form via `MultipartFile`). Both funnel into `applyFeaturesAndFinalize`, which:
1. Normalizes + validates the camper and activity rosters.
2. Builds an `EnhancedRoster` seeded with camper data.
3. Builds an `EnhancementContext` (roster + activity roster + `WarningManager`) and iterates the enabled feature IDs in registry order, calling `preValidate → applyFeature(context) → postValidate` on each.
4. Sorts headers via `RosterHeader.updateHeaderMapOrder`.

`EnhancementContext` exists specifically so every feature including `ActivityFeature` has the same `applyFeature` signature — historically `ActivityFeature` required a special-cased overload and `instanceof` branch. Don't reintroduce that split.

**WarningManager is per-run, not a singleton.** `RosterService` creates a new `WarningManager` at the start of each `createEnhancedRoster` call. Pre/post validation failures, feature errors, and import errors are all logged here. `RosterService.getWarningManager()` is only valid after a run has started. Web's `ProcessController` reads it after the pipeline returns to surface warnings + errors in the response.

**Web layer is intentionally thin.** `ProcessController.process` accepts two multipart CSVs + optional `features` list, runs the pipeline via the stream overload, exports the enriched CSV to a buffer via `ExportService.exportRosterToCSV(..., ByteArrayOutputStream)`, runs assertions, and returns `ProcessResponse` (assertion report DTO + CSV string + flat warning list). DTOs in `com.echo.web.dto` exist to keep core domain types out of the JSON contract.

**Desktop app startup must touch Swing on the EDT.** `HappyCamper.setupApp` constructs services off-EDT, then uses `SwingUtilities.invokeAndWait` to build `MainWindow` on the EDT. Don't move `MainWindow` construction back off-EDT — there was a fix specifically for this (commit `3659f5a`).

## Test conventions

- Surefire picks up `**/*Test.java` (configured in the parent pom). All current tests are `*Test.java`.
- ROADMAP.md Phase 1 plans a Surefire/Failsafe split where `*Test.java` stays unit and `*IT.java` becomes integration. Not yet implemented — when adding tests now, use `*Test.java`.
- Test fixtures live in `happycamper-core/src/test/resources/testRosters/` (mini, demo, basic, merged, newTests subdirs). The `com.echo.automation.*` package (`TestFiles`, `TestFileFinder`, `TestPreset`) provides typed handles to these files and is published in the core test-jar for reuse by desktop tests.
- `TestFileFinder.TEST_RESOURCES_DIR` is hardcoded to `redo/src/test/resources/testRosters` — a pre-module-split path. If you touch that finder, verify it still resolves correctly under the current layout.

## Operating rules

1. **Never commit without explicit user approval.** Draft the message, show `git status` + `git diff --staged`, wait for "yes commit." Applies project-wide, not just to large changes.
2. **Never delete branches, stashes, or files without explicit user approval.** List what you would delete; wait.
3. **Never push to remote without explicit user approval.**
4. When the active phase has a `prompt.md` in `-PLANNING/sprint-N/phase-N/`, treat it as the contract for the session.

# ADR-002: Assertion architecture

**Status:** Accepted (2026-05-18, Sprint 1 Phase 3)

## Context

Phase 3 ships the assertion feature rescued from the `proto-web-setup` branch: a structured pass/fail report that runs after enhancement and surfaces invariants like "no swim conflicts" or "every camper got their max activity rounds." The rescue branch shipped a parallel `AssertionRegistry` that mirrored `FeatureRegistry`'s shape — every assertion gated 1:1 on a single feature via `isApplicable(roster) → roster.hasFeature("foo")`.

HappyCamper's eventual desktop deployment has no re-deploy/rollback path. Silent fallthrough on a broken roster is the real danger; over-aggressive blocking is recoverable later. Phase 3's posture is therefore "loud but not blocking," with a clean upgrade path to blocking when a UI consumer exists.

## Decision

**Assertions are a sub-component of features, not a parallel registry.** `RosterFeature.getAssertions()` is the sole entry point. There is no `AssertionRegistry` class. `FeatureRegistry` remains the only registry; `AssertionService` walks features in registry order, calls `getAssertions()` on each, and collects results. Two registries to express a 1:1 relationship is one too many, and co-locating assertions with their column-writing feature means a feature author owns their own checks.

**`isApplicable` was dropped from `RosterAssertion`.** Feature-enablement gating is intrinsic to the new model — the service evaluates an assertion if its owning feature is enabled, otherwise records SKIPPED. Authors no longer write `roster.hasFeature("foo")` boilerplate. Side effect: if a feature is enabled but didn't populate its column (a feature-internal bug), `evaluate` will throw and `evaluateSafely` converts it to FAILED, surfacing the upstream defect rather than masking it.

**Tri-state result: `PASSED / FAILED / SKIPPED`.** SKIPPED is reserved for "owning feature not enabled" and appears in the report so callers (preset-driven IT, future UI) can pin "this check was correctly skipped." Without SKIPPED in the report, the e2e fixture mechanism couldn't distinguish "feature off" from "assertion missing."

**Exception in `evaluate` → FAILED, not propagated.** `AssertionService.evaluateSafely` catches any `Throwable`, wraps the message as a FAILED result detail, and continues. One broken assertion does not abort the whole report.

**`AssertionResult.MAX_FAILURE_DETAILS = 50`** caps the per-result failure-detail strings. The total `failureCount` is preserved; only the detail-string list is truncated. Prevents a pathological roster from producing megabytes of report text. Documented here so a reviewer reading truncated output doesn't think it's a bug.

**Assertion ids are a stable contract.** Once shipped (e.g., `no_swim_conflicts`, `all_campers_have_max_rounds`), the id string is referenced by preset YAML fixtures and future regression tests. Renaming an id is a breaking change to preset contracts.

**Kept deliberately separate from `WarningManager`.** `WarningManager` is for ad-hoc per-camper warnings raised during enhancement; `AssertionReport` is the structured pass/fail summary after enhancement. Bridging assertion FAILEDs into `WarningManager` is deferred until a UI consumer needs it (the Sprint N+3 DESKTOP-08 overview panel is the natural destination).

**Failure weight is informational in Phase 3.** A FAILED assertion does not block CSV export and does not mark the roster unhealthy. The only Phase 3 surface is one SLF4J `info` log line summarizing total/passed/failed/skipped after each enhancement run. The report is available at the call site in `RosterService`, so a future flag can flip export off on FAILED with a one-line change.

## Consequences

- Feature authors write checks next to the code that produces the columns those checks read — the shortest possible feedback loop.
- The single-registry model forecloses cross-feature assertions (e.g., "no medical conflicts with activities"). None exist today; revisit when one does, via a synthetic owning feature or a new orphan slot.
- Phase 3 ships a report that is observable but inert. Upgrading to blocking export, surfacing FAILEDs in the desktop UI, or routing them through `WarningManager` are all additive changes against the same `AssertionReport` value.
- Preset YAML `expectedOutputs` can pin assertion outcomes by id; the id-stability rule above is what makes those fixtures durable across refactors.

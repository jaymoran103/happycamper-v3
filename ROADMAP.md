# HappyCamper-v3 Roadmap

**Last updated:** 2026-05-16
**Status:** Active — Sprint 1 in planning

This document is the single source of truth for project sequencing. It consolidates the current sprint plan with the outstanding architectural work originally tracked in `-PLANNING/PHASE-PLAN.md` (2026-05-04). PHASE-PLAN.md is superseded by this file once Sprint 1 begins.

---

## Sprint 1 Goal

Land agentic infrastructure on desktop, then ship the parked Phase 4 work as a web-accessible assertion feature — without losing desktop's ability to run standalone.

**Audience:** small group of known users. Moderate rigor: reproducible builds, real CI, light release process.

**Cadence:** focused work, realistically ~1–2 weeks (originally scoped as "days, not weeks" but honest sizing of the phases below puts it higher).

---

## Architectural Principles (load-bearing)

1. **Desktop is the resilient fallback.** Shared features live in `happycamper-core`. Web is a thin transport layer over core. If web breaks or is deferred, desktop still works.
2. **Single-distribution focus while infra forms.** Tooling/test/CI work targets desktop only until the rhythm is proven; web rejoins the matrix once infra is stable.
3. **Plan one level deeper than you can see, no more.** Current phase = concrete tasks. Next phase = bulleted intent. Later phases = a sentence each.

---

## Sprint 1 Phases

### Phase 0 — Rescue & Setup *(half-session, mechanical)*

- [ ] Install GitHub MCP server; confirm `gh` CLI auth
- [ ] Scaffold `.github/ISSUE_TEMPLATE/` (bug, feature; both with `area: core|desktop|web|all` dropdown)
- [ ] Add labels: `area:core`, `area:desktop`, `area:web`, `type:bug`, `type:feat`, `type:infra`
- [ ] Add minimal CODEOWNERS
- [ ] Cleanup pass (low priority): delete `appmod/java-upgrade-20260506153848` branch + `stash@{2}` once confirmed unwanted. Phase-1/2/3 branches can also go since they're merged.

### Phase 1 — Desktop test wiring + presets *(largest engineering phase)*

- Surefire/Failsafe split: `*Test.java` → unit, `*IT.java` → integration
- Preset abstraction (location TBD in planning — core vs desktop). Loadable via `-Dhappycamper.preset=<name>` from `presets/` resources
- Compose with (or cleanly supersede) the existing `automation/*` test utilities — do not duplicate
- Migrate `verify-desktop.sh` flows to preset-driven launches; keep the script as a thin wrapper
- Ship 1–2 reference presets that double as e2e fixtures (e.g. `demo-small`, `bug-repro-template`)

**Planning tools:** plan mode + Plan subagent (this phase warrants both).

### Phase 2 — CI for desktop + core *(templatable)*

- `ci.yml`: `dorny/paths-filter` detects `happycamper-core/**` or root pom changes
- Matrix initially `{module: [desktop]}`. Web slot scaffolded but commented — one-line enable in Phase 4
- `setup-java@v4` (Temurin 22), m2 cache keyed on pom hashes, run `mvn -pl happycamper-core,happycamper-<module> -am verify`
- "All-green" rollup job; mark as required status check in branch protection

**Planning:** plan mode is enough.

### Phase 3 — Land assertion package in core *(first real feature ships)*

- From the rescued Phase 4 branch, take only the **core** changes: `assertion/*`, `RosterService` and `ActivityFeature` edits, `AssertionServiceTest`
- Land via a clean PR with proper tests under the new Failsafe split
- Desktop now exposes/uses assertions (UI integration scope TBD — possibly minimal until Sprint N+3 brings the overview panel)
- **Sprint outcome A:** desktop users get assertions. Web still parked.

### Phase 4 — Bring web back into the matrix

- Land web's Spring Boot scaffolding from the rescued branch: `HappyCamperWebApplication`, `CoreServicesConfig`, DTOs, `application.properties`, root pom updates
- Flip the web slot on in `ci.yml`
- Ensure web builds and tests green in CI before adding any new code

### Phase 5 — Ship web's assertion endpoint *(sprint goal)*

- Finish `ProcessController` + `ProcessControllerTest` from the rescued branch
- Verify the controller composes cleanly with the core assertion service (it should — that's why core landed first)
- Address PHASE-PLAN.md flag **F4** (originally no test task for web endpoint): confirm `ProcessControllerTest` covers real integration paths, not just stubs
- Release tag + GitHub Release with desktop fat-jar attached. Web deployment story TBD (worth a research spike at end of sprint)
- **Sprint outcome B:** web users get the assertion endpoint

### Phase 6 — Next-feature TDD rhythm *(post-sprint, kickoff only in Sprint 1)*

- Use the now-mature loop for whatever's next: spec → failing `*IT.java` → implement → verify → ship
- This is where the infra investment compounds

---

## Tracking Conventions

- **GitHub Milestones = phases.** One milestone per phase above. Issues belong to a milestone.
- **Decision log** at `docs/decisions/NNN-title.md` — one paragraph per non-obvious choice (e.g. "why preset loader lives in core not desktop")
- **Now / Next / Later** — three-line note in this file or a pinned issue, updated weekly
- **Re-plan triggers:**
  - Phase 1 reveals presets need their own module → restructure Phase 1
  - Phase 3 integration breaks something in `RosterService` → reassess assertion design
  - Phase 5 web deployment story is non-trivial → split into its own sprint

---

## Inherited from PHASE-PLAN.md (consolidated)

The earlier `-PLANNING/PHASE-PLAN.md` (generated 2026-05-04 from a 66-task registry) defined Phases 1–7 of the architectural roadmap. The mapping into the new structure:

| PHASE-PLAN.md phase | Status | Where it lives now |
|---|---|---|
| Phase 1 — Module split + cleanup | Complete (merged) | Context only |
| Phase 2 — Eliminate mutable static state | Complete (merged) | Context only |
| Phase 3 — Clean module boundaries + SLF4J + registry | Complete (merged) | Context only |
| Phase 4 — Web Backend MVP + assertions | In progress (rescued branch) | Sprint 1 Phases 3–5 |
| Phase 5 — Web Frontend MVP | Not started | Sprint N+1 (below) |
| Phase 6 — Header System Overhaul | Not started | Sprint N+2 (below) |
| Phase 7 — Config persistence + Desktop UX + supplemental features | Not started | Sprint N+3 (below) |

### Post-Sprint-1 Roadmap (carried forward)

These sprints are intentionally underspecified — replan each one when its turn arrives, using the relevant PHASE-PLAN.md sections as the starting point.

#### Sprint N+1 — Web Frontend MVP

Brings the web app to non-technical users. Gated on **DEC-05** (frontend tech choice).

- WEB-05: Upload page (file form, feature toggle checkboxes, submit)
- WEB-06: Results page (assertion summary primary; enriched CSV download)
- Acceptance: a user opens the app in a browser, uploads two CSVs, optionally selects features, and gets an assertion report + downloadable CSV. No auth.

#### Sprint N+2 — Header System Overhaul *(debt reduction, no user-visible value)*

Replaces the overloaded `RosterHeader` enum with a four-layer system. Largest mechanical change in the project (~33 files in HDR-04). Both desktop and web test suites must exist before starting (which they will, post-Sprint-1).

- HDR-01: `HeaderId` enum (canonical field identity)
- HDR-02: `HeaderMetadata` + `HeaderRegistry` with `DisplayRole`
- HDR-03: `CamperRosterSchema` + `ActivityRosterSchema`
- HDR-04: Migrate Camper map keys to `HeaderId.name()` canonical keys *(largest single change)*
- HDR-05: `RoundHeaderUtils` + dynamic round header generation
- HDR-06: Retire `RosterHeader` enum

**Safest abort point:** between HDR-03 and HDR-04. Sequence is add new → migrate consumers → validate per subsystem → delete old enum.

#### Sprint N+3 — Config Persistence + Desktop Enhancements + Supplemental Features

The big "fleshing out the user experience" sprint. Several independent threads — could split into multiple smaller sprints if needed.

**Config persistence (gated on DEC-01, DEC-02):**
- FEAT-05: `ConfigRepository` interface in core
- DESKTOP-02: JSON file `ConfigRepository` for desktop (`~/.happycamper/config.json`)
- WEB-03: `ConfigRepository` for web module
- DESKTOP-03: `CampConfig` settings dialog (tabbed: Activities, Programs, General)

**Data-driven program logic:**
- FEAT-06: Make `ProgramNameAdjuster` data-driven from `CampConfig.ProgramDefinition`
- FEAT-04: Config-driven program round target assertion

**Preference reporting (self-contained):**
- FEAT-10: `PreferenceReportService` in core (per-cabin metrics + whole-set distribution)
- FEAT-11: Surface preference report in desktop and web outputs

**Repetition feature (gated on DEC-03):**
- FEAT-08: `PriorActivitySource` interface + `RepetitionFeature` skeleton (graceful degradation if no source)
- FEAT-09: `ActivityRosterPriorSource` + `EnhancedRosterPriorSource`
- DESKTOP-04: Desktop import flow for repetition

**Desktop UX redesign:**
- DESKTOP-08: Primary overview panel (new MainWindow view, depends on FEAT-03 assertions for display)
- DESKTOP-09: Demote table view to secondary mode
- DESKTOP-05: Modernize look-and-feel (plan after Sprint N+1 establishes web visual language)
- DESKTOP-06: Fix help dialog resizing
- DESKTOP-07: File drag-and-drop on import dialog
- DESKTOP-10: Sort indicators + settings discoverability on table view

**Cleanup:**
- CLEAN-10: Null safety review pass (now that canonical keys + stream I/O are stable)

### Open Decisions (carried forward)

| ID | Decision | Blocks | Recommendation |
|----|----------|--------|----------------|
| DEC-01 | Desktop config persistence format | Sprint N+3 | JSON file in user home (Option A) — supports nested structures like `ProgramDefinition` aliases |
| DEC-02 | Web config strategy | Sprint N+3 | Bundled classpath resource (Option C) for single-org MVP; Option D (per-request upload) only if multi-org demanded |
| DEC-03 | RepetitionFeature import flow | Sprint N+3 | Post-import comparison action (Option B) — keeps primary import flow uncluttered |
| DEC-04 | `dependsOn()` in `RosterFeature` interface | None | Decide inline when a second-order feature dependency actually arises. Low stakes either way. |
| DEC-05 | Web frontend technology | Sprint N+1 | Thymeleaf for stateless MVP. SPA (Vue/React) only if inline filtering or live table preview becomes a priority. |

### Flag Carryovers from PHASE-PLAN.md

- **F4 (still live):** No explicit integration test task for the Spring Boot `ProcessController` existed in the original registry. The rescued Phase 4 branch contains a `ProcessControllerTest` — Sprint 1 Phase 5 must confirm it covers real integration paths (MockMvc or `@SpringBootTest`), not just stubs.
- **F1, F2, F3, F5, F6, F7:** resolved by current state or rolled into new phase planning. Notes preserved in `-PLANNING/PHASE-PLAN.md` if needed.

### Deferred (no scheduled sprint)

- **FEAT-07** — Make `MAX_ROUNDS` runtime-configurable via `CampConfig`. Highest-risk config change. Do not schedule until Sprint N+3 stabilizes all other `CampConfig` properties.
- **WEB-07** — Collaboration/file-sharing features. Post-MVP placeholder. Requires auth infrastructure (out of scope).

---

## Archival Note

Once this file is committed and Sprint 1 has started, `-PLANNING/PHASE-PLAN.md` can be moved to `-PLANNING/0505-completed/` (or wherever feels right) with a header pointing to `ROADMAP.md` as the active document. The detailed per-task notes in PHASE-PLAN.md remain useful as historical context but should not be treated as a live plan.

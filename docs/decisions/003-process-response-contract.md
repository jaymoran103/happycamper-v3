# ADR-003: `/process` response contract

**Status:** Accepted (2026-05-19, Sprint 1 Phase 5)

## Context

Phase 5 ships `POST /process`, the first real HTTP surface for HappyCamper. The endpoint runs the core enhancement pipeline on two uploaded CSVs (camper roster + activity roster), evaluates the assertion registry (per ADR-002), and returns the enriched CSV plus the assertion report.

The web layer is intentionally thin — desktop is the resilient fallback, and core/desktop work resumes immediately after Phase 5 closes. The maintainability lens for this ADR: every line in `happycamper-web` is a future liability. Lock the public contract now so the next person who touches `ProcessController` does not re-litigate response shape, error model, or default behavior.

## Decision

### Success response (200 OK)

```json
{
  "assertions": { "summary": {...}, "results": [...] },
  "enrichedCsv": "<full enriched roster as CSV text>",
  "warnings": [ { "type": "BAD_DATA_FORMAT", "message": "..." }, ... ]
}
```

Three top-level fields. Stateless. No IDs, resource handles, or pagination — every request is self-contained.

**Schema evolution rule:** additive only. New top-level fields are fair game (clients ignore unknown fields). Renaming or removing any of the three locks the contract — version to `/v2/process` instead.

### Warning shape — `[{type, message}]`

`type` is the `com.echo.logging.RosterWarning.WarningType` enum name. Eleven values today: `OTHER`, `UNMATCHED_ACTIVITY_SKIPPED`, `UNMATCHED_ACTIVITY_ADDED`, `DUPLICATE_ACTIVITY`, `BAD_DATA_FORMAT`, `PROGRAM_PARSING_FAILURE`, `MISSING_FEATURE_HEADER`, `CAMPER_MISSING_FIELD`, `UNKNOWN_SWIM_ACTIVITY_FLAGGED`, `UNKNOWN_SWIM_ACTIVITY_IGNORED`, `UNKNOWN_SWIM_LEVEL`.

`message` is pre-rendered, opaque text. Today: `RosterWarning.getDisplayData()` joined by `" | "`. Clients render the string as-is and **never parse it**. Changing the internal join character or field order is not a breaking change.

**`WarningType` enum names are now part of the API contract.** Renaming any of the eleven values is a breaking change requiring `/v2/process`. Adding a new value is additive — clients receive an unknown `type` and either render generically or ignore. Refactors in `RosterWarning.java` must not rename existing enum values without a versioned endpoint.

### Pipeline-abort response (422 Unprocessable Entity)

```json
{
  "errors": [
    { "type": "MALFORMED", "message": "campers.csv row 5: expected 8 columns, found 7" }
  ]
}
```

Symmetric with the warnings shape. `type` is the `com.echo.logging.RosterException.ErrorType` enum name (six values: `FILE`, `MALFORMED`, `MISSING_DATA`, `HEADER`, `WRAPPER`, `INTERNAL`). `message` is the exception message.

`ErrorType` enum names are part of the API contract on the same terms as `WarningType`.

### Request-validation response (400 Bad Request)

```json
{ "error": "camperFile is required" }
```

Asymmetric with the 422 shape on purpose. 400 covers pre-flight validation (missing multipart, unreadable upload) — there is no `ErrorType` enum value that fits, and inventing a parallel web-only `VALIDATION` enum adds state for two error cases. The asymmetry is deliberate and documented here so it is not "fixed" later under the impression it is an oversight.

### Status code map

| Code | When | Body shape |
|---|---|---|
| `200 OK` | Pipeline ran end-to-end. May include FAILED assertions (informational per ADR-002). | `ProcessResponse` |
| `400 Bad Request` | `camperFile` or `activityFile` is missing or empty; `IOException` reading the upload. | `{ "error": string }` |
| `413 Payload Too Large` | A multipart file or the total request exceeds `spring.servlet.multipart.max-file-size` / `max-request-size` (today 10MB / 20MB). Translated from Spring's `MaxUploadSizeExceededException` by `ProcessController.handleUploadTooLarge`; otherwise Spring's default returns 500 with no body. | `{ "error": string }` |
| `422 Unprocessable Entity` | Pipeline aborted: prereq feature `preValidate` failed (e.g., ActivityFeature) or a feature's `postValidate` failed. | `{ "errors": [{type, message}] }` |
| `500 Internal Server Error` | Unhandled exception. Spring's default handler is fine; intentionally not a contract surface. | Spring default |

A FAILED assertion does **not** flip the response code from 200 to 422 — assertions are informational. Upgrading to "block on FAILED" is a one-line change at the controller's `if (enhancedRoster == null)` check, gated on a future UI signal.

### Feature-ID resolution

The `features` request param is optional. When omitted, the controller defaults to **every registered feature ID** from `RosterService.getFeatureRegistry().all()`. A new feature added to core auto-runs on `/process` with zero web edits — this is the single biggest maintainability lever in the web layer.

When `features` is supplied (e.g., `?features=activity,swimlevel`), only those features run. `ActivityFeature` is `alwaysEnabled=true` regardless of the request and runs in every invocation (its absence aborts the pipeline → 422).

Unknown feature IDs are silently ignored (per `RosterService.applyFeaturesAndFinalize`'s `registration.find(id).orElse(null)` continue), not treated as 400 errors. Adding a "stricter validation" mode is a future option but would break the current "old client with new feature set" behavior.

### DTOs are separate from core types

`com.echo.assertion.AssertionReport` is the domain type. `com.echo.web.dto.AssertionReportDto` is the JSON contract. They can drift independently — a core refactor that splits or merges fields does not force a web breaking change. Same for `AssertionResultDto`, `WarningDto`, `ErrorDto`. The DTOs duplicate fields field-for-field today; tomorrow they may diverge.

### Serialization conventions

- Empty collections always serialize as `[]`. Never omitted, never `null`.
- Field order in JSON is unspecified — clients must not depend on it.
- Warning and error array **ordering is unspecified.** `WarningManager`'s log uses unordered maps (`HashMap<WarningType, ArrayList<...>>`). Clients sort if they care. Adding a guarantee later is safe; revoking one is not.
- Numbers serialize as JSON numbers (Jackson defaults). Booleans as `true`/`false`. Strings UTF-8.

### Cardinality

Warning and error arrays are unbounded today. A pathological roster could produce hundreds of entries. No `MAX_WARNINGS` cap — defer until a real roster hits the wall. (`AssertionResult.MAX_FAILURE_DETAILS = 50` already caps per-result failure details; the report-level cap is a separate decision tracked as a follow-up.)

## Consequences

- The first real HTTP consumer (web UI, integration test, third-party script) sees a stable contract from day one. Internal core refactors do not propagate to the API unless they cross the DTO/enum boundary.
- `WarningType` and `ErrorType` enum names are now load-bearing for external consumers. Renames require a versioned endpoint or a coordinated client update.
- The "default to all features when omitted" rule means new core features ship in the web API immediately. If a feature is dangerous or backward-incompatible, the registry default behavior may need to gain an opt-in/opt-out flag — orthogonal change, not breaking.
- The 400/422 asymmetry will get questioned. The asymmetry exists because the error categories are different (request-shape vs pipeline-output), not from an oversight.
- Versioning to `/v2/process` is the escape hatch for any contract violation. Cost: one new controller method + one new DTO package. Carry-on cost: maintaining two endpoints for the migration window.

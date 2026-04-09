# ADDED Requirements

## Requirement: Per-service specs SHALL be joined into one unified contract

The three per-service OpenAPI documents SHALL be joined into a single
`openapi/unified-openapi.yaml` file using `redocly join` with
`--prefix-components-with-info-prop title`.

### Scenario: Join produces a valid unified spec

- **WHEN** the `openapi:join` script runs against the three per-service specs
- **THEN** a single file `openapi/unified-openapi.yaml` MUST be produced that is
  a valid OpenAPI 3.x document containing all paths, components, and tags from
  all three services

### Scenario: Shared component schemas are prefixed by service

- **WHEN** multiple services define a component with the same name (e.g.,
  `JsendResponse`, `FailData`, `Violation`)
- **THEN** each MUST be prefixed with the service's PascalCase title (e.g.,
  `UserManagement_JsendResponse`, `MeetingManagement_JsendResponse`)

### Scenario: Tags are NOT prefixed

- **WHEN** the join runs
- **THEN** tags MUST retain their original names without prefix (they are
  already unique across services)

## Requirement: A Redocly configuration SHALL be present at the repository root

A `redocly.yaml` file SHALL be created at the repository root extending the
`recommended` ruleset with `operation-operationId` enforced as an error.

### Scenario: Lint catches missing operationId

- **WHEN** `redocly lint` runs against the unified spec
- **THEN** any operation missing an `operationId` MUST be reported as an error

## Requirement: The unified spec SHALL be linted after join

The `openapi:lint` script SHALL run `redocly lint` on the unified spec and MUST
pass without errors.

### Scenario: Lint passes on clean spec

- **WHEN** `pnpm run openapi:lint` runs
- **THEN** the exit code MUST be 0

## Requirement: The unified spec SHALL be committed to version control

`openapi/unified-openapi.yaml` SHALL be committed to git. Generated SDK code
SHALL be gitignored.

### Scenario: Unified spec is tracked

- **WHEN** `git status` is checked after running the pipeline
- **THEN** `openapi/unified-openapi.yaml` MUST be a tracked file (not
  gitignored)

## Requirement: package.json scripts SHALL orchestrate the full pipeline

The root `package.json` SHALL contain updated scripts:

- `openapi:services` — runs `generateOpenApiDocsFromTests` for all three
  services
- `openapi:join` — runs `redocly join` with the correct flags
- `openapi:lint` — runs `redocly lint` on the unified spec
- `openapi:unified` — runs services, join, and lint in sequence

### Scenario: Full pipeline succeeds end-to-end

- **WHEN** `pnpm run openapi:unified` is executed
- **THEN** it MUST generate per-service specs, join them, and lint the result
  without errors

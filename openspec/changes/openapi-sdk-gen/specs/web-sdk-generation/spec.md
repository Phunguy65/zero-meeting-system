# ADDED Requirements

## Requirement: The web app SHALL generate TypeScript client code from the unified spec

`@hey-api/openapi-ts` with `@hey-api/client-fetch` SHALL be configured to
generate TypeScript API client code and types from
`openapi/unified-openapi.yaml` into `frontends/web/src/generated/`.

### Scenario: SDK generation produces TypeScript files

- **WHEN** `pnpm run sdk:generate` runs in the web app
- **THEN** TypeScript source files MUST be generated in
  `frontends/web/src/generated/` including client functions and type definitions

### Scenario: Generated code passes lint and build

- **WHEN** the web app runs lint and build after SDK generation
- **THEN** `pnpm --filter web lint` and `pnpm --filter web build` MUST succeed

## Requirement: Generated web code SHALL be gitignored

`frontends/web/src/generated/` SHALL be excluded from version control.

### Scenario: Generated files are not tracked

- **WHEN** `git status` is checked after running generation
- **THEN** no files under `frontends/web/src/generated/` MUST appear as
  untracked or modified

## Requirement: @hey-api dependencies SHALL be added to the web app

`@hey-api/openapi-ts` and `@hey-api/client-fetch` SHALL be added as
devDependencies in `frontends/web/package.json`.

### Scenario: Dependencies are declared

- **WHEN** `frontends/web/package.json` is inspected
- **THEN** `@hey-api/openapi-ts` and `@hey-api/client-fetch` MUST be listed
  under `devDependencies`

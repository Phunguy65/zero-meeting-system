# Context

The project has three public Spring Boot REST services behind Kong API Gateway:

- **user-management** — Postgres, Kafka, Redis, gRPC server, Firebase
- **meeting-management** — Postgres, Kafka, Redis, gRPC client, LiveKit
- **chat-management** — MongoDB, Kafka, LiveKit (no API versioning, hardcoded
  `/api/chat/rooms`)

A **notification** service exists but is Kafka-consumer-only (no REST) and is
out of scope.

All services use springdoc-openapi (v3.0.2, injected by the `service.base`
convention plugin) and already have `@Operation`, `@Tag`, and `@Hidden`
annotations on controllers. Each service has a `generateOpenApiDocsFromTests`
Gradle task that filters for `*OpenApiGenerationTest`, but **no test class
matching that pattern exists yet**.

A legacy springdoc Gradle plugin flow (`openApi {}` block +
`forkedSpringBootRun`) also exists in each service's `build.gradle.kts` and is
broken for chat-management (missing AOT workaround). This legacy flow will be
removed.

The `service.base` convention plugin injects JPA, Postgres, Redis, gRPC, and
Kafka starters into **all** services — including chat-management which uses
MongoDB. chat-management has `testRuntimeOnly("com.h2database:h2")` to satisfy
the inherited JPA auto-config.

Android app (`frontends/android-app`) uses the same `libs.versions.toml` from
`../../gradle/libs.versions.toml` which already catalogues `openapiGenerator`
(v7.20.0), `retrofit` (2.12.0), `okhttp` (4.12.0), and `converter-gson`
(2.12.0).

Web app (`frontends/web`) uses Next.js 16, React 19, TypeScript, Biome, and
Tailwind 4.

## Goals / Non-Goals

**Goals:**

- Produce per-service OpenAPI specs from test classes without requiring real
  Kafka, Redis, gRPC, LiveKit, or Firebase
- Join three specs into one unified contract committed to version control
- Generate typed Android (Java/Retrofit 2) and web (TypeScript/fetch) SDKs from
  the unified contract
- Unwrap JSend envelopes at the client interceptor/middleware layer so
  application code receives payloads directly and errors as typed exceptions
- Expose an `ErrorTranslator` hook for future i18n integration

**Non-Goals:**

- Implementing i18n message translation (extension point only)
- Normalizing chat-management's path to `/api/v1/chat/...`
- Generating SDKs for notification service
- Auto-syncing Kong routes from the unified spec

## Decisions

### 1. OpenAPI generation via `@SpringBootTest(RANDOM_PORT)` + Testcontainers DB + `@MockitoBean` external deps

The test boots the full Spring context against a real database (Postgres TC or
MongoDB TC) while mocking all external infrastructure beans. The test calls
`GET /v3/api-docs.yaml` and writes the result to `build/openapi/openapi.yaml`.

Why:

- Springdoc requires a running Spring ApplicationContext — no static
  annotation-scanning alternative exists for Spring MVC.
- `RANDOM_PORT` gives a real servlet environment so springdoc resolves all
  paths, versioning, and security metadata accurately.
- Testcontainers for DB avoids mocking JPA/Flyway/Mongo, which would risk
  incomplete context loading.
- `@MockitoBean` for Kafka, Redis, gRPC, LiveKit, Firebase avoids the need for
  those services at generation time.

Alternative considered:

- `webEnvironment = MOCK` + `MockMvc`: lighter, but unclear whether Spring 7's
  API versioning resolves correctly without a real servlet. Rejected in favour
  of the more accurate approach.

### 2. Remove legacy `openApi {}` + `forkedSpringBootRun` plugin flow

The legacy springdoc Gradle plugin boots the full application to fetch
`/v3/api-docs`. It is broken for chat-management (missing AOT workaround),
conflicts with the test-based approach, and creates a duplicate source of truth.

Why: single generation path is simpler and already covers the use case.

### 3. `SecurityConfig` permits springdoc endpoints in all services

Each service's `SecurityConfig` will add `/v3/api-docs` and `/v3/api-docs.yaml`
to `permitAll`. Without this, the default `anyRequest().authenticated()` (or
`denyAll()` in chat-management) blocks the springdoc endpoint during test
generation.

Why: simpler than injecting mock auth in the test; springdoc endpoints are
informational and safe to expose in dev/test.

### 4. PascalCase `info.title` for Redocly component prefixing

`@OpenAPIDefinition(info.title)` values become `UserManagement`,
`MeetingManagement`, `ChatManagement`. Redocly's
`--prefix-components-with-info-prop title` uses these as schema name prefixes
(e.g., `UserManagement_JsendResponse`).

Why: hyphenated titles (`user-management`) produce invalid identifiers in some
code generators. PascalCase is safe for Java, TypeScript, and OpenAPI schema
names.

### 5. Redocly join with component prefix only (no tag prefix)

Tags across services are already unique: `Auth`, `Users`, `Me`, `Meetings`,
`Recordings`, `Participants`, `JoinRequests`, `UserMeetings`, `Chat`. No prefix
needed.

Shared schemas (`JsendResponse`, `FailData`, `Violation`,
`CursorScrollResponse`, `OffsetScrollResponse`) collide across services and
require prefixing.

### 6. Spike before full pipeline: verify springdoc path versioning output

Controllers use Spring 7 API versioning:
`@GetMapping(value = "/{version}/users/{id}", version = "1.0")`. It is unknown
whether springdoc emits `/api/{version}/users/{id}` (path variable) or
`/api/v1/users/{id}` (concrete). The spike generates one spec and inspects the
output. If springdoc emits a path variable, an `OpenApiCustomizer` bean will
rewrite paths to concrete versions.

### 7. Android SDK: `openapi-generator-gradle-plugin` (Java, Retrofit 2, Gson)

Generated into `build/generated/openapi/` and wired via
`sourceSets.main.java.srcDir`. Gitignored.

Why: integrates into the Gradle lifecycle; regenerates automatically when the
unified spec changes. Plugin already in version catalog.

### 8. Web SDK: `@hey-api/openapi-ts` + `@hey-api/client-fetch`

Generated into `frontends/web/src/generated/`. Gitignored.

Why: native fetch matches Next.js 16 patterns; zero extra dependencies; first-
class TypeScript types.

### 9. JSend unwrap: OkHttp Interceptor (Android) + @hey-api middleware (web)

Both parse the JSend envelope before application code sees the response:

- `"success"` → unwrap `data` field, rebuild response body
- `"fail"` → throw typed exception with `code`, `message`, `violations`
- `"error"` → throw server error exception with `message`

An `ErrorTranslator` interface/type is provided with a default that returns the
original message unchanged. Translation implementation is out of scope.

### 10. `unified-openapi.yaml` committed; generated SDK code gitignored

The unified spec is the contract and is version-controlled. Generated client
code is a build artifact and is gitignored.

### 11. Server URL configured at source via springdoc

Each service sets `springdoc.servers[0].url` in its properties. The first file
in the Redocly join determines the unified spec's `servers` block.

## Risks / Trade-offs

- **[Path versioning unknown]** → Spike task verifies springdoc output before
  full pipeline. If `/{version}/` is emitted, an `OpenApiCustomizer` will
  rewrite to concrete `/v1/` paths.
- **[operationId collision after join]** → `redocly lint` with
  `operation-operationId: error` catches collisions. If found, explicit
  `operationId` attributes will be added to `@Operation` annotations.
- **[`service.base` injects JPA/Postgres into chat-management]** → H2 already
  present as `testRuntimeOnly` satisfies inherited JPA auto-config. MongoDB
  Testcontainer handles Mongo auto-config. Acceptable trade-off.
- **[Prefixed shared schemas produce duplicated types in SDK]** →
  `UserManagement_JsendResponse` and `MeetingManagement_JsendResponse` are
  semantically identical but prefixed differently. Client interceptors handle
  the envelope before generated code sees it, so duplication is acceptable.

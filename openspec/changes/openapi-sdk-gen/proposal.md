# Why

The three public REST services (user-management, meeting-management,
chat-management) expose JSend-based HTTP APIs but have no automated pipeline to
produce typed client SDKs. Android and web frontends currently hand-write API
calls, leading to contract drift, duplicated error handling, and no compile-time
safety when the server API changes. A unified OpenAPI contract and generated
client code close this gap.

## What Changes

- Each public service gains an `OpenApiGenerationTest` that boots the app via
  `@SpringBootTest(RANDOM_PORT)` with Testcontainers for its database and
  `@MockitoBean` for all external infrastructure (Kafka, Redis, gRPC, LiveKit,
  Firebase), fetches `/v3/api-docs.yaml`, and writes the result to
  `build/openapi/openapi.yaml`.
- The legacy springdoc Gradle plugin flow (`openApi {}` block,
  `forkedSpringBootRun`, `generateOpenApiDocs`) is removed from all three
  services; `generateOpenApiDocsFromTests` becomes the single generation path.
- Each service's `SecurityConfig` adds `/v3/api-docs` and `/v3/api-docs.yaml` to
  `permitAll` so the springdoc endpoint is accessible during test generation.
- `@OpenAPIDefinition(info.title)` on each Application class is changed to
  PascalCase (`UserManagement`, `MeetingManagement`, `ChatManagement`) so that
  Redocly component prefixes produce valid identifiers for code generators.
- Each service configures `springdoc.servers[0].url` to point to the Kong
  gateway URL.
- A `redocly.yaml` configuration is added at the repository root.
- The three per-service specs are joined into one `openapi/unified-openapi.yaml`
  via `redocly join --prefix-components-with-info-prop title` (no tag prefix
  since tags are already unique across services). The unified file is committed
  to version control.
- `openapi-generator-gradle-plugin` (already catalogued as v7.20.0) is wired
  into the Android app Gradle build to generate Java Retrofit 2 client code from
  the unified spec into `build/generated/openapi/`.
- `@hey-api/openapi-ts` with `@hey-api/client-fetch` is added to the web app to
  generate TypeScript fetch client code into `frontends/web/src/generated/`.
- A hand-written OkHttp `Interceptor` on Android and a `@hey-api` response
  transformer on web unwrap JSend envelopes: success responses yield the payload
  directly; failure/error responses throw typed exceptions that preserve the
  machine-readable error code and message.
- Both client integrations expose an `ErrorTranslator` extension point for
  future i18n message translation (translation itself is out of scope).
- A spike task verifies whether springdoc emits concrete `/v1/` paths or literal
  `/{version}/` path parameters under Spring Framework 7 API versioning; the
  result determines whether an `OpenApiCustomizer` is needed.

## Capabilities

### New Capabilities

- `service-openapi-generation`: Per-service OpenAPI spec generation from test
  classes, including SecurityConfig changes, server URL configuration, legacy
  plugin removal, and the versioning spike.
- `unified-openapi-contract`: Redocly join, lint, and configuration producing
  one canonical `openapi/unified-openapi.yaml`.
- `android-sdk-generation`: openapi-generator Gradle plugin integration for the
  Android app producing Java Retrofit 2 client code.
- `web-sdk-generation`: @hey-api/openapi-ts integration for the web app
  producing TypeScript fetch client code.
- `client-jsend-unwrap`: OkHttp interceptor (Android) and @hey-api middleware
  (web) that unwrap JSend envelopes and expose error-translation hooks.

### Modified Capabilities

_(none -- no existing spec-level requirements change)_

## Impact

- **Backend services** (user-management, meeting-management, chat-management):
  SecurityConfig, Application class annotation, build.gradle.kts, and test
  source sets are modified. chat-management additionally gains
  `TestcontainersConfiguration` (MongoDB) and `application-test` config.
- **Android app** (`frontends/android-app/app/build.gradle.kts`): new plugin,
  generated source set, interceptor and exception classes.
- **Web app** (`frontends/web/package.json`, `src/`): new dev dependencies,
  generated directory, middleware module.
- **Root** (`package.json`, `redocly.yaml`, `.gitignore`,
  `openapi/unified-openapi.yaml`): new tooling config and committed contract
  artifact.
- **Dependencies added**: `@hey-api/openapi-ts`, `@hey-api/client-fetch` (web
  devDependencies). No new backend dependencies (springdoc + openapi-generator
  already catalogued).

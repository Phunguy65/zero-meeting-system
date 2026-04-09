# Tasks

## 1. Spike: Verify springdoc path versioning output

- [x] 1.1 Create `OpenApiGenerationTest` in user-management with
      `@SpringBootTest(RANDOM_PORT)`,
      `@Import(TestcontainersConfiguration.class)`, `@ActiveProfiles("test")`,
      `@MockitoBean` for `KafkaEventPublisher`, `OutboxEventPublisher`,
      `KafkaTemplate`, `KafkaConfig`, `FirebaseTokenVerifier`, and Redis/gRPC
      beans. Test calls `GET /v3/api-docs.yaml` and writes to
      `build/openapi/openapi.yaml`
- [x] 1.2 Temporarily add `/v3/api-docs` and `/v3/api-docs.yaml` to `permitAll`
      in user-management `SecurityConfig`
- [x] 1.3 Run
      `./services/gradlew -p services/user-management generateOpenApiDocsFromTests`
      and inspect the output YAML for path format
      (`/api/{version}/auth/register` vs `/api/v1/auth/register`)
- [x] 1.4 Document the spike result as a comment in this file. If paths contain
      `/{version}/`, create an `OpenApiCustomizer` bean in user-management that
      rewrites paths to concrete `/v1/` segments, then re-run and verify

<!-- Spike result (2026-04-06):
  - springdoc outputs CONCRETE version paths: /api/1.0/auth/register (not /{version}/).
  - However, the format uses bare "1.0" instead of "v1" — clients and Kong use /api/v1/...
  - Created VersionPathCustomizer (OpenApiCustomizer) that rewrites /api/1.0/ → /api/v1/.
  - After customizer: all paths correctly show /api/v1/...
  - Additional findings:
    * WebConfig.addPathPrefix("/api", @RestController) was also prefixing springdoc's
      controller, causing /api/v3/api-docs which clashed with API versioning (version 3.0.0
      rejected). Fixed by narrowing predicate to io.github.phunguy65.zms.* package only.
    * Mocking KafkaConfig @Configuration directly doesn't work — must mock ProducerFactory
      and KafkaTemplate individually instead.
    * Redis/gRPC beans load fine without mocking (conditional or lazy connect).
-->

## 2. Backend: Remove legacy springdoc plugin flow

- [x] 2.1 Remove `openApi { }` block, `forkedSpringBootRun` task references and
      workarounds, and `springdocOpenapi` plugin alias from
      `services/user-management/build.gradle.kts`
- [x] 2.2 Remove `openApi { }` block, `forkedSpringBootRun` task references and
      workarounds, and `springdocOpenapi` plugin alias from
      `services/meeting-management/build.gradle.kts`
- [x] 2.3 Remove `openApi { }` block and `springdocOpenapi` plugin alias from
      `services/chat-management/build.gradle.kts`

## 3. Backend: Update Application class info.title to PascalCase

- [x] 3.1 Change `@OpenAPIDefinition(info = @Info(title = "user-management"))`
      to `title = "UserManagement"` in `UserManagementApplication.java`
- [x] 3.2 Change
      `@OpenAPIDefinition(info = @Info(title = "meeting-management"))` to
      `title = "MeetingManagement"` in `MeetingManagementApplication.java`
- [x] 3.3 Change `@OpenAPIDefinition(info = @Info(title = "chat-management"))`
      to `title = "ChatManagement"` in `ChatManagementApplication.java`

## 4. Backend: Configure springdoc server URL

- [x] 4.1 Add `springdoc.servers[0].url=${GATEWAY_URL:http://localhost:8080}`
      and `springdoc.servers[0].description=API Gateway` to
      `services/user-management/src/main/resources/application.properties`
- [x] 4.2 Add the same springdoc server properties to
      `services/meeting-management/src/main/resources/application.properties`
- [x] 4.3 Add the same springdoc server properties (YAML format) to
      `services/chat-management/src/main/resources/application.yaml`

## 5. Backend: SecurityConfig — permit springdoc endpoints

- [x] 5.1 Add
      `.requestMatchers("/v3/api-docs", "/v3/api-docs.yaml").permitAll()` to
      user-management `SecurityConfig.securityFilterChain()` (if not already
      done in spike)
- [x] 5.2 Add
      `.requestMatchers("/v3/api-docs", "/v3/api-docs.yaml").permitAll()` to
      meeting-management `SecurityConfig.securityFilterChain()`
- [x] 5.3 Add
      `.requestMatchers("/v3/api-docs", "/v3/api-docs.yaml").permitAll()` to
      chat-management `ChatSecurityConfig.filterChain()`

## 6. Backend: chat-management test infrastructure

- [x] 6.1 Create
      `services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/config/TestcontainersConfiguration.java`
      with `@TestConfiguration`, `@Bean @ServiceConnection` for
      `MongoDBContainer`
- [x] 6.2 Create
      `services/chat-management/src/test/resources/application-test.properties`
      with chat JWT secret and any required test-profile overrides

## 7. Backend: OpenApiGenerationTest for remaining services

- [x] 7.1 Create `OpenApiGenerationTest` in meeting-management with
      `@SpringBootTest(RANDOM_PORT)`,
      `@Import(TestcontainersConfiguration.class)`, `@ActiveProfiles("test")`,
      `@MockitoBean` for `KafkaEventPublisher`, `OutboxEventPublisher`,
      `KafkaTemplate`, `KafkaConfig`, `MeetingSseManager`,
      `UserProfileUpdatedConsumer`, `RecordingCleanupJob`,
      `JoinRequestCleanupJob`, LiveKit beans, gRPC client stub, and Redis beans.
      Test calls `GET /v3/api-docs.yaml` and writes to
      `build/openapi/openapi.yaml`. Include `OpenApiCustomizer` if spike
      determined it is needed
- [x] 7.2 Create `OpenApiGenerationTest` in chat-management with
      `@SpringBootTest(RANDOM_PORT)`,
      `@Import(TestcontainersConfiguration.class)`, `@ActiveProfiles("test")`,
      `@MockitoBean` for `KafkaTemplate`, `KafkaConfig`, `MeetingEventConsumer`,
      `ParticipantEventConsumer`, LiveKit beans. Test calls
      `GET /v3/api-docs.yaml` and writes to `build/openapi/openapi.yaml`.
      Include `OpenApiCustomizer` if spike determined it is needed
- [x] 7.3 Run `generateOpenApiDocsFromTests` for all three services and verify
      each produces a valid `build/openapi/openapi.yaml`

## 8. Redocly: Configuration and join pipeline

- [x] 8.1 Create `redocly.yaml` at repository root extending `recommended` with
      `operation-operationId: error` and `no-ambiguous-paths: error`
- [x] 8.2 Update `package.json` script `openapi:join` to run `redocly join` with
      `--prefix-components-with-info-prop title -o openapi/unified-openapi.yaml`
      against the three per-service spec files
- [x] 8.3 Update `package.json` script `openapi:lint` to run
      `redocly lint openapi/unified-openapi.yaml`
- [x] 8.4 Run `pnpm run openapi:unified` end-to-end and verify
      `openapi/unified-openapi.yaml` is produced and lint passes. Fix any
      operationId collisions or lint errors

## 9. Android: openapi-generator Gradle plugin integration

- [x] 9.1 Add `alias(libs.plugins.openapiGenerator)` to
      `frontends/android-app/app/build.gradle.kts` plugins block
- [x] 9.2 Configure `openApiGenerate` task: `generatorName = "java"`,
      `library = "retrofit2"`,
      `inputSpec = "../../openapi/unified-openapi.yaml"`,
      `outputDir = build/generated/openapi`,
      `apiPackage = "io.github.phunguy65.zms.sdk.api"`,
      `modelPackage = "io.github.phunguy65.zms.sdk.model"`, `configOptions` with
      `dateLibrary = "java8"`, `serializationLibrary = "gson"`
- [x] 9.3 Add
      `sourceSets { main { java { srcDir("${layout.buildDirectory.get()}/generated/openapi/src/main/java") } } }`
      to wire generated code into compilation
- [x] 9.4 Run `./gradlew :app:openApiGenerate` and then
      `./gradlew :app:compileDebugJavaWithJavac` to verify generated code
      compiles

## 10. Android: JSend unwrap interceptor

- [x] 10.1 Create `JsendEnvelope.java` model class (fields: `status`, `data`,
      `message`) in the android-app SDK package
- [x] 10.2 Create `ApiFailException.java` extending `RuntimeException` with
      `code`, `message`, `violations` fields
- [x] 10.3 Create `ApiErrorException.java` extending `RuntimeException` with
      `message` field
- [x] 10.4 Create `ErrorTranslator.java` interface with
      `String translate(String code, String defaultMessage)` and a `DEFAULT`
      instance that returns `defaultMessage`
- [x] 10.5 Create `JsendUnwrapInterceptor.java` implementing
      `okhttp3.Interceptor` that parses the response body as `JsendEnvelope`,
      unwraps `data` on success, throws `ApiFailException` on fail (with
      translator), and throws `ApiErrorException` on error

## 11. Web: @hey-api SDK generation

- [x] 11.1 Add `@hey-api/openapi-ts` and `@hey-api/client-fetch` as
      devDependencies to `frontends/web/package.json`
- [x] 11.2 Add script
      `"sdk:generate": "openapi-ts --input ../../openapi/unified-openapi.yaml --output src/generated --client @hey-api/client-fetch"`
      to `frontends/web/package.json`
- [x] 11.3 Add `src/generated/` to `frontends/web/.gitignore`
- [x] 11.4 Run `pnpm --filter web sdk:generate` and verify TypeScript files are
      produced in `frontends/web/src/generated/`
- [x] 11.5 Run `pnpm --filter web lint` and `pnpm --filter web build` to verify
      generated code integrates cleanly

## 12. Web: JSend unwrap middleware

- [x] 12.1 Create `frontends/web/src/lib/api/types.ts` with `ApiFailError` class
      (extends `Error`, fields: `code`, `message`, `errors`) and `ApiError`
      class (extends `Error`, field: `message`), and `ErrorTranslator` type
- [x] 12.2 Create `frontends/web/src/lib/api/jsend-middleware.ts` that exports a
      response transformer: unwrap `data` on success, throw `ApiFailError` on
      fail (with optional translator), throw `ApiError` on error
- [x] 12.3 Create `frontends/web/src/lib/api/client.ts` that creates the
      @hey-api client with the JSend middleware configured

## 13. Root: gitignore and pipeline verification

- [x] 13.1 Add `frontends/web/src/generated/` to root `.gitignore` (or
      `frontends/web/.gitignore`)
- [x] 13.2 Verify `build/` is already gitignored (covers Android generated +
      per-service openapi output)
- [x] 13.3 Verify `openapi/unified-openapi.yaml` is NOT gitignored (should be
      committed)
- [x] 13.4 Run full pipeline end-to-end: `pnpm run openapi:unified` → Android
      generate + compile → Web generate + lint + build. Fix any issues

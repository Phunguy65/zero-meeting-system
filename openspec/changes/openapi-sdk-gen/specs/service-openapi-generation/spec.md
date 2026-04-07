# ADDED Requirements

## Requirement: Each public service SHALL generate an OpenAPI document from a test class

Each of the three public REST services (user-management, meeting-management,
chat-management) SHALL produce an OpenAPI YAML document by running an
`OpenApiGenerationTest` via the existing `generateOpenApiDocsFromTests` Gradle
task. The test boots `@SpringBootTest(webEnvironment = RANDOM_PORT)` with
Testcontainers for the service's database and `@MockitoBean` for all external
infrastructure beans, then fetches the springdoc endpoint and writes the result
to `build/openapi/openapi.yaml`.

### Scenario: user-management generates its OpenAPI spec

- **WHEN** the `generateOpenApiDocsFromTests` Gradle task runs for
  user-management
- **THEN** a file `services/user-management/build/openapi/openapi.yaml` MUST be
  produced containing all public endpoints from AuthController, UserController,
  and MeController

### Scenario: meeting-management generates its OpenAPI spec

- **WHEN** the `generateOpenApiDocsFromTests` Gradle task runs for
  meeting-management
- **THEN** a file `services/meeting-management/build/openapi/openapi.yaml` MUST
  be produced containing all public endpoints from MeetingController,
  RecordingController, JoinRequestController, ParticipantController, and
  UserMeetingController, excluding `@Hidden` endpoints

### Scenario: chat-management generates its OpenAPI spec

- **WHEN** the `generateOpenApiDocsFromTests` Gradle task runs for
  chat-management
- **THEN** a file `services/chat-management/build/openapi/openapi.yaml` MUST be
  produced containing all public endpoints from ChatController

## Requirement: OpenAPI generation SHALL NOT require external infrastructure

The `OpenApiGenerationTest` in each service SHALL mock all non-database external
dependencies so that only Docker (for Testcontainers database) is required.

### Scenario: Generation succeeds without Kafka

- **WHEN** no Kafka broker is running
- **THEN** `OpenApiGenerationTest` MUST still produce the OpenAPI spec by
  mocking all Kafka-related beans (`KafkaTemplate`, `KafkaConfig`,
  `KafkaEventPublisher`, `OutboxEventPublisher`, Kafka consumer beans)

### Scenario: Generation succeeds without Redis

- **WHEN** no Redis/Valkey server is running
- **THEN** `OpenApiGenerationTest` for user-management and meeting-management
  MUST still produce the OpenAPI spec by mocking Redis-related beans

### Scenario: Generation succeeds without gRPC

- **WHEN** no gRPC server or peer service is running
- **THEN** `OpenApiGenerationTest` MUST mock gRPC server beans (user-management)
  and gRPC client stubs (meeting-management)

### Scenario: Generation succeeds without LiveKit

- **WHEN** no LiveKit server is running
- **THEN** `OpenApiGenerationTest` for meeting-management and chat-management
  MUST mock LiveKit adapter beans

### Scenario: Generation succeeds without Firebase

- **WHEN** no Firebase credentials are configured
- **THEN** `OpenApiGenerationTest` for user-management MUST mock
  `FirebaseTokenVerifier`

## Requirement: Springdoc endpoints SHALL be accessible during generation

Each service's `SecurityConfig` SHALL permit unauthenticated access to
`/v3/api-docs` and `/v3/api-docs.yaml` so the generation test can fetch the spec
without authentication.

### Scenario: Springdoc endpoint is not blocked by security

- **WHEN** a GET request is made to `/v3/api-docs.yaml` without authentication
- **THEN** the response MUST be HTTP 200 with the OpenAPI YAML content

## Requirement: Service info.title SHALL use PascalCase names

The `@OpenAPIDefinition(info = @Info(title = ...))` annotation on each
Application class SHALL use PascalCase names: `UserManagement`,
`MeetingManagement`, `ChatManagement`.

### Scenario: PascalCase title in generated spec

- **WHEN** the OpenAPI spec for user-management is generated
- **THEN** the `info.title` field MUST be `UserManagement`

## Requirement: Each service SHALL configure a gateway server URL

Each service SHALL set `springdoc.servers[0].url` to the Kong gateway URL so
that the generated spec's `servers` block points to the API gateway rather than
`localhost`.

### Scenario: Server URL in generated spec

- **WHEN** the OpenAPI spec is generated
- **THEN** the `servers[0].url` field MUST contain the configured gateway URL

## Requirement: Legacy springdoc plugin flow SHALL be removed

The `openApi {}` configuration block, the `forkedSpringBootRun` task and its
workarounds, and the `generateOpenApiDocs` task SHALL be removed from all three
service `build.gradle.kts` files.

### Scenario: Only one generation path exists

- **WHEN** the `build.gradle.kts` of any public service is inspected
- **THEN** the `openApi { }` block, `forkedSpringBootRun` references, and
  `generateOpenApiDocs` task MUST NOT be present

## Requirement: A spike SHALL verify springdoc path versioning output

Before the full pipeline is wired, a spike task SHALL generate a single service
spec and verify whether springdoc emits concrete `/v1/` path segments or literal
`/{version}/` path parameters.

### Scenario: Spike produces concrete version paths

- **WHEN** the spike generates the user-management OpenAPI spec
- **THEN** the output paths MUST be inspected; if they contain `/{version}/`
  instead of `/v1/`, an `OpenApiCustomizer` bean MUST be implemented to rewrite
  paths to concrete versions

### Scenario: Spike result is documented

- **WHEN** the spike completes
- **THEN** the result (concrete or variable) and any follow-up action taken MUST
  be documented as a comment in the tasks file

## Requirement: chat-management SHALL have test infrastructure for generation

chat-management SHALL have a `TestcontainersConfiguration` class providing a
MongoDB container and an `application-test` config file enabling the test
profile.

### Scenario: MongoDB Testcontainer configuration exists

- **WHEN** `OpenApiGenerationTest` for chat-management boots the Spring context
- **THEN** a `TestcontainersConfiguration` with `@ServiceConnection` for MongoDB
  MUST be imported

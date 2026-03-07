# ADDED Requirements

## Requirement: Shared EventPublisher port

The `shared` module SHALL provide a `PublishableEvent` interface (extending
`DomainEvent`) and an `EventPublisher` port interface in
`io.github.phunguy65.zms.shared.domain`. A `NoOpEventPublisher` default
implementation SHALL exist in
`io.github.phunguy65.zms.shared.infrastructure.messaging` and SHALL be
auto-configured when no other `EventPublisher` bean is present.

### Scenario: PublishableEvent carries required metadata

- **WHEN** a domain event implements `PublishableEvent`
- **THEN** it SHALL expose `eventId()` (UUIDv7), `aggregateId()` (UUID),
  `aggregateType()` (String), `eventType()` (String, e.g. `user.registered.v1`),
  `topic()` (String, e.g. `user-management.user.registered`), and `occurredAt()`
  (Instant)

### Scenario: NoOpEventPublisher is used when Kafka is not configured

- **WHEN** no `KafkaEventPublisher` bean is registered in the application
  context
- **THEN** `NoOpEventPublisher` SHALL be injected wherever `EventPublisher` is
  required and SHALL log a warning for each event it receives without throwing
  an exception

---

## Requirement: Transactional Outbox persistence

Each service that publishes events SHALL maintain an `outbox_event` table.
Saving a domain entity and saving the corresponding `outbox_event` row SHALL
occur within the same JPA transaction, guaranteeing atomicity.

### Scenario: Domain entity save and outbox row are atomic

- **WHEN** a use case saves a domain entity (e.g., `User`) and registers a
  `PublishableEvent`
- **THEN** both the entity row and the `outbox_event` row SHALL be committed
  together, or both SHALL be rolled back

### Scenario: Outbox row is created after transaction commits

- **WHEN** the JPA transaction commits successfully
- **THEN** a `@TransactionalEventListener(phase = AFTER_COMMIT)` listener SHALL
  persist the event to `outbox_event` with `published_at = NULL`

### Scenario: Outbox row is not created on transaction rollback

- **WHEN** the JPA transaction rolls back (e.g., due to a domain validation
  failure)
- **THEN** no `outbox_event` row SHALL be created

---

## Requirement: Kafka event publishing via outbox poller

A `@Scheduled` poller SHALL run every 1 second, query all `outbox_event` rows
where `published_at IS NULL`, and publish each to its Kafka topic using
`KafkaTemplate`. On success the row SHALL be marked with `published_at = NOW()`.
On failure the `retry_count` SHALL be incremented.

### Scenario: Successful publish marks row as published

- **WHEN** `KafkaTemplate.send()` completes without exception for an outbox row
- **THEN** the row's `published_at` SHALL be set to the current timestamp

### Scenario: Failed publish increments retry count

- **WHEN** `KafkaTemplate.send()` throws or times out
- **THEN** the row's `retry_count` SHALL be incremented and `last_error` SHALL
  be updated; the row SHALL remain with `published_at = NULL` for the next poll
  cycle

### Scenario: Event moved to Dead Letter Topic after max retries

- **WHEN** a row's `retry_count` exceeds 3
- **THEN** the event payload SHALL be published to `{original-topic}-dlt` and
  the row SHALL be marked with `published_at = NOW()` to unblock the poller

---

## Requirement: CloudEvents 1.0 binary content mode via official SDK

Every Kafka message published by any service SHALL conform to CloudEvents 1.0
**binary content mode** using the official CloudEvents Java SDK
(`io.cloudevents:cloudevents-kafka`). CloudEvents attributes SHALL be placed in
Kafka message **headers** (prefixed `ce_`). The Kafka message **value** SHALL
contain only the event `data` payload as raw JSON bytes. The `type` attribute
SHALL include the version suffix (e.g.,
`io.github.phunguy65.zms.user.registered.v1`). The Kafka message key SHALL be
the `aggregateId` string.

### Scenario: Published message uses binary content mode

- **WHEN** an event is published to Kafka
- **THEN** the Kafka message headers SHALL contain at minimum:
    - `ce_specversion` = `"1.0"`
    - `ce_id` = UUIDv7 string
    - `ce_type` = namespaced event type with version suffix
    - `ce_source` = service name (e.g., `user-management`)
    - `ce_time` = ISO-8601 timestamp
    - `ce_subject` = aggregate ID string
    - `ce_datacontenttype` = `"application/json"`
- **AND** the Kafka message value SHALL be the raw JSON bytes of the event
  payload object (e.g., `{"userId":"...","email":"..."}`) with no CloudEvents
  wrapper in the body

### Scenario: Kafka message key is the aggregate ID

- **WHEN** an event is published to Kafka
- **THEN** the Kafka record key SHALL be the string representation of
  `aggregateId()` to ensure partition ordering per aggregate

---

## Requirement: Kafka producer reliability configuration

The Kafka producer in each service SHALL be configured for at-least-once
delivery with producer-level idempotence.

### Scenario: Producer is configured for durability

- **WHEN** the application starts
- **THEN** the Kafka producer SHALL have `acks=all`, `retries=3`, and
  `enable.idempotence=true`

---

## Requirement: user-management domain events

The `user-management` service SHALL publish the following domain events to
Kafka:

| Event                 | Topic                             | Trigger                         |
| --------------------- | --------------------------------- | ------------------------------- |
| `UserRegisteredEvent` | `user-management.user.registered` | `RegisterUserUseCase` succeeds  |
| `UserLoggedInEvent`   | `user-management.user.logged-in`  | `LoginUserUseCase` succeeds     |
| `UserDeletedEvent`    | `user-management.user.deleted`    | `DeleteAccountUseCase` succeeds |

### Scenario: UserRegistered event published on successful registration

- **WHEN** `RegisterUserUseCase.execute()` completes with `Result.success()`
- **THEN** a `UserRegisteredEvent` SHALL be registered on the `User` aggregate
  and eventually published to `user-management.user.registered` with payload
  `{userId, email, fullName, registeredAt}`

### Scenario: UserLoggedIn event published on successful login

- **WHEN** `LoginUserUseCase.execute()` completes with `Result.success()`
- **THEN** a `UserLoggedInEvent` SHALL be published to
  `user-management.user.logged-in` with payload `{userId, email, loginAt}`

### Scenario: UserDeleted event published on successful account deletion

- **WHEN** `DeleteAccountUseCase.execute()` completes with `Result.success()`
- **THEN** a `UserDeletedEvent` SHALL be published to
  `user-management.user.deleted` with payload `{userId, email, deletedAt}`

### Scenario: No event published when use case fails

- **WHEN** any use case returns `Result.failure()`
- **THEN** no domain event SHALL be registered and no outbox row SHALL be
  created

---

## Requirement: meeting-management domain events

The `meeting-management` service SHALL publish domain events to Kafka using the
same Transactional Outbox pattern and CloudEvents 1.0 binary content mode as
`user-management`. Specific event types SHALL be determined during
implementation based on the meeting domain model.

### Scenario: Meeting domain events follow the same publishing pattern

- **WHEN** a meeting use case completes successfully and registers a
  `PublishableEvent`
- **THEN** the event SHALL be persisted to `outbox_event` and published to a
  topic following the pattern `meeting-management.meeting.{event-name}`

---

## Requirement: chat-management domain events

The `chat-management` service SHALL publish domain events to Kafka using the
same Transactional Outbox pattern and CloudEvents 1.0 binary content mode as
`user-management`. Specific event types SHALL be determined during
implementation based on the chat domain model.

### Scenario: Chat domain events follow the same publishing pattern

- **WHEN** a chat use case completes successfully and registers a
  `PublishableEvent`
- **THEN** the event SHALL be persisted to `outbox_event` and published to a
  topic following the pattern `chat-management.{aggregate}.{event-name}`

---

## Requirement: KRaft Kafka in local development environment

The `compose.yaml` SHALL include a single Kafka container running in KRaft mode
(no Zookeeper). A Kafka UI container SHALL also be included for local debugging.
All AMQP/RabbitMQ containers SHALL be removed.

### Scenario: Kafka starts without Zookeeper

- **WHEN** `docker compose up` is run
- **THEN** Kafka SHALL start using KRaft mode with
  `KAFKA_PROCESS_ROLES=broker,controller` and no Zookeeper dependency

### Scenario: Kafka UI is accessible for local debugging

- **WHEN** `docker compose up` is run
- **THEN** Kafka UI SHALL be accessible at `http://localhost:8090` and SHALL
  display the Kafka cluster and topics

---

## Requirement: AMQP dependencies removed from build

All `spring-boot-starter-amqp`, `spring-rabbit-stream`, and
`spring-integration-amqp` dependencies SHALL be removed from the `build-logic`
service base convention plugin. `spring-kafka` SHALL be added in their place.

### Scenario: Services compile without AMQP on the classpath

- **WHEN** any service is built with `./gradlew build`
- **THEN** the build SHALL succeed with no AMQP classes on the compile or
  runtime classpath

### Scenario: spring-kafka is available to all services

- **WHEN** any service is built
- **THEN** `KafkaTemplate`, `@KafkaListener`, and related Spring Kafka classes
  SHALL be available on the classpath via the service base convention plugin

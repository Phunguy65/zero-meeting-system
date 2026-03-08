# Tasks

## 1. Build Configuration

- [x] 1.1 Add `spring-kafka` (3.4.x), `cloudevents-core` (4.0.1), and
      `cloudevents-kafka` (4.0.1) entries to `gradle/libs.versions.toml`
- [x] 1.2 Remove `spring-boot-starter-amqp`, `spring-rabbit-stream`,
      `spring-integration-amqp` entries from `gradle/libs.versions.toml`
- [x] 1.3 Update `build-logic` service base convention plugin: replace AMQP
      dependencies with `implementation(libs.spring.kafka)`
- [x] 1.4 Verify all three services build cleanly with `./gradlew build -x test`

## 2. Local Dev Infrastructure (compose.yaml)

- [x] 2.1 Add KRaft Kafka service to `compose.yaml`
      (`confluentinc/cp-kafka:7.8.0`, `KAFKA_PROCESS_ROLES=broker,controller`,
      fixed `CLUSTER_ID`, data volume)
- [x] 2.2 Add Kafka UI service to `compose.yaml`
      (`provectuslabs/kafka-ui:latest`, port `8090:8080`)
- [x] 2.3 Remove any existing Zookeeper and RabbitMQ services from
      `compose.yaml`
- [x] 2.4 Verify `docker compose up` starts Kafka and Kafka UI successfully

## 3. Shared Module — Domain Abstractions

- [x] 3.1 Create `PublishableEvent` interface in
      `io.github.phunguy65.zms.shared.domain` extending `DomainEvent` with
      methods: `eventId()`, `aggregateId()`, `aggregateType()`, `eventType()`,
      `topic()`
- [x] 3.2 Create `EventPublisher` port interface in
      `io.github.phunguy65.zms.shared.domain` with `publish(PublishableEvent)`
      and default `publishAll(List<PublishableEvent>)`
- [x] 3.3 Create `NoOpEventPublisher` in
      `io.github.phunguy65.zms.shared.infrastructure.messaging` annotated
      `@Component @ConditionalOnMissingBean(EventPublisher.class)`, logs a
      warning per event

## 4. user-management — Domain Events

- [x] 4.1 Create `UserRegisteredEvent` record in `domain/event/` implementing
      `PublishableEvent` with fields: `eventId`, `aggregateId` (userId),
      `email`, `fullName`, `registeredAt`
- [x] 4.2 Create `UserLoggedInEvent` record in `domain/event/` implementing
      `PublishableEvent` with fields: `eventId`, `aggregateId` (userId),
      `email`, `loginAt`
- [x] 4.3 Create `UserDeletedEvent` record in `domain/event/` implementing
      `PublishableEvent` with fields: `eventId`, `aggregateId` (userId),
      `email`, `deletedAt`
- [x] 4.4 Call `registerEvent(new UserRegisteredEvent(...))` inside
      `User.register()` factory method
- [x] 4.5 Call `registerEvent(new UserDeletedEvent(...))` inside the `User`
      delete method (or in `DeleteAccountUseCase` after save)
- [x] 4.6 Publish `UserLoggedInEvent` in `LoginUserUseCase` after successful
      token issuance (via `ApplicationEventPublisher`)

## 5. user-management — Outbox Infrastructure

- [x] 5.1 Create `OutboxEventEntity` JPA entity in `infrastructure/persistence/`
      with columns: `id` (BIGINT identity), `aggregate_id` (UUID),
      `aggregate_type`, `event_type`, `topic`, `payload` (TEXT), `created_at`,
      `published_at` (nullable), `retry_count`, `last_error`
- [x] 5.2 Create `OutboxEventRepository` Spring Data JPA interface with
      `findAllByPublishedAtIsNull()`
- [x] 5.3 Create Flyway migration `V{N}__create_outbox_event_table.sql` in
      `db/migration/` (PostgreSQL)
- [x] 5.4 Create H2-compatible Flyway migration in `db/h2-migration/` for test
      environment
- [x] 5.5 Create `OutboxEventListener` in `infrastructure/messaging/` with
      `@TransactionalEventListener(phase = AFTER_COMMIT)` that persists
      `PublishableEvent` to `outbox_event`

## 6. user-management — Kafka Adapter

- [x] 6.1 Add `implementation(libs.cloudevents.kafka)` to `user-management`
      `build.gradle.kts` (or via the service base convention plugin if shared
      across all services)
- [x] 6.2 Create `KafkaEventPublisher` in `infrastructure/messaging/`
      implementing `EventPublisher`, using `KafkaTemplate<String, CloudEvent>`
      (with `CloudEventSerializer` as value serializer) to send a `CloudEvent`
      built via `CloudEventBuilder.v1()` with `aggregateId` as key
- [x] 6.3 Create `KafkaConfig` in `infrastructure/config/` defining
      `KafkaTemplate<String, CloudEvent>` bean with `CloudEventSerializer` as
      value serializer and `StringSerializer` as key serializer
- [x] 6.4 Add Kafka producer properties to `application.properties`:
      `bootstrap-servers`, `acks=all`, `retries=3`, `enable.idempotence=true`,
      key serializer (`StringSerializer`), value serializer
      (`CloudEventSerializer`)
- [x] 6.5 Create `OutboxEventPublisher` in `infrastructure/messaging/` with
      `@Scheduled(fixedDelay = 1000)` that polls unpublished rows, calls
      `KafkaTemplate.send()`, marks published, handles DLT after 3 retries
- [x] 6.6 Enable scheduling in `UserManagementApplication` with
      `@EnableScheduling`
- [x] 6.7 Wire `ApplicationEventPublisher` into use cases that need to emit
      events (`RegisterUserUseCase`, `LoginUserUseCase`, `DeleteAccountUseCase`)

## 7. user-management — Tests

- [x] 7.1 Unit test `UserRegisteredEvent`, `UserLoggedInEvent`,
      `UserDeletedEvent` — verify all `PublishableEvent` fields are populated
      correctly
- [x] 7.2 Unit test `OutboxEventListener` — verify it persists an outbox row
      when a `PublishableEvent` is received
- [x] 7.3 Unit test `KafkaEventPublisher` — verify `KafkaTemplate.send()` is
      called with correct topic, key, and that the `CloudEvent` value has the
      expected `ce_type`, `ce_source`, `ce_id` attributes and correct `data`
- [x] 7.4 Unit test `OutboxEventPublisher` — verify retry logic and DLT routing
      after 3 failures
- [x] 7.5 Integration test `RegisterUserUseCase` — verify `outbox_event` row is
      created after successful registration

## 8. meeting-management — Kafka Event Publishing

- [x] 8.1 Analyze `meeting-management` domain model to identify publishable
      events (e.g., `MeetingCreated`, `MeetingEnded`, `MeetingCancelled`)
- [x] 8.2 Create domain event records in `domain/event/` implementing
      `PublishableEvent`
- [x] 8.3 Register events in relevant aggregate methods or use cases
- [x] 8.4 Create `OutboxEventEntity`, `OutboxEventRepository`, Flyway migrations
      (PostgreSQL + H2)
- [x] 8.5 Create `OutboxEventListener`, `KafkaEventPublisher`, `KafkaConfig`,
      `OutboxEventPublisher` (same pattern as user-management, using
      `KafkaTemplate<String, CloudEvent>` with `CloudEventSerializer`)
- [x] 8.6 Add Kafka producer properties to `application.properties`
- [x] 8.7 Enable `@EnableScheduling` in `MeetingManagementApplication`
- [x] 8.8 Write unit tests for domain events and outbox infrastructure

## 9. chat-management — Kafka Event Publishing

- [x] 9.1 Analyze `chat-management` domain model to identify publishable events
      (e.g., `MessageSent`, `RoomCreated`)
- [x] 9.2 Create domain event records in `domain/event/` implementing
      `PublishableEvent`
- [x] 9.3 Register events in relevant aggregate methods or use cases
- [x] 9.4 Create `OutboxEventEntity`, `OutboxEventRepository`, Flyway migrations
      (PostgreSQL + H2)
- [x] 9.5 Create `OutboxEventListener`, `KafkaEventPublisher`, `KafkaConfig`,
      `OutboxEventPublisher` (same pattern as user-management, using
      `KafkaTemplate<String, CloudEvent>` with `CloudEventSerializer`)
- [x] 9.6 Add Kafka producer properties to `application.properties`
- [x] 9.7 Enable `@EnableScheduling` in `ChatManagementApplication`
- [x] 9.8 Write unit tests for domain events and outbox infrastructure

## 10. Verification

- [x] 10.1 Run `./gradlew build` across all services — all tests pass, no AMQP
      classes on classpath
- [x] 10.2 Start `docker compose up`, register a user via REST, verify
      `outbox_event` row is created in DB
- [x] 10.3 Verify the event appears in Kafka UI at `http://localhost:8090` on
      topic `user-management.user.registered`
- [x] 10.4 Verify CloudEvents binary content mode in the Kafka message: Kafka
      headers contain `ce_specversion`, `ce_id`, `ce_type`, `ce_source`,
      `ce_time`, `ce_subject`; message value contains only the event data JSON
- [x] 10.5 Simulate Kafka downtime: verify outbox row stays unpublished, then
      publishes when Kafka recovers

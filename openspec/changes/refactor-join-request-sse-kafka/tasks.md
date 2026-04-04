# Tasks

## 1. Domain Events

- [x] 1.1 Create `JoinRequestExpiredEvent.java` in `domain/event/` implementing
      `PublishableEvent` with `eventId`, `meetingId`, `joinRequestId`,
      `occurredAt` fields; `aggregateId()=meetingId`,
      `aggregateType()="meeting"`,
      `eventType()="io.github.phunguy65.zms.meeting.join_request_expired.v1"`,
      `topic()="meeting-management.join_request.expired"`
- [x] 1.2 Add `liveKitToken: String` field to `JoinRequestApprovedEvent.java`
      (add after `approvedBy` field) — this is a record, update the canonical
      constructor

## 2. Consul-driven SSE Configuration

- [x] 2.1 Create `SseProperties.java` in `infrastructure/config/` as
      `@Component @RefreshScope @ConfigurationProperties(prefix = "app.sse")`
      with fields `timeoutMs = 300_000L` and `joinRequestTimeoutMs = 600_000L`
      with standard getters/setters (not record — needs setters for
      `@ConfigurationProperties` binding)
- [x] 2.2 Add default config properties to `application.properties`:
      `app.sse.timeout-ms=300000` and `app.sse.join-request-timeout-ms=600000`

## 3. Kafka Consumer Configuration

- [x] 3.1 Add `ConsumerFactory<String, CloudEvent>` bean to `KafkaConfig.java`:
      `StringDeserializer` for key, `CloudEventDeserializer`
      (`io.cloudevents.kafka.CloudEventDeserializer`) for value,
      `GROUP_ID_CONFIG = "meeting-sse-" + UUID.randomUUID().toString()`
      (evaluated once at startup), `AUTO_OFFSET_RESET_CONFIG = "latest"`
- [x] 3.2 Add `ConcurrentKafkaListenerContainerFactory<String, CloudEvent>` bean
      named `cloudEventKafkaListenerContainerFactory` to `KafkaConfig.java`
      using the consumer factory from 3.1

## 4. Rewrite MeetingSseManager

- [x] 4.1 Remove `RedisMessageListenerContainer` dependency and inner
      `RedisEventListener` class from `MeetingSseManager`; add `SseProperties`
      injection; rename `subscribe(UUID meetingId, UUID userId)` to
      `subscribeHost(UUID meetingId, UUID userId)` and replace hardcoded
      `SSE_TIMEOUT_MS = 300_000L` with `sseProperties.getTimeoutMs()`
- [x] 4.2 Add GUEST emitter registry
      `ConcurrentHashMap<UUID, SseEmitter> guestEmittersByRequest` and
      `subscribeGuest(UUID requestId)` method: creates
      `SseEmitter(sseProperties.getJoinRequestTimeoutMs())`, registers in map,
      adds `onCompletion`/`onTimeout`/`onError` callbacks that call
      `guestEmittersByRequest.remove(requestId)`, returns emitter
- [x] 4.3 Add
      `@KafkaListener(topics = "meeting-management.join_request.created", containerFactory = "cloudEventKafkaListenerContainerFactory")`
      method `onJoinRequestCreated(CloudEvent event)`: deserialize data payload
      to `JoinRequestCreatedEvent` using `objectMapper`; extract `meetingId`;
      push `SseEmitter.event().name("join_request_created").data(sseData)` to
      all HOST emitters for that meeting; dead emitter cleanup on IOException
- [x] 4.4 Add
      `@KafkaListener(topics = "meeting-management.join_request.approved", ...)`
      method `onJoinRequestApproved(CloudEvent event)`: deserialize to
      `JoinRequestApprovedEvent`; extract `joinRequestId`; push
      `SseEmitter.event().name("join_request_approved").data({requestId, status:"APPROVED", liveKitToken})`
      to GUEST emitter for that requestId; call `emitter.complete()` to close
      the stream after sending; remove from map
- [x] 4.5 Add
      `@KafkaListener(topics = "meeting-management.join_request.denied", ...)`
      method `onJoinRequestDenied(CloudEvent event)`: deserialize to
      `JoinRequestDeniedEvent`; push `event name("join_request_denied")` to
      GUEST emitter; call `emitter.complete()` and remove from map
- [x] 4.6 Add
      `@KafkaListener(topics = "meeting-management.join_request.expired", ...)`
      method `onJoinRequestExpired(CloudEvent event)`: deserialize to
      `JoinRequestExpiredEvent`; push `event name("join_request_expired")` to
      HOST emitters (meeting-level) AND GUEST emitter (request-level); call
      `emitter.complete()` on GUEST emitter and remove from map

## 5. Remove Redis Pub/Sub Publisher

- [x] 5.1 Delete `infrastructure/sse/RedisSseEventPublisher.java`
- [x] 5.2 Remove `RedisMessageListenerContainer` bean from `RedisConfig.java`
      (keep `StringRedisTemplate` bean — still needed for
      `JoinRequestRedisRepository`)
- [x] 5.3 Remove `RedisSseEventPublisher` field, constructor parameter, and
      `sseEventPublisher.publish()` call from `RequestJoinUseCase.java`
- [x] 5.4 Remove `RedisSseEventPublisher` field, constructor parameter, and
      `sseEventPublisher.publish()` call from `ApproveJoinRequestUseCase.java`;
      pass `token` to `JoinRequestApprovedEvent` constructor (new field from
      task 1.2)
- [x] 5.5 Remove `RedisSseEventPublisher` field, constructor parameter, and
      `sseEventPublisher.publish()` calls from
      `ApproveAllJoinRequestsUseCase.java`; pass each `token` to
      `JoinRequestApprovedEvent` constructors
- [x] 5.6 Remove `RedisSseEventPublisher` field, constructor parameter, and
      `sseEventPublisher.publish()` call from `DenyJoinRequestUseCase.java`
- [x] 5.7 Remove `RedisSseEventPublisher` field, constructor parameter, and
      `sseEventPublisher.publish()` calls from
      `MeetingEndedJoinRequestHandler.java`; inject `ApplicationEventPublisher`
      instead; publish `JoinRequestDeniedEvent` per pending request; add
      `@Transactional` annotation to `handle()` method
- [x] 5.8 Remove `RedisSseEventPublisher` field and
      `sseEventPublisher.publish()` call from `JoinRequestCleanupJob.java`;
      inject `ApplicationEventPublisher` instead; publish
      `JoinRequestExpiredEvent` (from task 1.1) per expired request; add
      `@Transactional` annotation to `cleanupExpiredRequests()` method

## 6. Update Presentation Layer

- [x] 6.1 Add guest SSE endpoint
      `GET /{version}/joinRequests/{requestId}/events` to
      `JoinRequestController.java` (no auth required): call
      `meetingSseManager.subscribeGuest(requestId)` and return
      `ResponseEntity.ok().contentType(TEXT_EVENT_STREAM).body(emitter)`
- [x] 6.2 Update host SSE endpoint in `JoinRequestController.java`: change call
      from `meetingSseManager.subscribe(id, userId)` to
      `meetingSseManager.subscribeHost(id, userId)` (rename from task 4.1)
- [x] 6.3 Remove `PollJoinRequestStatusUseCase` field, constructor parameter,
      and `GET /{version}/joinRequests/{requestId}` polling endpoint from
      `JoinRequestController.java`
- [x] 6.4 Delete `application/usecase/PollJoinRequestStatusUseCase.java`

## 7. Kafka Topic K8s Manifests

- [x] 7.1 Add `KafkaTopic` resource for
      `meeting-management.join_request.created` to
      `services/k8s/kafka/kafka-topics.yaml`: `partitions: 3`, `replicas: 1`,
      `retention.ms: 1800000`, `cleanup.policy: delete`
- [x] 7.2 Add `KafkaTopic` resource for
      `meeting-management.join_request.approved` to
      `services/k8s/kafka/kafka-topics.yaml`: same config as 7.1
- [x] 7.3 Add `KafkaTopic` resource for `meeting-management.join_request.denied`
      to `services/k8s/kafka/kafka-topics.yaml`: same config as 7.1
- [x] 7.4 Add `KafkaTopic` resource for
      `meeting-management.join_request.expired` to
      `services/k8s/kafka/kafka-topics.yaml`: same config as 7.1

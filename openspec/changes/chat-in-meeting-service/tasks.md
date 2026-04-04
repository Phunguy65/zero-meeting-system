# Tasks: Chat-in-Meeting Service

## Legend

- `[ ]` = pending
- `[x]` = complete
- `(verify: <checkpoint>)` = verification checkpoint after this task group

---

## Phase A: Shared Module — Fix CleanArchitectureTest

### A1. Add @Document ArchUnit Rules

> **Approach:** User chose "keep @Document in domain, fix ArchUnit rule." The
> current `CleanArchitectureTest` only checks `@Entity` — `@Document` is
> invisible to ArchUnit. The fix: add rules that **govern** `@Document`
> placement without **blocking** it from domain (as per user's decision).

Add rules to
`services/shared/src/testFixtures/java/io/github/phunguy65/zms/shared/architecture/CleanArchitectureTest.java`:

- Add `import org.springframework.data.mongodb.core.mapping.Document`
- Add `document_naming_guide` rule: classes with `@Document` in
  infrastructure.persistence should be named `*Document` (informational,
  `allowEmptyShould(true)`)
- **Do NOT** add `document_must_be_in_infrastructure_persistence` — would block
  @Document from domain
- **Do NOT** add `domain_must_not_have_document` — would block @Document from
  domain

> **Effect:** After Phase A, `@Document` in domain/model is explicitly allowed.
> Existing `ChatRoom.java` and `ChatMessage.java` remain valid.

---

## Phase B: meeting-management — Add Participant Events

### B1. Create ParticipantJoinedEvent

Create
`services/meeting-management/src/main/java/io/github/phunguy65/zms/meetingmanagement/domain/event/ParticipantJoinedEvent.java`:

- `record ParticipantJoinedEvent(UUID eventId, UUID meetingId, UUID userId, String displayName, Instant occurredAt)`
- Implements `PublishableEvent`
- `topic()`: `"meeting-management.participant.joined"` (follows existing
  convention)
- `eventType()`: `"io.github.phunguy65.zms.meeting.participant_joined.v1"`

### B2. Create ParticipantLeftEvent

Create
`services/meeting-management/src/main/java/io/github/phunguy65/zms/meetingmanagement/domain/event/ParticipantLeftEvent.java`:

- `record ParticipantLeftEvent(UUID eventId, UUID meetingId, @Nullable UUID userId, @Nullable String displayName, Instant occurredAt)`
- Implements `PublishableEvent`
- `topic()`: `"meeting-management.participant.left"` (follows existing
  convention)
- `eventType()`: `"io.github.phunguy65.zms.meeting.participant_left.v1"`

### B3. Publish ParticipantJoinedEvent from LiveKit Webhook Handler

Read
`services/meeting-management/src/main/java/io/github/phunguy65/zms/meetingmanagement/presentation/LiveKitWebhookController.java`
to find where `participant_joined` is handled.

Steps:

1. Add `EventPublisher` (from `domain.port.EventPublisher`) to
   `LiveKitWebhookController` constructor
2. After `assignSidUseCase.execute()` returns successfully, build and publish:
    ```java
    ParticipantJoinedEvent event = new ParticipantJoinedEvent(
        UUID.randomUUID(), meetingId, userId, displayName, Instant.now());
    eventPublisher.publish(event);
    ```
3. Use `domain.port.EventPublisher`, NOT `ApplicationEventPublisher` (Spring
   internal bus)

### B4. Publish ParticipantLeftEvent from LiveKit Webhook Handler

Read
`services/meeting-management/src/main/java/io/github/phunguy65/zms/meetingmanagement/presentation/LiveKitWebhookController.java`
to find where `participant_left` is handled.

Steps:

1. After `leaveMeetingUseCase.execute()` returns success, build and publish:
    ```java
    ParticipantLeftEvent event = new ParticipantLeftEvent(
        UUID.randomUUID(), meetingId, userId, displayName, Instant.now());
    eventPublisher.publish(event);
    ```
2. Use `domain.port.EventPublisher`, NOT `ApplicationEventPublisher`

### B5. Add EventPublisher to LiveKitWebhookController Constructor

Update `LiveKitWebhookController` constructor to inject `EventPublisher`:

````java
private final EventPublisher eventPublisher;  // Kafka, not ApplicationEventPublisher

public LiveKitWebhookController(..., EventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
    ...
}

---

## Phase C: chat-management — Build Configuration

### C1. Update build.gradle.kts

Modify `services/chat-management/build.gradle.kts`:

- Add `implementation(libs.spring.boot.starter.data.mongodb)`
- Add `implementation(libs.spring.kafka)`
- Add `implementation(libs.cloudevents.kafka)`
- Add `implementation(libs.jjwt.api)`
- Add `implementation(libs.livekit.server)`
- Add `implementation(libs.spring.boot.starter.validation)`
- Add `runtimeOnly(libs.jjwt.impl)`
- Add `runtimeOnly(libs.jjwt.jackson)`
- Add `testImplementation(libs.spring.boot.data.mongodb.test)`
- Add `testImplementation(libs.testcontainers.mongodb)`
- Add `testImplementation(libs.archunit.junit5)` (verify service.base already
  includes it)

### C2. Update compose.yaml

Replace contents of `services/chat-management/compose.yaml`:

- Change postgres service to `mongodb` (image: mongo:8.0, port 27017)
- Add `kafka` service (confluentinc/cp-kafka:7.9.0, port 9092) with KRaft config
- Remove postgres references

### C3. Update init-mongo.js

Update `services/chat-management/init-mongo.js`:

- Keep existing indexes on `chat_messages` (idx_room_seqnum, idx_room_created_at, idx_ttl_30d)
- Keep existing indexes on `chat_rooms` (idx_room_id, idx_meeting_id)
- No additional index needed for `seq_counters` — MongoDB auto-creates `_id` as unique index

> **Note:** `seq_counters` collection is auto-created by `upsert: true` in `findAndModify`. MongoDB implicitly creates `_id` as a unique index, which is exactly what we need.

### C4. Create application.yaml

Create `services/chat-management/src/main/resources/application.yaml`:

- `server.port: 8082`
- `spring.data.mongodb.uri: mongodb://localhost:27017/chat_management`
- `spring.kafka.bootstrap-servers: localhost:9092`
- `app.livekit.*` properties
- `app.chat.jwt-secret` property

---

## Phase D: chat-management — Domain Model Cleanup

> **Approach:** Keep `@Document` in domain (as per user's decision). Only remove settings-related code (RoomSettings deleted). Phase A added ArchUnit rules that govern @Document naming without blocking it from domain.

### D1. Update ChatRoom.java

Modify
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/domain/model/ChatRoom.java`:

- Keep: `@Document`, `@Id`, `@Indexed`, `@CreatedDate`, `@LastModifiedDate`
- Remove: `settings` field and `getSettings()` method (RoomSettings deleted)
- Keep: factory method `create()`, constants `RoomStatus`, all getters/setters
- Keep: `setStatus(String)` for closing room

### D2. Update ChatMessage.java

Modify
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/domain/model/ChatMessage.java`:

- Keep all existing structure — no changes needed (already correct)

### D3. Delete RoomSettings

Delete
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/domain/model/RoomSettings.java`.

---

## Phase E: chat-management — Infrastructure (Persistence)

> **Approach:** `@Document` stays in domain (Phase D). Adapters use `MongoTemplate` directly on domain model classes (`ChatRoom`, `ChatMessage`) — Spring Data MongoDB maps `@Document` classes directly without wrapper classes.

### E1. Create MongoChatRoomAdapter

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/persistence/MongoChatRoomAdapter.java`:

- `@Repository`
- Implements `ChatRoomRepository`
- Constructor: `MongoTemplate`
- `save(ChatRoom)`: `mongo.save(room)`
- `findByRoomId(String)`: `mongo.findOne(Query(Criteria.where("roomId").is(roomId)), ChatRoom.class)`
- `existsByRoomId(String)`: `mongo.exists(Query(Criteria.where("roomId").is(roomId)), ChatRoom.class)`

### E2. Create MongoChatMessageAdapter

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/persistence/MongoChatMessageAdapter.java`:

- `@Repository`
- Implements `ChatMessageRepository`
- Constructor: `MongoTemplate`
- `save(ChatMessage)`: `mongo.save(message)`
- `findByRoomId(String, ScrollParams, Optional<String>)`: cursor pagination using `seqNum`
  - With `beforeSeqNum`: `Query(Criteria.where("roomId").is(roomId).and("seqNum").lt(beforeSeqNum))`
  - Sort by `seqNum` descending, limit `pageSize + 1`
  - Return `hasNext = items.size() > pageSize`
  - Map to `CursorPageResponse`
- Note: `@Document` annotated domain classes work directly with `MongoTemplate`

### E3. Create MongoSeqCounterAdapter

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/persistence/MongoSeqCounterAdapter.java`:

- `@Component` (not `@Repository` — not a repository per se)
- Constructor: `MongoTemplate`
- `long nextSeq(String roomId)`:
  ```java
  Query query = new Query(Criteria.where("_id").is(roomId));
  Update update = new Update().inc("seq", 1);
  FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);
  SeqCounter result = mongoTemplate.findAndModify(query, update, options, SeqCounter.class);
  return result != null ? result.getSeq() : 1L;
````

- `SeqCounter` is a private static inner class or top-level
  `@Document(collection = "seq_counters")` class with `_id` (String) and `seq`
  (Long) fields
- MongoDB auto-creates `_id` as unique index — no init script needed

---

## Phase F: chat-management — Application Ports

### F1. Create ChatRoomRepository Port

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/port/ChatRoomRepository.java`:

- Interface
- Methods: `ChatRoom save(ChatRoom)`, `Optional<ChatRoom> findByRoomId(String)`,
  `boolean existsByRoomId(String)`

### F2. Create ChatMessageRepository Port

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/port/ChatMessageRepository.java`:

- Interface
- Methods: `ChatMessage save(ChatMessage)`,
  `CursorPageResponse<ChatMessage> findByRoomId(String, ScrollParams, Optional<String>)`

### F3. Create ChatLiveKitPort

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/port/ChatLiveKitPort.java`:

- Interface
- Method:
  `Result<Void, ChatError> broadcastMessage(String roomName, ChatMessage message)`

---

## Phase G: chat-management — Use Cases

### G1. Create OpenChatRoomUseCase

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/usecase/OpenChatRoomUseCase.java`:

- `@Service`
- Constructor: `ChatRoomRepository`
- `void execute(String meetingId)` — idempotent: check `existsByRoomId`, create
  if absent
- Called by `MeetingEventConsumer.onMeetingStarted()`

### G2. Create CloseChatRoomUseCase

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/usecase/CloseChatRoomUseCase.java`:

- `@Service`
- Constructor: `ChatRoomRepository`
- `void execute(String meetingId)` — set status to `ARCHIVED`, idempotent (no-op
  if not found)
- Called by `MeetingEventConsumer.onMeetingEnded()`

### G3. Create SendMessageUseCase

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/usecase/SendMessageUseCase.java`:

- `@Service`
- Constructor: `ChatRoomRepository`, `ChatMessageRepository`, `ChatLiveKitPort`,
  `MongoTemplate`
- Constants: `MAX_MESSAGE_LENGTH = 4000`, `RATE_LIMIT_PER_MINUTE = 10`
- `Result<ChatMessage, ChatError> execute(String roomId, String senderId, String senderName, String content, Long replyToSeqNum)`
- Validation chain: room exists → room ACTIVE → rate limit → content length →
  save → broadcast
- Rate limit: count messages in last 60s per sender via `mongoTemplate.count()`
- Broadcast via `ChatLiveKitPort` — save succeeds even if broadcast fails
  (MongoDB is authoritative)
- Generate `seqNum` via `MongoSeqCounterAdapter`

### G4. Create GetMessagesUseCase

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/usecase/GetMessagesUseCase.java`:

- `@Service`
- Constructor: `ChatRoomRepository`, `ChatMessageRepository`
- `Result<CursorPageResponse<ChatMessage>, ChatError> execute(String roomId, ScrollParams params, Optional<String> beforeSeqNum)`
- Returns `ChatError.RoomNotFound` if room doesn't exist

### G5. Create CreateSystemMessageUseCase

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/usecase/CreateSystemMessageUseCase.java`:

- `@Service`
- Constructor: `ChatRoomRepository`, `ChatMessageRepository`, `ChatLiveKitPort`,
  `MongoTemplate`
- `void execute(String roomId, String content)` — only if room exists and is
  ACTIVE
- Generates `seqNum` via `MongoSeqCounterAdapter`
- Creates `ChatMessage.systemMessage()`
- Saves and broadcasts

### G6. Create GetRoomUseCase

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/application/usecase/GetRoomUseCase.java`:

- `@Service`
- Constructor: `ChatRoomRepository`
- `Result<ChatRoom, ChatError> execute(String roomId)`
- Returns `ChatError.RoomNotFound` if not found

---

## Phase H: chat-management — Infrastructure (Kafka)

### H1. Create KafkaConfig

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/config/KafkaConfig.java`:

> **⚠️ CRITICAL: Use FIXED group IDs, NOT `UUID.randomUUID()`** Unlike
> meeting-management's SSE config (which uses `UUID.randomUUID()` per instance
> so all instances receive all events for fan-out), chat-management is a
> stateful service. Use **fixed group IDs** so Kafka partitions events across
> instances — each message processed exactly once.
>
> - `groupId = "chat-management-meeting"` (or `"chat-management-participant"`)
> - `auto.offset.reset = earliest` (chat needs all historical events when
>   starting up)
> - `enable.idempotence = true` on producer side

- Follow meeting-management pattern for beans:
  `cloudEventKafkaListenerContainerFactory`, `cloudEventKafkaTemplate`
- Use `CloudEventDeserializer` / `CloudEventSerializer` from `cloudevents.kafka`
- Kafka producer: `acks=all`, `retries=3`, `enable.idempotence=true`
- Consumer: fixed `groupId` (not UUID), `auto.offset.reset=earliest`

### H2. Create MeetingEventConsumer

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/messaging/MeetingEventConsumer.java`:

- `@Component`
- `@KafkaListener` for `meeting-management.meeting.started` (groupId:
  `chat-management-meeting-started`)
- `@KafkaListener` for `meeting-management.meeting.ended` (groupId:
  `chat-management-meeting-ended`)
- Both use `cloudEventKafkaListenerContainerFactory`
- Deserialize CloudEvent → call `OpenChatRoomUseCase` / `CloseChatRoomUseCase`
- Follow same `deserialize()` pattern as `MeetingSseManager`

### H3. Create ParticipantEventConsumer

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/messaging/ParticipantEventConsumer.java`:

- `@Component`
- Three `@KafkaListener` methods with full topic paths:
    - topic `"meeting-management.participant.joined"` (groupId:
      `chat-management-participant`)
    - topic `"meeting-management.participant.left"` (groupId:
      `chat-management-participant`)
    - topic `"meeting-management.participant.kicked"` (groupId:
      `chat-management-participant`)
- All use same groupId: `chat-management-participant`
- Build system message content:
    - joined: `"{displayName} đã tham gia cuộc họp"`
    - left: `"{displayName} đã rời cuộc họp"`
    - kicked: `"{displayName} đã bị xóa khỏi cuộc họp"`
- Call `CreateSystemMessageUseCase`

---

## Phase I: chat-management — Infrastructure (LiveKit + Security)

### I1. Create LiveKitProperties

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/config/LiveKitProperties.java`:

- `@Component`, `@ConfigurationProperties(prefix = "app.livekit")`
- Fields: `url`, `apiKey`, `apiSecret`
- Follow same pattern as meeting-management `LiveKitProperties`

### I2. Create ChatLiveKitAdapter

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/livekit/ChatLiveKitAdapter.java`:

- `@Component`
- Implements `ChatLiveKitPort`
- Constructor: `RoomServiceClient` (from LiveKit SDK), `ObjectMapper`
- `broadcastMessage()`: build JSON payload using `ObjectMapper` (for proper JSON
  escaping), call `roomServiceClient.sendData()`, handle KNPE bug (SDK 0.12.1)
- `buildDataPayload(ChatMessage)`: use
  `ObjectMapper.writeValueAsString(ChatPayload)` where `ChatPayload` is a
  private record — avoids manual escaping issues with `.formatted()`
- `isKnownKnpeBug(Exception)`: checks for `KotlinNullPointerException` or
  null-related error messages
- Inject `ObjectMapper` from Spring context (auto-configured by Spring Boot)

### I3. Create JwtAuthFilter

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/security/JwtAuthFilter.java`:

- Extends `OncePerRequestFilter`
- Reads `Authorization: Bearer <token>` header
- Parses JWT with `io.jsonwebtoken.Jwts` (same secret as meeting-management)
- Extracts `sub` claim as userId
- Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`

### I4. Create ChatSecurityConfig

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/security/ChatSecurityConfig.java`:

- `@Configuration`, `@EnableWebSecurity`
- `SecurityFilterChain`: CSRF disabled, stateless, permitAll `/actuator/**`,
  authenticated `/api/chat/**`
- Inject `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`

### I5. Create ChatMongoProperties

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/infrastructure/config/ChatMongoProperties.java`:

- `@Component`, `@ConfigurationProperties(prefix = "app.chat.mongo")`
- Field: `database` (default: `"chat_management"`)

---

## Phase J: chat-management — Presentation

### J1. Create ChatController

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/presentation/rest/ChatController.java`:

- `@RestController`, `@RequestMapping("/api/chat/rooms")`
- Extend pattern from meeting-management controllers
- `POST /{roomId}/messages` → `SendMessageUseCase`
- `GET /{roomId}/messages` → `GetMessagesUseCase` (with `ScrollParams` from
  `@RequestParam`)
- `GET /{roomId}` → `GetRoomUseCase`
- Map `Result` to `JsendResponse` using `Result.fold()` (success → 200, failure
  → 400)
- Extract `senderId` from
  `SecurityContextHolder.getContext().getAuthentication().getName()`

### J2. Create Request DTOs

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/presentation/rest/request/SendMessageRequest.java`:

- `record SendMessageRequest(String senderName, String content, Long replyToSeqNum)`
- No validation annotations (validation at use case layer)

### J3. Create Response DTOs

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/presentation/rest/response/ChatMessageResponse.java`:

- `record ChatMessageResponse(String id, Long seqNum, String roomId, String senderId, String senderName, String content, String type, Instant createdAt)`

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/presentation/rest/response/ChatRoomResponse.java`:

- `record ChatRoomResponse(String roomId, String meetingId, String status, Instant createdAt)`

### J4. Create ChatExceptionHandler

> **⚠️ Required — BL4 from verification.** Without this, `ChatError` variants
> map to wrong HTTP statuses (all → 500).

Create
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/presentation/rest/ChatExceptionHandler.java`
(or follow meeting-management's `GlobalExceptionHandler` pattern in `shared`):

Follow
`io.github.phunguy65.zms.shared.infrastructure.web.GlobalExceptionHandler`
pattern:

- Handle `ChatError` sealed interface via `Result.Failure` in controller — but
  add explicit exception-to-HTTP mapping as a safety net:
    - `ChatError.RoomNotFound` → HTTP 404
    - `ChatError.Unauthorized` → HTTP 403
    - `ChatError.MessageTooLong` → HTTP 400
    - `ChatError.RateLimitExceeded(retryAfter)` → HTTP 429 with `Retry-After`
      header
    - `ChatError.PersistenceFailure` → HTTP 500

If `GlobalExceptionHandler` in shared module can be extended/configured for
chat-specific errors, use that. Otherwise create a chat-specific handler in
`presentation/rest/`.

---

## Phase K: chat-management — Tests

### K1. Create ChatManagementArchitectureTest

Create
`services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/ChatManagementArchitectureTest.java`:

- `@AnalyzeClasses(packages = "io.github.phunguy65.zms.chatmanagement")`
- Extends `CleanArchitectureTest` (inherits all shared rules + new @Document
  rules from Phase A)
- Verify: domain has no `@Service`, `@Repository`, `@Entity`; application
  depends only on ports (no infrastructure)

### K2. Create OpenChatRoomUseCaseTest

Create
`services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/application/usecase/OpenChatRoomUseCaseTest.java`:

- Test: idempotent (room exists → no-op)
- Test: creates new room with ACTIVE status

### K3. Create SendMessageUseCaseTest

Create
`services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/application/usecase/SendMessageUseCaseTest.java`:

- Test: valid message → success
- Test: room not found → RoomNotFound error
- Test: room not ACTIVE → Unauthorized error
- Test: rate limit exceeded → RateLimitExceeded error
- Test: content too long → MessageTooLong error
- Test: LiveKit broadcast throws KNPE → message still saved (MongoDB
  authoritative)
- Test: LiveKit broadcast throws real error → return PersistenceFailure, message
  NOT saved (or saved — design choice: save even on failure? Document in
  SendMessageUseCase)

### K4. Create GetMessagesUseCaseTest

Create
`services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/application/usecase/GetMessagesUseCaseTest.java`:

- Test: room not found → RoomNotFound error
- Test: returns paginated messages

### K5. Create CreateSystemMessageUseCaseTest

Create
`services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/application/usecase/CreateSystemMessageUseCaseTest.java`:

- Test: room not found → no-op
- Test: room not ACTIVE → no-op
- Test: room ACTIVE → creates and broadcasts system message

### K6. Create Integration Test Config

Create
`services/chat-management/src/test/java/io/github/phunguy65/zms/chatmanagement/config/TestcontainersConfiguration.java`:

- Spring Boot test configuration for MongoDB Testcontainers
- `@TestConfiguration` with `@AutoConfigureBefore`

---

## Phase L: Final Assembly

### L1. Update ChatManagementApplication.java

Update
`services/chat-management/src/main/java/io/github/phunguy65/zms/chatmanagement/ChatManagementApplication.java`:

- Ensure
  `@SpringBootApplication(scanBasePackages = {"io.github.phunguy65.zms.chatmanagement", "io.github.phunguy65.zms.shared"})`

### L2. Verify All Imports Compile

Run `./gradlew :services:chat-management:compileJava` to verify:

- No missing imports
- No circular dependencies
- All Spring beans wired correctly

---

## Verification Checklist (manual)

After all tasks complete:

```
[ ] @Document rules added to CleanArchitectureTest.java
[ ] ChatRoom.java: @Document kept in domain, settings removed
[ ] ChatMessage.java: @Document kept in domain
[ ] RoomSettings.java: DELETED
[ ] ParticipantJoinedEvent: published from LiveKitWebhookController via EventPublisher
[ ] ParticipantLeftEvent: published from LiveKitWebhookController via EventPublisher
[ ] OpenChatRoom: idempotent
[ ] CloseChatRoom: archives room
[ ] SendMessage: all 5 validation paths work
[ ] SendMessage: message saved even if LiveKit KNPE
[ ] GetMessages: cursor pagination works
[ ] System messages: created for join/leave/kick
[ ] System messages: no-op when room inactive
[ ] JWT filter: rejects invalid, passes valid tokens
[ ] Kafka consumers: receive events from meeting-management topics
[ ] Kafka consumer group IDs: fixed (not UUID.randomUUID())
[ ] MongoDB: TTL 30d index on chat_messages.createdAt
[ ] SeqNum counter: atomic increment via findAndModify
[ ] ChatExceptionHandler: maps ChatError → correct HTTP status codes
[ ] GET /api/chat/rooms/{id} → 200 + room
[ ] POST /api/chat/rooms/{id}/messages → 200 + message
[ ] GET /api/chat/rooms/{id}/messages → paginated messages
[ ] Unauthenticated → 401
[ ] ArchUnit tests pass
[ ] ./gradlew :services:chat-management:compileJava succeeds
```

/api/chat/rooms/{id}/messages → paginated messages [ ] Unauthenticated → 401 [ ]
ArchUnit tests pass

```


```

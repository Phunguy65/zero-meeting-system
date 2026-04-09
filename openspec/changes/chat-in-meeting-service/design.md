# Design: Chat-in-Meeting Service

## 1. Architecture Overview

> **⚠️ Kafka Consumer Groups:** Chat-management KHÔNG phải SSE service — dùng
> **fixed group IDs** (`chat-management-meeting`,
> `chat-management-participant`). KHÔNG dùng `UUID.randomUUID()` như SSE pattern
> của meeting-management.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         CHAT-IN-MEETING ARCHITECTURE                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  MEETING-MANAGEMENT (PostgreSQL)          CHAT-MANAGEMENT (MongoDB)        │
│                                                                              │
│  domain/event/                                   domain/model/              │
│  + ParticipantJoinedEvent ←── NEW ──→  Kafka ──→ ParticipantEventConsumer  │
│  + ParticipantLeftEvent                         │                            │
│    MeetingStartedEvent ──── Kafka ──────────────→ MeetingEventConsumer       │
│    MeetingEndedEvent   ──── Kafka ──────────────→ MeetingEventConsumer       │
│    ParticipantKickedEvent                       │                            │
│                                              domain/model/                   │
│                                              ├── ChatRoom (@Document)       │
│                                              ├── ChatMessage (@Document)     │
│                                              ├── ChatError (sealed)         │
│                                              └── ChatErrorCode (enum)       │
│                                                                              │
│                                              application/                    │
│                                              ├── port/                       │
│                                              │   ├── ChatRoomRepository      │
│                                              │   ├── ChatMessageRepository   │
│                                              │   └── ChatLiveKitPort         │
│                                              └── usecase/                   │
│                                                  ├── OpenChatRoomUseCase     │
│                                                  ├── CloseChatRoomUseCase    │
│                                                  ├── SendMessageUseCase       │
│                                                  ├── GetMessagesUseCase      │
│                                                  └── CreateSystemMessageUseCase│
│                                                                              │
│                                              infrastructure/                 │
│                                              ├── persistence/               │
│                                              │   ├── MongoChatRoomAdapter    │
│                                              │   ├── MongoChatMessageAdapter │
│                                              │   └── MongoSeqCounterAdapter  │
│                                              ├── messaging/                 │
│                                              │   ├── MeetingEventConsumer    │
│                                              │   └── ParticipantEventConsumer│
│                                              ├── livekit/                   │
│                                              │   └── ChatLiveKitAdapter      │
│                                              ├── security/                  │
│                                              │   ├── ChatSecurityConfig      │
│                                              │   └── JwtAuthFilter           │
│                                              └── config/                     │
│                                                  ├── KafkaConfig              │
│                                                  ├── ChatMongoProperties      │
│                                                  └── LiveKitProperties        │
│                                                                              │
│                                              presentation/rest/              │
│                                              └── ChatController.java         │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  KAFKA TOPICS (consumed by chat-management)                                 │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  "meeting-management.meeting.started"      → OpenChatRoomUseCase              │
│  "meeting-management.meeting.ended"       → CloseChatRoomUseCase             │
│  "meeting-management.participant.joined" → CreateSystemMessageUseCase │
│  "meeting-management.participant.left"   → CreateSystemMessageUseCase │
│  "meeting-management.participant.kicked" → CreateSystemMessageUseCase │
│                                                                              │
│  ─────────────────────────────────────────────────────────────────────────  │
│  LIVEKIT DATA MESSAGE FLOW                                                   │
│  ─────────────────────────────────────────────────────────────────────────  │
│                                                                              │
│  Client ──POST /api/chat/rooms/{id}/messages──→ SendMessageUseCase           │
│                                                         │                    │
│                                                    Save to MongoDB           │
│                                                         │                    │
│                                              ChatLiveKitAdapter.sendData()   │
│                                                         │                    │
│                                              LiveKit broadcasts to all        │
│                                              participants in room             │
│                                                         │                    │
│                                              Android: onDataReceived()        │
│                                              callback → update UI            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## 2. Data Model

> **⚠️ @Document in domain:** `ChatRoom` và `ChatMessage` giữ `@Document`
> annotation trong `domain/model/`. CleanArchitectureTest được update để check
> `@Document` tương tự `@Entity`. Không tạo separate document classes.

### 2.1 ChatRoom

```java
@Document(collection = "chat_rooms")
public class ChatRoom {
    @Id          private String id;
    @Indexed(unique = true) private String roomId;   // = meetingId
    private String meetingId;
    private String status;                            // ACTIVE | ARCHIVED | DELETED
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    public static ChatRoom create(String meetingId) {
        return new ChatRoom(meetingId, meetingId, RoomStatus.ACTIVE.name(),
                           Instant.now(), Instant.now());
    }
}

public class RoomStatus {
    public static final String ACTIVE   = "ACTIVE";
    public static final String ARCHIVED = "ARCHIVED";
    public static final String DELETED  = "DELETED";
}
```

**Indexes** (via `@CompoundIndex` + `@Indexed`):

- `idx_room_id` — unique on `roomId`

### 2.2 ChatMessage

```java
@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_room_seqnum", def = "{'roomId': 1, 'seqNum': 1}"),
    @CompoundIndex(name = "idx_room_created_at", def = "{'roomId': 1, 'createdAt': -1}")
})
public class ChatMessage {
    @Id               private String id;
    private Long       seqNum;        // monotonic per room (MongoDB findAndModify $inc)
    @Indexed          private String roomId;
    private String     senderId;
    private String     senderName;
    private String     content;
    private String     type;          // TEXT | SYSTEM
    private MessageMetadata metadata;  // optional reply ref
    @CreatedDate
    @Indexed(expireAfter = "30d") private Instant createdAt;  // TTL 30 days
    @LastModifiedDate private Instant updatedAt;
    private boolean    deleted;
    private Instant    deletedAt;
    private String     deletedBy;

    // Factory: user-sent TEXT message
    public static ChatMessage send(Long seqNum, String roomId, String senderId,
                                   String senderName, String content, Long replyToSeqNum) { ... }

    // Factory: system-generated message
    public static ChatMessage systemMessage(Long seqNum, String roomId, String content) { ... }
}

public static class MessageType {
    public static final String TEXT   = "TEXT";
    public static final String SYSTEM = "SYSTEM";
}
```

**Indexes** (via `@CompoundIndex` + `@Indexed`):

- `idx_room_seqnum` — `{roomId, seqNum}` — cursor pagination
- `idx_room_created_at` — `{roomId, createdAt:-1}` — history query
- `idx_ttl_30d` — `createdAt` with `expireAfterSeconds: 2592000` — auto-delete

## 3. Error Model

```java
public sealed interface ChatError extends DomainError {
    record MessageTooLong(int maxLength, int actualLength) implements ChatError { ... }
    record RateLimitExceeded(int retryAfterSeconds) implements ChatError { ... }
    record RoomNotFound(String roomId) implements ChatError { ... }
    record Unauthorized(String reason) implements ChatError { ... }
    record PersistenceFailure(String detail) implements ChatError { ... }
}

public enum ChatErrorCode implements ErrorCode {
    MESSAGE_TOO_LONG,
    RATE_LIMIT_EXCEEDED,
    ROOM_NOT_FOUND,
    UNAUTHORIZED,
    PERSISTENCE_FAILURE
}
```

## 4. API Design

### 4.1 Send Message

```
POST /api/chat/rooms/{roomId}/messages
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "senderName": "Nguyen Van A",
  "content": "Hello everyone!",
  "replyToSeqNum": 41   // optional
}

200 OK
{
  "status": "success",
  "data": {
    "id": "mongo-object-id",
    "seqNum": 42,
    "roomId": "meeting-uuid",
    "senderId": "user-uuid",
    "senderName": "Nguyen Van A",
    "content": "Hello everyone!",
    "type": "TEXT",
    "createdAt": "2025-01-15T10:30:00Z"
  }
}

400 Bad Request (fail)
{
  "status": "fail",
  "data": { "violations": [{ "field": "RateLimitExceeded", "message": "..." }] }
}
```

### 4.2 Get Messages (Cursor Pagination)

```
GET /api/chat/rooms/{roomId}/messages?limit=20&before=<cursor-token>
Authorization: Bearer <jwt>

200 OK
{
  "status": "success",
  "data": {
    "items": [...],
    "hasNext": true,
    "pageSize": 20
  }
}
```

### 4.3 Get Room Info

```
GET /api/chat/rooms/{roomId}
Authorization: Bearer <jwt>

200 OK
{
  "status": "success",
  "data": {
    "roomId": "meeting-uuid",
    "meetingId": "meeting-uuid",
    "status": "ACTIVE",
    "createdAt": "2025-01-15T10:00:00Z"
  }
}
```

## 5. Kafka Consumer Design

### 5.1 Event DTOs

Each event is wrapped in CloudEvents binary format. Consumers deserialize:

```java
// Kafka topic: "meeting-management.meeting.started"
public record MeetingStartedEventDto(
    UUID eventId, UUID aggregateId, UUID hostId, String liveKitRoomName, Instant startedAt) {}

// Kafka topic: "meeting-management.meeting.ended"
public record MeetingEndedEventDto(UUID eventId, UUID aggregateId, UUID hostId, Instant endedAt) {}

// Kafka topic: "meeting-management.participant.joined" (NEW — follows existing convention)
public record ParticipantJoinedEventDto(
    UUID eventId, UUID meetingId, UUID userId, String displayName, Instant occurredAt) {}

// Kafka topic: "meeting-management.participant.left" (NEW — follows existing convention)
public record ParticipantLeftEventDto(
    UUID eventId, UUID meetingId, @Nullable UUID userId, @Nullable String displayName,
    Instant occurredAt) {}

// Kafka topic: "meeting-management.participant.kicked" (existing — keep as-is)
public record ParticipantKickedEventDto(
    UUID eventId, UUID meetingId, UUID kickedBy, @Nullable UUID kickedUserId,
    @Nullable String kickedDisplayName, Instant occurredAt) {}
```

> **Topic naming convention:** Events use `{prefix}.{entity}.{action}` pattern:
>
> - `meeting-management.meeting.started`
> - `meeting-management.participant.joined`
> - `meeting-management.participant.kicked` New events (`participant_joined`,
>   `participant_left`) follow the same pattern as `ParticipantKickedEvent`.

### 5.2 Consumer Pattern

```java
@KafkaListener(
    topics = "meeting-management.meeting.started",
    groupId = "chat-management-meeting",    // fixed, not UUID.randomUUID()
    containerFactory = "cloudEventKafkaListenerContainerFactory")
public void onMeetingStarted(CloudEvent cloudEvent) {
    MeetingStartedEventDto event = deserialize(cloudEvent, MeetingStartedEventDto.class);
    if (event == null) return;
    openChatRoomUseCase.execute(event.aggregateId().toString());
}
```

> **Note:** Unlike meeting-management's SSE config which uses
> `UUID.randomUUID()` per instance, chat-management uses **fixed group IDs** so
> Kafka partitions events across instances. This is correct because
> chat-management is stateful (MongoDB) and needs each message processed exactly
> once.

````

## 6. LiveKit Integration

### 6.1 ChatLiveKitAdapter

```java
@Component
public class ChatLiveKitAdapter implements ChatLiveKitPort {

    private static final String CHAT_TOPIC = "chat";
    private final RoomServiceClient roomServiceClient;
    private final ObjectMapper objectMapper;  // for JSON serialization

    @Override
    public Result<Void, ChatError> broadcastMessage(String roomName, ChatMessage message) {
        String payload = buildDataPayload(message);
        if (payload.getBytes().length > 15_000) {
            return Result.failure(new ChatError.MessageTooLong(4000, payload.length()));
        }

        SendDataRequest request = SendDataRequest.newBuilder()
            .setRoom(roomName)
            .setData(ByteString.copyFromUtf8(payload))
            .setKind(DataPacket.Kind.RELIABLE)   // guaranteed delivery
            .setTopic(CHAT_TOPIC)               // filterable by client
            .build();

        try {
            roomServiceClient.sendData(request).execute();
            return Result.success();
        } catch (Exception e) {
            if (isKnownKnpeBug(e)) {
                return Result.success();  // message likely delivered despite KNPE
            }
            return Result.failure(new ChatError.PersistenceFailure(e.getMessage()));
        }
    }

    private String buildDataPayload(ChatMessage message) {
        // Use ObjectMapper for proper JSON escaping (handles ", \, \n, etc.)
        ChatPayload payload = new ChatPayload(
            message.getId(),
            message.getSeqNum(),
            message.getSenderId(),
            message.getSenderName(),
            message.getContent(),
            message.getType(),
            message.getCreatedAt() != null ? message.getCreatedAt().toString() : null
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize chat payload", e);
        }
    }

    private boolean isKnownKnpeBug(Exception e) {
        return e instanceof KotlinNullPointerException
            || e.getCause() instanceof KotlinNullPointerException
            || (e.getMessage() != null && e.getMessage().contains("null"));
    }

    // POJO for JSON serialization with proper escaping
    private record ChatPayload(
        String id,
        Long seqNum,
        String senderId,
        String senderName,
        String content,
        String type,
        String createdAt
    ) {}
}
````

### 6.2 Payload Spec

All LiveKit data messages use JSON with `topic = "chat"`:

```json
{
    "id": "mongo-object-id",
    "seqNum": 42,
    "senderId": "user-uuid-or-null",
    "senderName": "Nguyen Van A",
    "content": "Hello!",
    "type": "TEXT",
    "createdAt": "2025-01-15T10:30:00Z"
}
```

Client filters by `topic == "chat"`, then parses JSON payload.

## 7. Validation Rules

```java
// SendMessageUseCase validation chain:
1. JWT valid?          → JwtAuthFilter (401 if invalid)
2. Room exists?         → ChatError.RoomNotFound (404)
3. Room ACTIVE?         → ChatError.Unauthorized "Meeting has ended" (403)
4. Rate limit (10/min)? → ChatError.RateLimitExceeded (429, retryAfter=60s)
5. Content ≤ 4000 chars → ChatError.MessageTooLong (400)
6. Save to MongoDB      → ChatError.PersistenceFailure (500)
7. Broadcast via LiveKit → non-blocking: message saved even if broadcast fails
```

## 8. MongoDB Counter for SeqNum

```java
// SeqNum generation via MongoDB atomic findAndModify:
private long nextSeqNum(String roomId) {
    CounterDoc counter = mongoTemplate.findAndModify(
        new Query(Criteria.where("_id").is(roomId)),
        new Update().inc("seq", 1),
        FindAndModifyOptions.options().returnNew(true).upsert(true),
        CounterDoc.class
    );
    return counter.seq;
}

@Document(collection = "seq_counters")
class CounterDoc {
    @Id private String id;  // = roomId
    private long seq;
}
```

## 9. Security

```java
// JwtAuthFilter: extracts userId from JWT, sets SecurityContext
// JWT claims used: sub (userId), roles (optional)
@Bean
SecurityFilterChain filterChain(HttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/api/chat/**").authenticated()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

## 10. Infrastructure Dependencies

```kotlin
// chat-management/build.gradle.kts
dependencies {
    implementation(libs.shared)
    implementation(libs.spring.boot.starter.data.mongodb)   // MongoDB
    implementation(libs.spring.kafka)                       // Kafka consumer
    implementation(libs.cloudevents.kafka)                  // CloudEvent deserialization
    implementation(libs.jjwt.api)                          // JWT validation
    implementation(libs.livekit.server)                     // LiveKit SDK
    implementation(libs.spring.boot.starter.integration)   // SSE (future)
    implementation(libs.spring.boot.starter.validation)       // Bean validation
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    testImplementation(testFixtures(libs.shared))
    testImplementation(libs.spring.boot.data.mongodb.test)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.archunit.junit5)
}
```

## 11. Docker Compose

```yaml
services:
    mongodb:
        image: mongo:8.0
        ports:
            - '27017'
        environment:
            MONGO_INITDB_DATABASE: chat_management
        volumes:
            - ./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro

    kafka:
        image: confluentinc/cp-kafka:7.9.0
        ports:
            - '9092'
        environment:
            KAFKA_PROCESS_ROLES: broker,controller
            KAFKA_NODE_ID: 1
            KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9092
            KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
            KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
            KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
            KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
            KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
            KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
        depends_on:
            - mongodb
```

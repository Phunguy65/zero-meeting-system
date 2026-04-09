# Context

The `meeting-management` service currently uses two parallel notification
channels for join request events:

1. **Domain events via Outbox → Kafka** (`JoinRequestCreatedEvent`,
   `JoinRequestApprovedEvent`, `JoinRequestDeniedEvent`) — for external
   consumers and durability
2. **Redis Pub/Sub via `RedisSseEventPublisher`** — for low-latency SSE fan-out
   to connected host clients

Guests (join requestors) receive status updates via HTTP polling
(`GET /joinRequests/{requestId}`), relying on `PollJoinRequestStatusUseCase`
which regenerates a LiveKit token on every poll for approved requests.

SSE timeout is hardcoded in `MeetingSseManager` as `SSE_TIMEOUT_MS = 300_000L`
(5 minutes), not externally configurable.

**Files at the center of the change:**

- `infrastructure/sse/RedisSseEventPublisher.java` — publishes to Redis channel
  `meeting:{meetingId}:events`
- `infrastructure/sse/MeetingSseManager.java` — subscribes via
  `RedisMessageListenerContainer`, fans out to HOST SSE emitters (keyed by
  meetingId)
- `infrastructure/config/RedisConfig.java` — defines
  `RedisMessageListenerContainer` bean
- 5 use cases + 1 handler + 1 scheduled job that call
  `RedisSseEventPublisher.publish()`

## Goals / Non-Goals

**Goals:**

- Eliminate Redis Pub/Sub dependency for SSE notification path
- Kafka becomes the single event bus for all join request lifecycle events
- Guests receive status updates via SSE (not polling) — endpoint
  `GET /v1.0/joinRequests/{requestId}/events`
- SSE timeout configurable from Consul KV without service restart
- Multi-instance SSE fan-out preserved (all instances receive all events)

**Non-Goals:**

- Removing Redis entirely (still used for join request data storage: ZSET +
  HASH)
- Reducing Outbox polling interval (remains 1000ms)
- Changing join request data model in Redis
- Implementing WebSocket or long-polling alternatives

## Decisions

### Decision 1: Kafka consumer group strategy for SSE fan-out

**Decision:** `MeetingSseManager` uses a **unique consumer group ID per
instance** (`"meeting-sse-" + UUID.randomUUID()` at startup) with
`auto.offset.reset=latest`.

**Rationale:** Standard Kafka consumer groups partition messages across
instances — each message goes to exactly ONE instance. This breaks SSE fan-out
where every instance must receive every event (to push to locally connected
clients). Using a unique group ID per instance makes each instance an
independent consumer receiving all messages.

**Alternative considered:** Kafka `auto.offset.reset=earliest` with stable group
ID. Rejected — replaying historical events on restart would re-push stale
notifications to still-connected clients.

**Alternative considered:** Store SSE state in Redis and route events through
Redis. Rejected — this reintroduces the Redis Pub/Sub dependency we're
eliminating.

---

### Decision 2: LiveKit token delivery for approved join requests

**Decision:** Add `liveKitToken: String` field to `JoinRequestApprovedEvent`.
The token is generated in the use case, included in the Outbox JSON payload,
flows through Kafka, and is pushed via SSE to the guest.

**Rationale:** The guest SSE endpoint must deliver the LiveKit token on
approval. Alternative of re-generating the token in the Kafka consumer was
rejected (consumer layer would depend on `LiveKitPort` — wrong architecture
boundary). Alternative of storing in Redis temporarily was rejected (additional
key management, inconsistent cleanup).

**Trade-off accepted:** LiveKit token (JWT, 1800s TTL) is stored in Postgres
`outbox_event.payload` and in the Kafka
`meeting-management.join_request.approved` topic. Mitigated by:

- Kafka topic `retention.ms=1800000` (30 min = token TTL)
- Token expires and becomes invalid after TTL even if visible in logs

---

### Decision 3: Two emitter registries in MeetingSseManager

**Decision:** `MeetingSseManager` manages two separate emitter maps:

- `hostEmittersByMeeting: ConcurrentHashMap<UUID meetingId, CopyOnWriteArrayList<SseEmitter>>`
  — for hosts watching new join requests
- `guestEmittersByRequest: ConcurrentHashMap<UUID requestId, SseEmitter>` — for
  guests waiting on approval decision

**Rationale:** Host and guest have different subscription scopes (meeting-level
vs request-level) and different lifecycles (host SSE stays open for meeting
duration; guest SSE closes once request is resolved).

**Event routing:**

- `join_request.created` → HOST emitters (notify host of new request)
- `join_request.expired` → HOST emitters + GUEST emitter (cleanup both sides)
- `join_request.approved` → GUEST emitter only (host initiated the action, no
  notification needed)
- `join_request.denied` → GUEST emitter only (same reasoning)

---

### Decision 4: @Transactional on JoinRequestCleanupJob and MeetingEndedJoinRequestHandler

**Decision:** Add `@Transactional` to both
`JoinRequestCleanupJob.cleanupExpiredRequests()` and
`MeetingEndedJoinRequestHandler.handle()`.

**Rationale:** `OutboxEventListener` is annotated
`@TransactionalEventListener(phase=AFTER_COMMIT)` — it only fires when an active
transaction commits. Without `@Transactional`:

- `@Scheduled` jobs have no transaction context →
  `applicationEventPublisher.publishEvent()` won't trigger `OutboxEventListener`
- `@TransactionalEventListener` methods (phase=AFTER_COMMIT) run outside the
  original transaction → publishing new events from within them also won't
  trigger `OutboxEventListener`

Adding `@Transactional` creates the required transaction context in both cases.

---

### Decision 5: JoinRequestExpiredEvent creation

**Decision:** Create `JoinRequestExpiredEvent` implementing `PublishableEvent`
with topic `meeting-management.join_request.expired`.

**Rationale:** The domain event record exists as a class but is never
instantiated — `JoinRequestCleanupJob` currently only publishes to Redis SSE,
never to the Outbox. The Kafka consumer needs a real event on this topic to
notify guest SSE streams of expired requests. The record must implement
`PublishableEvent` (not just `DomainEvent`) so `OutboxEventListener` picks it up
for Kafka publishing.

---

### Decision 6: SseProperties for Consul-driven timeout

**Decision:** Create `SseProperties` as
`@Component @RefreshScope @ConfigurationProperties(prefix="app.sse")` with
fields `timeoutMs=300000L` and `joinRequestTimeoutMs=600000L`. Pattern matches
existing `LiveKitProperties` and `MeetingLimitsConfig`.

**Consul KV path:** `config/meeting-management/data` (existing YAML format
config path).

**Behavior on refresh:** Timeout changes apply to NEW SSE connections only.
Existing connections retain their original timeout (set at `SseEmitter`
construction time). This is documented behavior — closing active emitters on
refresh is not implemented as it would interrupt active host monitoring
sessions.

---

### Decision 7: CloudEvent deserialization in Kafka consumer

**Decision:** Use `io.cloudevents.kafka.CloudEventDeserializer` (confirmed
available in `cloudevents-kafka:4.0.1`) in the consumer factory. Extract and
deserialize the data payload manually per listener method using Spring-managed
`ObjectMapper`.

```java
// In each @KafkaListener:
byte[] data = cloudEvent.getData().toBytes();
JoinRequestApprovedEvent event = objectMapper.readValue(data, JoinRequestApprovedEvent.class);
```

**Rationale:** `PojoCloudEventDataMapper` requires a single target type per
consumer factory. Since `MeetingSseManager` handles 4 different event types
across 4 `@KafkaListener` methods, per-method manual deserialization is simpler
and avoids factory proliferation.

**Jackson record support:** Spring Boot 4.0.3 uses Jackson 3 which natively
supports Java record deserialization without annotations (project compiles with
`-parameters` flag by default via Spring Boot).

## Risks / Trade-offs

**[Risk: Outbox latency for SSE]** → Guest and host SSE notifications have ~1-2s
delay (Outbox poll interval = 1000ms). Acceptable trade-off for architectural
simplicity. Mitigation: if real-time becomes a requirement, the Outbox poll
interval can be reduced without other changes.

**[Risk: Token in Kafka topic]** → `liveKitToken` (JWT) stored in
`meeting-management.join_request.approved` topic. Mitigation: topic retention
set to 1800000ms (30 min = token TTL), so tokens expire before significant
exposure window. Production hardening: enable Kafka encryption at rest + ACLs.

**[Risk: Unique consumer group accumulation]** → Each service restart creates a
new consumer group in Kafka (group ID includes random UUID). Old groups
accumulate as inactive consumer groups in Kafka. Mitigation: Kafka consumer
groups with no active members are cleaned up by Kafka's
`offsets.retention.minutes` (default 7 days). Not a correctness issue.

**[Risk: @Transactional on @Scheduled job]** →
`JoinRequestCleanupJob.cleanupExpiredRequests()` iterates many Redis keys and
publishes multiple events within one transaction. Long transaction holds
database connection. Mitigation: Job runs every 60s; typical expired request
count is small. If volume grows, consider chunked transactions.

**[Risk: Guest SSE timeout before approval]** → If guest SSE connection times
out (10 min default) before host acts, guest must reconnect. Client must handle
reconnect and re-subscribe. Mitigation: document expected client behavior;
timeout configurable via Consul.

## Migration Plan

1. Deploy updated `meeting-management` service — old polling endpoint
   `GET /v1.0/joinRequests/{requestId}` is removed
2. Apply Kafka topic manifests (`kafka-topics.yaml`) — new topics auto-created
   by Strimzi
3. Update Consul KV at `config/meeting-management/data` with `app.sse.*` keys
   (optional — defaults are applied)
4. Update client apps to use `GET /v1.0/joinRequests/{requestId}/events` SSE
   instead of polling

**Rollback:** Redeploy previous image. Redis Pub/Sub infrastructure remains
intact during transition (Redis is still present). Previous polling endpoint is
restored.

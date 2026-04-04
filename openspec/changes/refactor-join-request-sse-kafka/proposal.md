# Why

The current join request notification system uses Redis Pub/Sub for SSE
broadcasting and client-side polling for status updates. This creates
architectural redundancy (domain events already flow through Kafka) and
suboptimal UX (polling introduces latency and wastes resources). SSE timeout is
hardcoded, preventing runtime adjustment. This refactor consolidates event
distribution through Kafka, replaces polling with SSE streams for guests, and
externalizes configuration to Consul.

## What Changes

- Remove Redis Pub/Sub for SSE event broadcasting — use Kafka as the single
  event bus
- Replace guest client polling (`GET /joinRequests/{requestId}`) with SSE stream
  (`GET /joinRequests/{requestId}/events`)
- Externalize SSE timeout configuration to Consul KV with `@RefreshScope`
  hot-reload
- Add `liveKitToken` field to `JoinRequestApprovedEvent` for token delivery via
  Kafka
- Refactor `MeetingSseManager` from Redis listener to Kafka consumer with unique
  consumer group per instance
- Add `@Transactional` to `JoinRequestCleanupJob` and
  `MeetingEndedJoinRequestHandler` for Outbox pattern compatibility
- Create `JoinRequestExpiredEvent` domain event (currently missing)
- Delete `RedisSseEventPublisher` and `PollJoinRequestStatusUseCase`

## Capabilities

### New Capabilities

- `join-request-sse-kafka`: Kafka-based SSE event distribution for join request
  lifecycle (created, approved, denied, expired) with multi-instance fan-out
  support

### Modified Capabilities

<!-- No existing specs to modify — this is a refactor of infrastructure, not requirements -->

## Impact

**Services:**

- `meeting-management`: 16 files modified (2 deleted, 2 created, 12 updated)

**Infrastructure:**

- Kafka: 4 new topics with 30-minute retention (matching token TTL)
- Redis: `RedisMessageListenerContainer` removed (Redis still used for join
  request storage)
- Consul: New config keys `app.sse.timeout-ms` and
  `app.sse.join-request-timeout-ms`

**APIs:**

- **BREAKING**: `GET /v1.0/joinRequests/{requestId}` endpoint removed (replaced
  by SSE)
- **NEW**: `GET /v1.0/joinRequests/{requestId}/events` SSE endpoint for guest
  status stream

**Dependencies:**

- No new dependencies (CloudEvents Kafka deserializer already available in
  `cloudevents-kafka:4.0.1`)

**Latency:**

- SSE notifications: ~1-2s delay (Outbox polling interval) — acceptable
  trade-off for architectural simplicity

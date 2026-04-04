# ADDED Requirements

## Requirement: Kafka consumer for join request events

The `meeting-management` service SHALL consume join request lifecycle events
from Kafka topics (`meeting-management.join_request.created`, `.approved`,
`.denied`, `.expired`) using a unique consumer group ID per instance to ensure
all instances receive all events for SSE fan-out.

### Scenario: Consumer group uniqueness per instance

- **WHEN** a `meeting-management` service instance starts
- **THEN** it SHALL create a Kafka consumer with group ID
  `"meeting-sse-" + UUID.randomUUID()` and `auto.offset.reset=latest`

### Scenario: All instances receive all events

- **WHEN** a join request event is published to Kafka
- **THEN** all running `meeting-management` instances SHALL receive the event
  (not partitioned across instances)

---

## Requirement: SSE endpoint for guest join request status

The system SHALL provide an SSE endpoint
`GET /v1.0/joinRequests/{requestId}/events` that streams join request status
updates to the guest (no authentication required).

### Scenario: Guest subscribes to join request events

- **WHEN** a guest connects to `GET /v1.0/joinRequests/{requestId}/events`
- **THEN** the system SHALL return an SSE stream with
  `Content-Type: text/event-stream`

### Scenario: Guest receives approval notification with token

- **WHEN** a host approves the join request
- **THEN** the guest SSE stream SHALL receive an event with
  `event: join_request_approved` and
  `data: {requestId, status: "APPROVED", liveKitToken: "..."}`

### Scenario: Guest receives denial notification

- **WHEN** a host denies the join request
- **THEN** the guest SSE stream SHALL receive an event with
  `event: join_request_denied` and `data: {requestId, status: "DENIED"}` and the
  stream SHALL close

### Scenario: Guest receives expiration notification

- **WHEN** the join request expires (TTL elapsed)
- **THEN** the guest SSE stream SHALL receive an event with
  `event: join_request_expired` and `data: {requestId, status: "EXPIRED"}` and
  the stream SHALL close

### Scenario: Guest SSE timeout

- **WHEN** the guest SSE connection exceeds the configured timeout without
  receiving a resolution event
- **THEN** the system SHALL close the SSE stream and remove the emitter from the
  registry

---

## Requirement: SSE timeout configuration from Consul

The system SHALL read SSE timeout values from Consul KV and hot-reload them
without service restart using `@RefreshScope`.

### Scenario: Default timeout values on startup

- **WHEN** the service starts and Consul is unavailable
- **THEN** the system SHALL use default timeout values
  (`app.sse.timeout-ms=300000`, `app.sse.join-request-timeout-ms=600000`)

### Scenario: Timeout values from Consul KV

- **WHEN** Consul KV contains `app.sse.timeout-ms` and
  `app.sse.join-request-timeout-ms` at path `config/meeting-management/data`
- **THEN** the system SHALL use those values for new SSE connections

### Scenario: Timeout refresh on Consul KV update

- **WHEN** Consul KV values are updated
- **THEN** the `SseProperties` bean SHALL be refreshed within the watch interval
  and new SSE connections SHALL use the updated timeout values

### Scenario: Existing connections retain original timeout

- **WHEN** Consul KV timeout values are updated
- **THEN** already-open SSE connections SHALL continue using their original
  timeout value (set at connection creation time)

---

## Requirement: LiveKit token in JoinRequestApprovedEvent

The `JoinRequestApprovedEvent` domain event SHALL include the LiveKit access
token as a string field for delivery via Kafka to the guest SSE stream.

### Scenario: Token included in approved event

- **WHEN** a host approves a join request
- **THEN** the `JoinRequestApprovedEvent` SHALL contain a `liveKitToken` field
  with the generated JWT token

### Scenario: Token flows through Outbox and Kafka

- **WHEN** `JoinRequestApprovedEvent` is published via
  `ApplicationEventPublisher`
- **THEN** the event SHALL be persisted to the `outbox_event` table with the
  token in the JSON payload and subsequently published to Kafka topic
  `meeting-management.join_request.approved`

---

## Requirement: Kafka topics for join request events

The system SHALL define four Kafka topics for join request lifecycle events with
30-minute retention matching LiveKit token TTL.

### Scenario: Topic retention matches token TTL

- **WHEN** Kafka topics are created
- **THEN** each topic SHALL have `retention.ms=1800000` (30 minutes) and
  `cleanup.policy=delete`

### Scenario: Topic names match domain event topics

- **WHEN** domain events are published
- **THEN** the Kafka topics SHALL be named
  `meeting-management.join_request.created`,
  `meeting-management.join_request.approved`,
  `meeting-management.join_request.denied`,
  `meeting-management.join_request.expired`

---

## Requirement: Transactional context for event publishing

The system SHALL ensure `@Transactional` context exists when publishing domain
events from scheduled jobs and transactional event listeners to trigger
`OutboxEventListener`.

### Scenario: JoinRequestCleanupJob publishes with transaction

- **WHEN** `JoinRequestCleanupJob.cleanupExpiredRequests()` publishes
  `JoinRequestExpiredEvent`
- **THEN** the method SHALL be annotated with `@Transactional` so
  `OutboxEventListener` captures the event

### Scenario: MeetingEndedJoinRequestHandler publishes with transaction

- **WHEN** `MeetingEndedJoinRequestHandler.handle()` publishes
  `JoinRequestDeniedEvent`
- **THEN** the method SHALL be annotated with `@Transactional` so
  `OutboxEventListener` captures the event

---

## Requirement: JoinRequestExpiredEvent domain event

The system SHALL define `JoinRequestExpiredEvent` as a `PublishableEvent` for
Kafka publishing when join requests expire.

### Scenario: Event implements PublishableEvent

- **WHEN** `JoinRequestExpiredEvent` is created
- **THEN** it SHALL implement `PublishableEvent` interface with topic
  `meeting-management.join_request.expired`

### Scenario: Event published on expiration

- **WHEN** `JoinRequestCleanupJob` detects an expired join request
- **THEN** it SHALL publish `JoinRequestExpiredEvent` via
  `ApplicationEventPublisher`

---

# REMOVED Requirements

## Requirement: Polling endpoint for join request status

**Reason**: Replaced by SSE stream endpoint
`GET /v1.0/joinRequests/{requestId}/events` for real-time status updates without
polling overhead.

**Migration**: Clients MUST switch from polling
`GET /v1.0/joinRequests/{requestId}` to subscribing to SSE stream
`GET /v1.0/joinRequests/{requestId}/events`. The SSE stream will push status
updates (approved, denied, expired) and close automatically once the request is
resolved.

---

## Requirement: Redis Pub/Sub for SSE event broadcasting

**Reason**: Architectural redundancy eliminated — domain events already flow
through Kafka. Kafka becomes the single event bus for all join request lifecycle
events.

**Migration**: No client-facing migration. Internal refactor only.
`RedisSseEventPublisher` removed; `MeetingSseManager` refactored to consume from
Kafka instead of Redis Pub/Sub. Redis remains in use for join request data
storage (ZSET + HASH).

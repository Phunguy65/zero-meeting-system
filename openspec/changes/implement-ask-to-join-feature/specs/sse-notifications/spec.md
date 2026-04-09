# ADDED Requirements

## Requirement: Host receives real-time notifications for join requests via SSE

The meeting host SHALL be able to establish a Server-Sent Events (SSE)
connection to receive real-time notifications when participants request to join
their meeting.

### Scenario: Host connects to SSE endpoint

- **WHEN** the authenticated host calls `GET /v1.0/meetings/{id}/events` with
  `X-User-ID` header
- **THEN** the system SHALL establish an SSE connection and return HTTP 200 with
  `Content-Type: text/event-stream`

### Scenario: Host receives join request notification

- **WHEN** a participant submits a join request for the host's meeting
- **THEN** the system SHALL send an SSE event with `event: join_request_created`
  and data containing `{ requestId, displayName, userId, requestedAt }`

### Scenario: Host receives approval notification

- **WHEN** the host approves a join request
- **THEN** the system SHALL send an SSE event with
  `event: join_request_approved` and data containing `{ requestId }`

### Scenario: Host receives denial notification

- **WHEN** the host denies a join request
- **THEN** the system SHALL send an SSE event with `event: join_request_denied`
  and data containing `{ requestId }`

### Scenario: Non-host tries to connect to SSE endpoint

- **WHEN** an authenticated user who is not the meeting host calls
  `GET /v1.0/meetings/{id}/events`
- **THEN** the system SHALL return HTTP 403 with error `NOT_AUTHORIZED`

### Scenario: SSE connection timeout

- **WHEN** an SSE connection has been idle for 5 minutes without any events
- **THEN** the system SHALL close the connection and the client SHALL reconnect

---

## Requirement: SSE events are distributed across multiple backend instances via Redis Pub/Sub

When a join request event occurs, the system SHALL publish the event to a Redis
Pub/Sub channel so that all backend instances can relay it to their connected
SSE clients.

### Scenario: Event published to Redis Pub/Sub

- **WHEN** a join request is created on backend instance A
- **THEN** the system SHALL publish a message to Redis channel
  `meeting:{meetingId}:events`

### Scenario: Event received by instance holding SSE connection

- **WHEN** backend instance B receives a message from Redis channel
  `meeting:{meetingId}:events` and holds an SSE connection for that meeting
- **THEN** instance B SHALL relay the event to the connected SSE client

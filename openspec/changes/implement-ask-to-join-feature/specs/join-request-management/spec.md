# ADDED Requirements

## Requirement: Participant can request to join a meeting

When a meeting's admission policy requires manual approval, a participant
(authenticated user or guest) SHALL be able to submit a join request instead of
joining immediately. The system SHALL return a pending join request ID that the
participant can use to poll for status updates.

### Scenario: Authenticated participant requests to join with MANUAL_APPROVAL policy

- **WHEN** an authenticated user calls `POST /v1.0/meetings/{id}:requestJoin`
  for a LIVE meeting with `admissionPolicy = MANUAL_APPROVAL`
- **THEN** the system SHALL create a join request with status `PENDING`, return
  HTTP 202 with `{ requestId, status: "PENDING" }`, and notify the host via SSE

### Scenario: Guest requests to join with MANUAL_APPROVAL policy

- **WHEN** a guest (unauthenticated user) calls
  `POST /v1.0/meetings/{id}:requestJoin` for a LIVE meeting with
  `admissionPolicy = MANUAL_APPROVAL` and the meeting allows guests
- **THEN** the system SHALL create a join request identified by `deviceId`,
  return HTTP 202 with `{ requestId, status: "PENDING" }`, and notify the host
  via SSE

### Scenario: Participant requests to join with ALLOW_ALL policy

- **WHEN** a user calls `POST /v1.0/meetings/{id}:requestJoin` for a LIVE
  meeting with `admissionPolicy = ALLOW_ALL`
- **THEN** the system SHALL immediately generate a LiveKit token and return HTTP
  200 with `{ status: "APPROVED", token, roomName }`

### Scenario: Duplicate join request from same participant

- **WHEN** a participant submits a join request for a meeting where they already
  have a PENDING request
- **THEN** the system SHALL return the existing `requestId` with status
  `PENDING` (idempotent, HTTP 200)

### Scenario: Join request for non-live meeting

- **WHEN** a participant submits a join request for a meeting that is not LIVE
- **THEN** the system SHALL return HTTP 400 with error
  `INVALID_STATUS_TRANSITION`

### Scenario: Guest join request denied when guests are not allowed

- **WHEN** an unauthenticated user submits a join request for a meeting with
  `allowGuest = false`
- **THEN** the system SHALL return HTTP 403 with error `GUEST_NOT_ALLOWED`

---

## Requirement: Join request has configurable TTL and expires automatically

Each join request SHALL have a configurable expiry duration (default 5 minutes).
The system SHALL automatically mark expired requests as `EXPIRED` and notify
affected participants via the poll endpoint.

### Scenario: Join request expires after TTL

- **WHEN** a join request has been in `PENDING` status for longer than the
  meeting's `joinRequestTimeoutSeconds`
- **THEN** the system SHALL mark the request as `EXPIRED` and a subsequent poll
  SHALL return status `EXPIRED`

### Scenario: Meeting uses custom TTL

- **WHEN** a meeting is created with `joinRequestTimeoutSeconds = 120`
- **THEN** join requests for that meeting SHALL expire after 120 seconds

---

## Requirement: Participant can poll join request status

A participant with a pending join request SHALL be able to poll its status. When
approved, the poll response SHALL include the LiveKit token needed to connect.

### Scenario: Participant polls a pending request

- **WHEN** a participant calls `GET /v1.0/joinRequests/{requestId}` and the
  request is still `PENDING`
- **THEN** the system SHALL return HTTP 200 with
  `{ status: "PENDING", requestId, expiresAt }`

### Scenario: Participant polls an approved request

- **WHEN** a participant calls `GET /v1.0/joinRequests/{requestId}` and the
  request has been `APPROVED`
- **THEN** the system SHALL return HTTP 200 with
  `{ status: "APPROVED", token, roomName }`

### Scenario: Participant polls a denied request

- **WHEN** a participant calls `GET /v1.0/joinRequests/{requestId}` and the
  request has been `DENIED`
- **THEN** the system SHALL return HTTP 200 with `{ status: "DENIED" }`

### Scenario: Participant polls an expired request

- **WHEN** a participant calls `GET /v1.0/joinRequests/{requestId}` and the
  request has `EXPIRED`
- **THEN** the system SHALL return HTTP 200 with `{ status: "EXPIRED" }`

### Scenario: Participant polls a non-existent request ID

- **WHEN** a participant calls `GET /v1.0/joinRequests/{requestId}` with an
  unknown `requestId`
- **THEN** the system SHALL return HTTP 404 with error `JOIN_REQUEST_NOT_FOUND`

---

## Requirement: Host can view pending join requests

The host of a LIVE meeting SHALL be able to retrieve the list of all pending
join requests for their meeting.

### Scenario: Host lists pending requests

- **WHEN** the authenticated host calls `GET /v1.0/meetings/{id}/joinRequests`
- **THEN** the system SHALL return HTTP 200 with an ordered list of
  `{ requestId, displayName, userId, requestedAt, expiresAt }` for all `PENDING`
  requests, ordered by `requestedAt` ascending

### Scenario: Non-host tries to list join requests

- **WHEN** an authenticated user who is not the meeting host calls
  `GET /v1.0/meetings/{id}/joinRequests`
- **THEN** the system SHALL return HTTP 403 with error `NOT_AUTHORIZED`

### Scenario: No pending requests

- **WHEN** the host calls `GET /v1.0/meetings/{id}/joinRequests` and there are
  no pending requests
- **THEN** the system SHALL return HTTP 200 with an empty list

---

## Requirement: Host can approve a single join request

The host SHALL be able to approve a single pending join request. Upon approval,
the system SHALL generate a LiveKit token that the participant can retrieve via
polling.

### Scenario: Host approves a pending request

- **WHEN** the host calls
  `POST /v1.0/meetings/{id}/joinRequests/{requestId}:approve`
- **THEN** the system SHALL update the request status to `APPROVED`, generate a
  LiveKit token, publish a `JoinRequestApprovedEvent` to Redis Pub/Sub, and
  return HTTP 200

### Scenario: Host approves an already-approved request (idempotent)

- **WHEN** the host calls `:approve` on a request that is already `APPROVED`
- **THEN** the system SHALL return HTTP 200 (no-op, re-generate token)

### Scenario: Host approves a denied request

- **WHEN** the host calls `:approve` on a request that is already `DENIED`
- **THEN** the system SHALL return HTTP 409 with error
  `INVALID_JOIN_REQUEST_TRANSITION`

### Scenario: Non-host tries to approve

- **WHEN** an authenticated user who is not the host calls `:approve`
- **THEN** the system SHALL return HTTP 403 with error `NOT_AUTHORIZED`

---

## Requirement: Host can deny a single join request

The host SHALL be able to deny a single pending join request. The participant's
next poll will return status `DENIED`.

### Scenario: Host denies a pending request

- **WHEN** the host calls
  `POST /v1.0/meetings/{id}/joinRequests/{requestId}:deny`
- **THEN** the system SHALL update the request status to `DENIED`, publish a
  `JoinRequestDeniedEvent` to Redis Pub/Sub, and return HTTP 200

### Scenario: Host denies an already-denied request (idempotent)

- **WHEN** the host calls `:deny` on a request that is already `DENIED`
- **THEN** the system SHALL return HTTP 200 (no-op)

### Scenario: Host denies an approved request

- **WHEN** the host calls `:deny` on a request that is already `APPROVED`
- **THEN** the system SHALL return HTTP 409 with error
  `INVALID_JOIN_REQUEST_TRANSITION`

---

## Requirement: Host can approve all pending join requests at once

The host SHALL be able to approve all pending join requests for a meeting in a
single call. This follows the Google API custom method style (`:approveAll`).

### Scenario: Host approves all pending requests

- **WHEN** the host calls `POST /v1.0/meetings/{id}/joinRequests:approveAll`
- **THEN** the system SHALL approve all `PENDING` requests, generate LiveKit
  tokens for each, and return HTTP 200 with `{ approvedCount: N }`

### Scenario: Approve all when no pending requests

- **WHEN** the host calls `:approveAll` and there are no pending requests
- **THEN** the system SHALL return HTTP 200 with `{ approvedCount: 0 }`

---

## Requirement: All pending join requests are auto-denied when a meeting ends

When a meeting transitions to `ENDED` state, the system SHALL automatically deny
all remaining pending join requests for that meeting.

### Scenario: Meeting ends with pending requests

- **WHEN** a meeting transitions to `ENDED` and there are `PENDING` join
  requests
- **THEN** the system SHALL mark all pending requests as `DENIED`, publish
  `JoinRequestDeniedEvent` for each, and clear the request queue

### Scenario: Meeting ends with no pending requests

- **WHEN** a meeting transitions to `ENDED` and there are no pending requests
- **THEN** no action is taken on join requests

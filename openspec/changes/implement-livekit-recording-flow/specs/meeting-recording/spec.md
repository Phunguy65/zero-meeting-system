# ADDED Requirements

## Requirement: Meeting recordings SHALL start through LiveKit Egress

The system SHALL create a real LiveKit room-composite egress session when an
authorized host starts recording a live meeting. The recording request SHALL
create or update a recording aggregate that tracks the LiveKit room name, the
resulting egress identifier, and the initial lifecycle state needed for
webhook-driven confirmation.

### Scenario: Host starts recording a live meeting

- **WHEN** the meeting host requests recording for a meeting in `LIVE` status
  and no other recording is active
- **THEN** the system SHALL create a recording flow for that meeting and call
  LiveKit Egress to start a room-composite recording

### Scenario: Start recording is rejected for non-host callers

- **WHEN** a caller other than the meeting host requests recording
- **THEN** the system SHALL reject the request as unauthorized and SHALL NOT
  create or start an egress session

### Scenario: Start recording is rejected when another recording is already active

- **WHEN** the meeting already has a recording in `PENDING` or `RECORDING`
- **THEN** the system SHALL reject the new request and SHALL NOT start another
  egress session

## Requirement: Recording lifecycle SHALL be driven by LiveKit egress webhooks

The system SHALL treat LiveKit webhook events as the authoritative source for
recording lifecycle transitions. A recording SHALL move from `PENDING` to
`RECORDING` on `egress_started`, and from `RECORDING` to either `COMPLETED` or
`FAILED` on `egress_ended` depending on the webhook payload.

### Scenario: Recording becomes active after egress start

- **WHEN** the system receives a valid `egress_started` webhook for a known
  recording session
- **THEN** the system SHALL transition that recording to `RECORDING` and persist
  the LiveKit egress identifier

### Scenario: Recording completes after egress end

- **WHEN** the system receives a valid `egress_ended` webhook with completed
  file metadata for a known recording session
- **THEN** the system SHALL mark the recording as `COMPLETED` and persist the
  returned file URL, storage path, duration, and file size

### Scenario: Recording fails after egress end reports an error

- **WHEN** the system receives a valid `egress_ended` webhook that reports an
  error or lacks usable file output
- **THEN** the system SHALL mark the recording as `FAILED` and persist an error
  message for later diagnosis

### Scenario: Duplicate or late webhook events are received

- **WHEN** the system receives a duplicate webhook or a webhook for a recording
  that is already in a terminal state
- **THEN** the system SHALL handle it idempotently without creating duplicate
  recordings or corrupting lifecycle state

## Requirement: Stopping a recording SHALL NOT end the meeting

The system SHALL treat recording termination and meeting termination as separate
actions. Stopping a recording SHALL stop the active LiveKit egress session only,
while leaving the meeting room active.

### Scenario: Host stops recording during a live meeting

- **WHEN** the meeting host requests to stop an active recording
- **THEN** the system SHALL stop the associated LiveKit egress session and SHALL
  keep the meeting room active

### Scenario: Stop recording is rejected when no recording is active

- **WHEN** the host requests to stop recording for a meeting that has no
  `PENDING` or `RECORDING` session
- **THEN** the system SHALL reject the request and SHALL NOT delete the meeting
  room

## Requirement: Ending a meeting SHALL stop active recording before room deletion

If a meeting is ended while a recording session is active, the system SHALL stop
the active egress session before deleting the LiveKit room so the recording can
finish cleanly.

### Scenario: Host ends a meeting while recording is active

- **WHEN** the meeting host ends a meeting that has an active recording
- **THEN** the system SHALL request egress stop before deleting the LiveKit room
  and SHALL rely on the later `egress_ended` webhook to finalize recording
  metadata

### Scenario: Host ends a meeting with no active recording

- **WHEN** the meeting host ends a meeting without an active recording
- **THEN** the system SHALL delete the LiveKit room without issuing an egress
  stop request

## Requirement: Recording webhooks SHALL be authenticated and routed through Kong

The system SHALL expose a webhook endpoint for LiveKit through Kong and SHALL
verify webhook signatures in `meeting-management` using the shared LiveKit API
secret before processing event payloads.

### Scenario: Authenticated webhook is accepted

- **WHEN** LiveKit sends a webhook request with a valid signature to the
  configured recording webhook route
- **THEN** the system SHALL accept and process the event

### Scenario: Unauthenticated webhook is rejected

- **WHEN** a webhook request arrives with a missing or invalid LiveKit signature
- **THEN** the system SHALL reject the request and SHALL NOT process the payload

## Requirement: Recording output SHALL be stored under a meeting-scoped RustFS path

The system SHALL direct LiveKit Egress to store meeting recordings in the
RustFS-backed `recordings` bucket using a `meetings/{meetingId}/` object prefix,
and SHALL persist the final object path returned by LiveKit.

### Scenario: Recording output path is scoped to a meeting

- **WHEN** the system starts a recording for a meeting
- **THEN** the generated egress output configuration SHALL target the
  `recordings` bucket with an object path prefix under `meetings/{meetingId}/`

### Scenario: Final storage path comes from webhook metadata

- **WHEN** LiveKit reports completed file output for a recording
- **THEN** the system SHALL persist the final storage path exactly as returned
  by the webhook payload rather than reconstructing it locally

## Requirement: Stale pending recordings SHALL be failed automatically

The system SHALL automatically fail recordings that remain in `PENDING` for
longer than the configured cleanup threshold so that stale sessions do not block
new recording attempts.

### Scenario: Pending recording exceeds cleanup threshold

- **WHEN** a recording remains in `PENDING` for more than 7 minutes
- **THEN** the system SHALL mark it as `FAILED` with a timeout-related error
  message

### Scenario: Recent pending recording is preserved

- **WHEN** a recording is still within the cleanup threshold
- **THEN** the cleanup job SHALL leave it unchanged

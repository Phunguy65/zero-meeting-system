# ADDED Requirements

## Requirement: Host can kick active participants from a live meeting

The system SHALL allow a meeting host to kick an active participant from a
meeting only when the meeting is currently live.

### Scenario: Host kicks a registered participant

- **WHEN** the host requests to kick a registered participant in a live meeting
  by `userId`
- **THEN** the system SHALL remove every active session for that user from the
  LiveKit room for that meeting

### Scenario: Host kicks a guest participant

- **WHEN** the host requests to kick a guest participant in a live meeting by
  `displayName`
- **THEN** the system SHALL remove every active guest session in that meeting
  matching that display name from the LiveKit room

### Scenario: Meeting is not live

- **WHEN** the host requests to kick a participant from a meeting that is not
  `LIVE`
- **THEN** the system SHALL reject the request with a domain error indicating
  the meeting is not in a kickable state

## Requirement: Kick moderation is host-only

The system SHALL only allow the host of the meeting to kick participants.

### Scenario: Non-host attempts to kick a participant

- **WHEN** a requester who is not the meeting host calls the kick API
- **THEN** the system SHALL reject the request as unauthorized

### Scenario: Host attempts to kick themselves

- **WHEN** the host targets their own active participant identity through the
  kick API
- **THEN** the system SHALL reject the request with a validation error
  indicating the host cannot kick themselves

## Requirement: Kick targets must be currently active in the room

The system SHALL only kick participants who currently have at least one active
participation log in the target meeting.

### Scenario: Registered user is not active in the room

- **WHEN** the host requests to kick a registered user whose active
  participation logs do not exist for that meeting
- **THEN** the system SHALL return a not-found style error indicating the user
  is not currently in the meeting

### Scenario: Guest display name is not active in the room

- **WHEN** the host requests to kick a guest display name with no active guest
  sessions in that meeting
- **THEN** the system SHALL return a not-found style error indicating the target
  is not currently in the meeting

## Requirement: Kick reuses the existing leave lifecycle

The system SHALL treat a kick as a forced disconnect through LiveKit and SHALL
continue to record participant departure through the existing `participant_left`
webhook flow.

### Scenario: Forced disconnect updates participation state through webhook

- **WHEN** the kick operation succeeds in removing a participant from LiveKit
- **THEN** the participant's active participation logs SHALL be closed by the
  existing webhook-driven leave handling rather than by direct mutation in the
  kick use case

### Scenario: One session is already gone during a multi-device kick

- **WHEN** one targeted session has already disappeared from LiveKit while other
  active sessions remain
- **THEN** the system SHALL continue attempting to remove the remaining active
  sessions instead of failing the whole kick immediately

## Requirement: Kick emits host-facing moderation updates

The system SHALL publish a moderation event after a successful kick so
host-facing realtime consumers can refresh participant views.

### Scenario: Host SSE subscribers receive participant-kicked notification

- **WHEN** a participant is kicked successfully
- **THEN** the system SHALL emit a `participant_kicked` host-facing realtime
  event for the meeting containing enough target information for the host UI to
  refresh its participant list

## Requirement: Kicked participants may request to join again later

The system SHALL not add any temporary or permanent rejoin block as part of the
kick action.

### Scenario: Kicked participant requests to join again

- **WHEN** a participant who was previously kicked starts a new join flow later
- **THEN** the system SHALL evaluate that join request using the normal meeting
  admission rules without any extra kick-specific block

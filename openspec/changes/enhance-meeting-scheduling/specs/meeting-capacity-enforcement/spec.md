# ADDED Requirements

## Requirement: Participant capacity enforced at join time

The system SHALL prevent more participants from joining a meeting than the
`settings.maxParticipants` value allows.

### Scenario: Join within capacity

- **WHEN** a user attempts to join a LIVE meeting where active participant count
  < `settings.maxParticipants`
- **THEN** the join succeeds and a `participation_logs` record is created

### Scenario: Join at capacity

- **WHEN** a user attempts to join a LIVE meeting where active participant
  count >= `settings.maxParticipants`
- **THEN** the system returns HTTP 409 with error code `MEETING_FULL` and the
  configured limit

### Scenario: Concurrent join at capacity boundary

- **WHEN** two users simultaneously attempt to join a meeting where active count
  = `maxParticipants - 1`
- **THEN** exactly one join succeeds and the other receives HTTP 409
  `MEETING_FULL`

### Scenario: Host always allowed to join

- **WHEN** the meeting host attempts to join a meeting that is at capacity
- **THEN** the join succeeds regardless of `maxParticipants`

---

## Requirement: Capacity check uses pessimistic lock

The capacity check SHALL use a `SELECT FOR UPDATE` lock on the `meetings` row to
prevent race conditions.

### Scenario: Lock acquired before count

- **WHEN** `JoinMeetingUseCase` executes
- **THEN** the `meetings` row is locked with `PESSIMISTIC_WRITE` before counting
  active `participation_logs`
- **THEN** the lock is released when the transaction commits or rolls back

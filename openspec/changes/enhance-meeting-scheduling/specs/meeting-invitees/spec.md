# ADDED Requirements

## Requirement: Invite users at scheduling time

When creating a scheduled meeting, the host SHALL be able to provide a list of
invitees identified by `email` or `username`. Only users that exist in the
system are accepted.

### Scenario: Invite existing users by email

- **WHEN** a host submits `POST /v1/meetings` with
  `invitees: [{ "email": "alice@example.com" }]`
- **THEN** the meeting is created and an `meeting_invitees` record is persisted
  with `status = PENDING`, `user_id` populated, and `display_name` from the
  resolved user's `fullName`

### Scenario: Invite existing users by username

- **WHEN** a host submits `POST /v1/meetings` with
  `invitees: [{ "username": "bob123" }]`
- **THEN** the meeting is created and an `meeting_invitees` record is persisted
  with `status = PENDING` and `user_id` populated

### Scenario: Invite non-existent user

- **WHEN** a host submits `POST /v1/meetings` with an email or username that
  does not match any active user
- **THEN** the system returns HTTP 422 with error code `INVITEE_NOT_FOUND` and
  the unresolved identifier
- **THEN** the meeting is NOT created

### Scenario: Duplicate invitee in request

- **WHEN** a host submits `POST /v1/meetings` with the same email appearing
  twice in `invitees`
- **THEN** the system deduplicates and creates only one `meeting_invitees`
  record for that email

### Scenario: Schedule without invitees

- **WHEN** a host submits `POST /v1/meetings` with no `invitees` field or an
  empty list
- **THEN** the meeting is created with no invitee records

### Scenario: Invitees list exceeds maximum

- **WHEN** a host submits `POST /v1/meetings` with more than 100 entries in
  `invitees`
- **THEN** the system returns HTTP 400 with a validation error

---

## Requirement: Invitees event published

After a meeting is created with at least one invitee, the system SHALL publish a
`MeetingInvitationsSentEvent` via the transactional outbox.

### Scenario: Event published with invitee details

- **WHEN** a meeting is successfully scheduled with invitees
- **THEN** a `MeetingInvitationsSentEvent` is written to the `outbox_event`
  table within the same transaction, containing `meetingId`, `meetingTitle`,
  `startTime`, and a list of `(userId, email, displayName)` tuples

### Scenario: No event when no invitees

- **WHEN** a meeting is scheduled with an empty invitees list
- **THEN** no `MeetingInvitationsSentEvent` is published

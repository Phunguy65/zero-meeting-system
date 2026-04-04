# MODIFIED Requirements

## Requirement: Meeting join behavior is controlled by admission policy

The meeting join flow SHALL check the meeting's `admissionPolicy` before
granting access. For `ALLOW_ALL`, the system SHALL generate a LiveKit token
immediately. For `MANUAL_APPROVAL`, the system SHALL create a join request and
return a pending status instead of a token.

### Scenario: Join a meeting with ALLOW_ALL policy

- **WHEN** a participant calls `POST /v1.0/meetings/{id}:requestJoin` on a LIVE
  meeting with `admissionPolicy = ALLOW_ALL`
- **THEN** the system SHALL return HTTP 200 with a LiveKit token and room name

### Scenario: Join a meeting with MANUAL_APPROVAL policy

- **WHEN** a participant calls `POST /v1.0/meetings/{id}:requestJoin` on a LIVE
  meeting with `admissionPolicy = MANUAL_APPROVAL`
- **THEN** the system SHALL return HTTP 202 with a `requestId` and
  `status: PENDING` (no token)

### Scenario: Host joins their own meeting (bypasses admission policy)

- **WHEN** the meeting host calls `POST /v1.0/meetings/{id}:requestJoin`
- **THEN** the system SHALL always generate a LiveKit token immediately,
  regardless of `admissionPolicy`

### Scenario: Password check still applies before admission policy

- **WHEN** a meeting has both `isPasswordProtected = true` and
  `admissionPolicy = MANUAL_APPROVAL`
- **THEN** the system SHALL first validate the password; if invalid, return HTTP
  401 (`INVALID_PASSWORD`); if valid, proceed to create a join request

### Scenario: Guest join request denied by admission policy and guest not allowed

- **WHEN** an unauthenticated user attempts to join a meeting with
  `allowGuest = false`
- **THEN** the system SHALL return HTTP 403 with error `GUEST_NOT_ALLOWED`
  regardless of admission policy

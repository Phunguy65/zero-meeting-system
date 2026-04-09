# ADDED Requirements

## Requirement: Meeting has an admission policy that controls join behavior

A meeting SHALL have an `admissionPolicy` field that determines whether
participants can join immediately or must be approved by the host. The policy
SHALL be an enum with values `ALLOW_ALL` and `MANUAL_APPROVAL`.

### Scenario: Meeting with ALLOW_ALL policy

- **WHEN** a meeting is created with `admissionPolicy = ALLOW_ALL`
- **THEN** any participant SHALL be able to join immediately without host
  approval

### Scenario: Meeting with MANUAL_APPROVAL policy

- **WHEN** a meeting is created with `admissionPolicy = MANUAL_APPROVAL`
- **THEN** participants SHALL create a join request that requires host approval
  before joining

### Scenario: Invalid admission policy value

- **WHEN** a meeting is created with an invalid `admissionPolicy` value (not
  `ALLOW_ALL` or `MANUAL_APPROVAL`)
- **THEN** the system SHALL return HTTP 400 with error `INVALID_SETTINGS`

---

## Requirement: Instant meetings default to MANUAL_APPROVAL

When creating an instant meeting without specifying an `admissionPolicy`, the
system SHALL default to `MANUAL_APPROVAL` for security.

### Scenario: Create instant meeting without specifying admission policy

- **WHEN** a user calls `POST /v1.0/meetings:instant` without an
  `admissionPolicy` field
- **THEN** the system SHALL create the meeting with
  `admissionPolicy = MANUAL_APPROVAL`

---

## Requirement: Scheduled meetings accept admission policy from request

When creating a scheduled meeting, the system SHALL accept the `admissionPolicy`
value from the API request.

### Scenario: Create scheduled meeting with ALLOW_ALL policy

- **WHEN** a user calls `POST /v1.0/meetings:schedule` with
  `admissionPolicy = ALLOW_ALL`
- **THEN** the system SHALL create the meeting with
  `admissionPolicy = ALLOW_ALL`

### Scenario: Create scheduled meeting with MANUAL_APPROVAL policy

- **WHEN** a user calls `POST /v1.0/meetings:schedule` with
  `admissionPolicy = MANUAL_APPROVAL`
- **THEN** the system SHALL create the meeting with
  `admissionPolicy = MANUAL_APPROVAL`

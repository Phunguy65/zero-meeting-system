# MODIFIED Requirements

## Requirement: Meeting settings include admission policy

The meeting settings SHALL include an `admissionPolicy` field (enum:
`ALLOW_ALL`, `MANUAL_APPROVAL`) replacing the previous `waitingRoom` boolean
field. The settings SHALL also include a `joinRequestTimeoutSeconds` field
(nullable integer) controlling how long a join request remains pending before
expiring.

### Scenario: Create meeting with admission policy

- **WHEN** a user creates a meeting and provides `admissionPolicy` in the
  settings
- **THEN** the system SHALL persist the `admissionPolicy` value and return it in
  the meeting settings response

### Scenario: Create meeting with custom join request timeout

- **WHEN** a user creates a meeting with `joinRequestTimeoutSeconds = 120`
- **THEN** the system SHALL persist the timeout value and apply it to all join
  requests for that meeting

### Scenario: Create meeting without join request timeout (uses default)

- **WHEN** a user creates a meeting without specifying
  `joinRequestTimeoutSeconds`
- **THEN** the system SHALL apply a default timeout of 300 seconds (5 minutes)
  to join requests

### Scenario: Meeting settings response includes admission policy

- **WHEN** a user retrieves a meeting
- **THEN** the settings object in the response SHALL include `admissionPolicy`
  (string) and `joinRequestTimeoutSeconds` (integer) fields

### Scenario: Invalid joinRequestTimeoutSeconds value

- **WHEN** a user creates a meeting with `joinRequestTimeoutSeconds` less than
  30 or greater than 600
- **THEN** the system SHALL return HTTP 400 with error `INVALID_SETTINGS`

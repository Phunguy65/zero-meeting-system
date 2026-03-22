# ADDED Requirements

## Requirement: Meeting description

A scheduled meeting SHALL support an optional free-text `description` field (max
2000 characters) for agenda, notes, or links.

### Scenario: Schedule meeting with description

- **WHEN** a host submits `POST /v1/meetings` with a non-empty `description`
- **THEN** the meeting is created and `description` is persisted and returned in
  the response

### Scenario: Schedule meeting without description

- **WHEN** a host submits `POST /v1/meetings` with no `description` field
- **THEN** the meeting is created with `description` as `null`

### Scenario: Description exceeds max length

- **WHEN** a host submits `POST /v1/meetings` with `description` longer than
  2000 characters
- **THEN** the system returns HTTP 400 with a validation error

---

## Requirement: Extended meeting settings

`MeetingSettings` SHALL support the following fields in addition to existing
ones:

| Field              | Type                                    | Default | Description                         |
| ------------------ | --------------------------------------- | ------- | ----------------------------------- |
| `maxParticipants`  | int (2–1000)                            | 100     | Per-meeting participant cap         |
| `recordingEnabled` | boolean                                 | false   | Allow recording                     |
| `requireApproval`  | boolean                                 | false   | Host must approve each join request |
| `screenShareMode`  | string (`ALL`\|`HOST_ONLY`\|`DISABLED`) | `ALL`   | Who can share screen                |
| `chatEnabled`      | boolean                                 | true    | Enable in-meeting chat              |

### Scenario: Schedule with custom settings

- **WHEN** a host submits `POST /v1/meetings` with
  `settings.maxParticipants = 50` and `settings.screenShareMode = "HOST_ONLY"`
- **THEN** the meeting is created with those settings persisted in the JSONB
  column

### Scenario: Schedule with default settings

- **WHEN** a host submits `POST /v1/meetings` with an empty `settings` object
- **THEN** the meeting is created with all settings at their default values

### Scenario: maxParticipants below minimum

- **WHEN** a host submits `POST /v1/meetings` with
  `settings.maxParticipants = 1`
- **THEN** the system returns HTTP 400 with a validation error

### Scenario: maxParticipants exceeds system ceiling

- **WHEN** a host submits `POST /v1/meetings` with `settings.maxParticipants`
  greater than the Consul-configured `max-participants-ceiling`
- **THEN** the system returns HTTP 400 with error code `INVALID_SETTINGS`
  indicating the ceiling value

### Scenario: Invalid screenShareMode value

- **WHEN** a host submits `POST /v1/meetings` with
  `settings.screenShareMode = "UNKNOWN"`
- **THEN** the system returns HTTP 400 with a validation error

---

## Requirement: Meeting duration validation

The system SHALL validate that the scheduled duration is within
Consul-configured bounds.

### Scenario: Duration below minimum

- **WHEN** a host submits `POST /v1/meetings` with
  `endTime - startTime < min-duration-minutes` (default 15 min)
- **THEN** the system returns HTTP 400 with error code
  `INVALID_MEETING_DURATION`

### Scenario: Duration above maximum

- **WHEN** a host submits `POST /v1/meetings` with
  `endTime - startTime > max-duration-minutes` (default 480 min)
- **THEN** the system returns HTTP 400 with error code
  `INVALID_MEETING_DURATION`

### Scenario: Duration within bounds

- **WHEN** a host submits `POST /v1/meetings` with duration between min and max
- **THEN** the meeting is created successfully

### Scenario: Dynamic limit update

- **WHEN** the Consul KV `config/meeting-management/data` is updated with a new
  `max-duration-minutes`
- **THEN** subsequent scheduling requests use the new limit without service
  restart

---

## Requirement: Consul-driven dynamic limits

The system SHALL read `meeting.limits.max-participants-ceiling`,
`meeting.limits.max-duration-minutes`, and `meeting.limits.min-duration-minutes`
from Consul KV and hot-reload them without restart.

### Scenario: Service starts without Consul

- **WHEN** Consul is unavailable at startup (`optional:consul:` import)
- **THEN** the service starts successfully using default fallback values
  (ceiling=500, max=480min, min=15min)

### Scenario: Consul KV updated at runtime

- **WHEN** the Consul KV value for `meeting.limits.max-duration-minutes` is
  changed
- **THEN** within the watch interval the `MeetingLimitsConfig` bean reflects the
  new value

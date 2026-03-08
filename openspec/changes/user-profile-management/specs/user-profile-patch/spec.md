# ADDED Requirements

## Requirement: Authenticated user can partially update their profile

The system SHALL expose `PATCH /api/v1/users/me` for authenticated users. The
request body SHALL use `JsonNullable<T>` fields so that absent fields are
ignored and only present fields are applied. The fields `email` and `password`
SHALL NOT be updatable via this endpoint. If the `preferences` key is present in
the request, the system SHALL replace the entire stored preferences object with
the provided value (RFC 7386 JSON Merge Patch semantics). After a successful
update the system SHALL publish a `UserUpdatedEvent` to Kafka carrying the
updated `fullName`, `avatarUrl`, and `authProvider`.

### Scenario: Update fullName only

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me` with body
  `{"fullName": "New Name"}`
- **THEN** the system SHALL update `fullName` to `"New Name"`, leave all other
  fields unchanged, return HTTP 200 with `JsendResponse.success` containing a
  `UserResponse`, and publish a `UserUpdatedEvent`

### Scenario: Clear avatarUrl by sending null

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me` with body
  `{"avatarUrl": null}`
- **THEN** the system SHALL set `avatarUrl` to `null`, leave all other fields
  unchanged, and return HTTP 200 with `UserResponse` where `avatarUrl` is
  absent/null

### Scenario: Replace entire preferences when key is present

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me` with body
  `{"preferences": {"theme": "dark", "defaultMic": false, "defaultCamera": true}}`
- **THEN** the system SHALL replace the stored preferences entirely with the
  provided object and return HTTP 200

### Scenario: Absent preferences key leaves preferences unchanged

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me` with body
  `{"fullName": "X"}` (no `preferences` key)
- **THEN** the system SHALL NOT modify the stored preferences

### Scenario: Empty body is a no-op

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me` with an empty
  body `{}`
- **THEN** the system SHALL return HTTP 200 with the current `UserResponse`
  unchanged and SHALL NOT publish a `UserUpdatedEvent`

### Scenario: fullName blank is rejected

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me` with body
  `{"fullName": ""}`
- **THEN** the system SHALL return HTTP 400 with `JsendResponse.fail` containing
  a `VALIDATION_ERROR` code

### Scenario: Unauthenticated request is rejected

- **WHEN** `PATCH /api/v1/users/me` is called without a valid Bearer token
- **THEN** the system SHALL return HTTP 401

---

## Requirement: Authenticated user can partially update individual preference fields

The system SHALL expose `PATCH /api/v1/users/me/preferences` for authenticated
users. The request body SHALL use `JsonNullable<T>` fields for `theme`,
`defaultMic`, and `defaultCamera`. Absent fields SHALL be left unchanged.
Present fields SHALL be merged into the existing preferences. The system SHALL
validate `theme` against the allowed values `dark`, `light`, `system` when
present.

### Scenario: Update theme only

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me/preferences` with
  body `{"theme": "dark"}`
- **THEN** the system SHALL update `theme` to `"dark"`, leave `defaultMic` and
  `defaultCamera` unchanged, and return HTTP 200 with `JsendResponse.success`
  containing a `UserPreferencesResponse`

### Scenario: Update multiple preference fields

- **WHEN** an authenticated user sends `PATCH /api/v1/users/me/preferences` with
  body `{"defaultMic": false, "defaultCamera": false}`
- **THEN** the system SHALL update both fields and return HTTP 200

### Scenario: Invalid theme value is rejected

- **WHEN** `PATCH /api/v1/users/me/preferences` is called with
  `{"theme": "blue"}`
- **THEN** the system SHALL return HTTP 400 with `VALIDATION_ERROR`

### Scenario: Unauthenticated request is rejected

- **WHEN** `PATCH /api/v1/users/me/preferences` is called without a valid Bearer
  token
- **THEN** the system SHALL return HTTP 401

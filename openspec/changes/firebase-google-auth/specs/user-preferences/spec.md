# ADDED Requirements

## Requirement: User can retrieve their preferences

The system SHALL expose `GET /api/v1/users/me/preferences` for authenticated
users. The response SHALL contain the current `theme`, `defaultMic`, and
`defaultCamera` values. If no preferences are stored, the system SHALL return
the defaults (`theme = "system"`, `defaultMic = true`, `defaultCamera = true`).

### Scenario: Authenticated user retrieves stored preferences

- **WHEN** an authenticated user sends `GET /api/v1/users/me/preferences`
- **THEN** the system SHALL return HTTP 200 with `JsendResponse.success`
  containing
  `{ "theme": "<value>", "defaultMic": <bool>, "defaultCamera": <bool> }`

### Scenario: Unauthenticated request is rejected

- **WHEN** `GET /api/v1/users/me/preferences` is called without a valid Bearer
  token
- **THEN** the system SHALL return HTTP 401

## Requirement: User can update their preferences

The system SHALL expose `PUT /api/v1/users/me/preferences` for authenticated
users. The request body SHALL contain `theme` (one of `"dark"`, `"light"`,
`"system"`), `defaultMic` (boolean), and `defaultCamera` (boolean). The system
SHALL persist the new preferences and return the updated values.

### Scenario: Authenticated user updates all preferences

- **WHEN** an authenticated user sends `PUT /api/v1/users/me/preferences` with a
  valid body `{ "theme": "dark", "defaultMic": false, "defaultCamera": true }`
- **THEN** the system SHALL persist the preferences to the `preferences` JSONB
  column and return HTTP 200 with `JsendResponse.success` containing the updated
  preferences

### Scenario: Invalid theme value is rejected

- **WHEN** `PUT /api/v1/users/me/preferences` is called with `"theme": "blue"`
  (not one of `dark`, `light`, `system`)
- **THEN** the system SHALL return HTTP 400 with `JsendResponse.fail` containing
  a validation error

### Scenario: Unauthenticated update request is rejected

- **WHEN** `PUT /api/v1/users/me/preferences` is called without a valid Bearer
  token
- **THEN** the system SHALL return HTTP 401

## Requirement: Preferences are valid JSON stored in JSONB column

The system SHALL store user preferences as a JSON object in the existing
`preferences JSONB` column. The stored object SHALL always contain exactly the
keys `theme`, `defaultMic`, and `defaultCamera`. Partial updates SHALL NOT be
supported — the entire preferences object is replaced on each `PUT`.

### Scenario: Stored preferences survive a round-trip

- **WHEN** a user updates preferences via `PUT` and then retrieves them via
  `GET`
- **THEN** the retrieved values SHALL exactly match the values sent in the `PUT`
  request

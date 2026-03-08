# ADDED Requirements

## Requirement: Username format validation

Username SHALL be a string of 3–30 characters containing only alphanumeric
characters, underscores (`_`), and hyphens (`-`). The system SHALL reject any
username that does not match the pattern `^[a-zA-Z0-9_-]{3,30}$`.

### Scenario: Valid username accepted

- **WHEN** a user provides a username matching `^[a-zA-Z0-9_-]{3,30}$`
- **THEN** the system SHALL accept the username without validation error

### Scenario: Username too short rejected

- **WHEN** a user provides a username shorter than 3 characters
- **THEN** the system SHALL return a 400 Bad Request with a validation error on
  the `username` field

### Scenario: Username too long rejected

- **WHEN** a user provides a username longer than 30 characters
- **THEN** the system SHALL return a 400 Bad Request with a validation error on
  the `username` field

### Scenario: Username with invalid characters rejected

- **WHEN** a user provides a username containing characters outside
  `[a-zA-Z0-9_-]` (e.g., spaces, `@`, `.`)
- **THEN** the system SHALL return a 400 Bad Request with a validation error on
  the `username` field

---

## Requirement: Username uniqueness among active users

The system SHALL enforce that no two active (non-deleted) users share the same
username. A soft-deleted user's username SHALL be available for reuse by a new
registration or update.

### Scenario: Duplicate username on registration rejected

- **WHEN** a user attempts to register with a username already held by an active
  user
- **THEN** the system SHALL return a 409 Conflict with error code
  `USERNAME_ALREADY_EXISTS`

### Scenario: Duplicate username on patch update rejected

- **WHEN** a user attempts to update their username to one already held by
  another active user
- **THEN** the system SHALL return a 409 Conflict with error code
  `USERNAME_ALREADY_EXISTS`

### Scenario: Soft-deleted user's username is reusable

- **WHEN** a user is soft-deleted and a new user registers with the same
  username
- **THEN** the system SHALL allow the registration to succeed

---

## Requirement: Username required on email registration

The `POST /api/v1/auth/register` endpoint SHALL require a `username` field.
Registration without a `username` SHALL be rejected.

### Scenario: Registration with username succeeds

- **WHEN** a client sends a valid registration request including a valid
  `username`
- **THEN** the system SHALL create the user and return a 201 response including
  the `username` field

### Scenario: Registration without username rejected

- **WHEN** a client sends a registration request without the `username` field
- **THEN** the system SHALL return a 400 Bad Request with a validation error on
  the `username` field

---

## Requirement: Username auto-generated for Google OAuth users

When a new user is created via Google OAuth (`POST /api/v1/auth/google-login`),
the system SHALL automatically generate a unique username at the application
layer in the format `user_<8-char-hex>`. The generated username SHALL pass the
uniqueness check before being assigned.

### Scenario: New Google user gets auto-generated username

- **WHEN** a new user authenticates via Google OAuth for the first time
- **THEN** the system SHALL assign a generated username in the format
  `user_[a-f0-9]{8}` and return it in the response

### Scenario: Generated username collision is retried

- **WHEN** the generated username candidate already exists for an active user
- **THEN** the system SHALL generate a new candidate and retry until a unique
  username is found

---

## Requirement: Username updatable via patch

The `PATCH /api/v1/users/me` endpoint SHALL accept an optional `username` field.
When provided, the system SHALL validate and apply the new username, enforcing
uniqueness among active users.

### Scenario: Successful username update

- **WHEN** an authenticated user sends a PATCH request with a valid, unique
  `username`
- **THEN** the system SHALL update the username and return the updated user
  profile including the new `username`

### Scenario: Username update to own current username is idempotent

- **WHEN** an authenticated user sends a PATCH request with their current
  username
- **THEN** the system SHALL treat it as no change and return success without
  error

---

## Requirement: Username included in user response

All endpoints that return user data SHALL include the `username` field in the
response payload. The field SHALL be `null` only for legacy users who have not
yet been backfilled (transitional period only).

### Scenario: GET /users/me includes username

- **WHEN** an authenticated user calls `GET /api/v1/users/me`
- **THEN** the response SHALL include a `username` field

### Scenario: GET /users/{id} includes username

- **WHEN** a caller requests `GET /api/v1/users/{id}`
- **THEN** the response SHALL include a `username` field

### Scenario: GET /users slice includes username per item

- **WHEN** a caller requests the users slice endpoint
- **THEN** each user object in the response SHALL include a `username` field

---

## Requirement: Username propagated in domain events

`UserRegisteredEvent` and `UserUpdatedEvent` SHALL include the `username` field
so downstream consumers can maintain consistent user projections.

### Scenario: UserRegisteredEvent carries username

- **WHEN** a new user is registered (email or Google OAuth)
- **THEN** the published `UserRegisteredEvent` SHALL include the `username`
  field

### Scenario: UserUpdatedEvent carries new username

- **WHEN** a user updates their username via PATCH
- **THEN** the published `UserUpdatedEvent` SHALL include the updated `username`
  field

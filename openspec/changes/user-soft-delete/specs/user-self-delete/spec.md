# ADDED Requirements

## Requirement: Authenticated user can soft-delete their own account

The system SHALL allow an authenticated user to soft-delete their own account by
calling `DELETE /api/v1/auth/me`. The operation SHALL set `deleted_at` to the
current timestamp on the `User` aggregate. The operation is one-way for the user
— no self-restore is available. All refresh tokens belonging to the user SHALL
be revoked immediately as part of the same transaction.

### Scenario: Successful self-delete

- **WHEN** an authenticated user sends `DELETE /api/v1/auth/me` with a valid JWT
- **THEN** the system sets `deleted_at = now()` on the user record
- **AND** all refresh tokens for that user are revoked
- **AND** the response is `204 No Content`

### Scenario: Deleted user cannot use existing JWT

- **WHEN** a user whose `deleted_at` is non-null presents a valid (non-expired)
  JWT
- **THEN** the system rejects the request with `401 Unauthorized`

### Scenario: Deleted user cannot log in

- **WHEN** a deleted user attempts to log in with correct credentials
- **THEN** the system returns `401 Unauthorized` with error code `USER_DELETED`

### Scenario: Deleted user's email can be re-registered

- **WHEN** a new user attempts to register with an email belonging to a
  soft-deleted account
- **THEN** the system allows registration and creates a new active user with
  that email

### Scenario: Self-delete on already-deleted account

- **WHEN** an authenticated user whose account is already soft-deleted sends
  `DELETE /api/v1/auth/me`
- **THEN** the system returns `401 Unauthorized` (JWT guard rejects before
  reaching use case)

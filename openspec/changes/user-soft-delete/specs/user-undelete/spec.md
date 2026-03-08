# ADDED Requirements

## Requirement: Admin can restore a soft-deleted user account

The system SHALL allow an authenticated user to restore a soft-deleted account
via `POST /api/v1/auth/me:undelete`. The request body SHALL include the `userId`
of the account to restore. The operation SHALL clear `deleted_at` (set to
`null`) on the target `User` aggregate. No RBAC enforcement is applied in this
iteration — any valid JWT can call this endpoint.

### Scenario: Successful undelete

- **WHEN** an authenticated user sends `POST /api/v1/auth/me:undelete` with a
  valid `userId` of a soft-deleted account
- **THEN** the system sets `deleted_at = null` on the target user
- **AND** the response is `204 No Content`

### Scenario: Undelete a non-deleted user

- **WHEN** the `userId` in the request refers to a user whose `deleted_at` is
  already `null`
- **THEN** the system returns a failure response with error code
  `USER_NOT_DELETED`

### Scenario: Undelete a non-existent user

- **WHEN** the `userId` in the request does not match any user record
- **THEN** the system returns a failure response with error code
  `USER_NOT_FOUND`

### Scenario: Unauthenticated undelete attempt

- **WHEN** a request to `POST /api/v1/auth/me:undelete` is made without a valid
  JWT
- **THEN** the system returns `401 Unauthorized`

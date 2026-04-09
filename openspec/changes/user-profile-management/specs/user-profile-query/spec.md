# ADDED Requirements

## Requirement: Authenticated user can retrieve their own profile

The system SHALL expose `GET /api/v1/users/me` for authenticated users. The
response SHALL contain the full user profile including `id`, `email`,
`fullName`, `avatarUrl`, `authProvider`, `preferences` (with defaults applied if
not stored), `createdAt`, and `updatedAt`. The fields `hashedPassword` and
`googleUid` SHALL NOT be included in the response.

### Scenario: Authenticated user retrieves own profile

- **WHEN** an authenticated user sends `GET /api/v1/users/me`
- **THEN** the system SHALL return HTTP 200 with `JsendResponse.success`
  containing a `UserResponse` with all profile fields populated

### Scenario: Preferences default when not stored

- **WHEN** an authenticated user with no stored preferences sends
  `GET /api/v1/users/me`
- **THEN** the system SHALL return `preferences` with defaults:
  `{"theme": "system", "defaultMic": true, "defaultCamera": true}`

### Scenario: Unauthenticated request is rejected

- **WHEN** `GET /api/v1/users/me` is called without a valid Bearer token
- **THEN** the system SHALL return HTTP 401

---

## Requirement: Authenticated user can retrieve any user profile by ID

The system SHALL expose `GET /api/v1/users/{id}` for authenticated users. Any
authenticated user SHALL be able to retrieve any active user's profile by their
UUID. If the user does not exist or has been soft-deleted, the system SHALL
return HTTP 404.

### Scenario: Retrieve existing active user by ID

- **WHEN** an authenticated user sends `GET /api/v1/users/{id}` with a valid
  UUID of an active user
- **THEN** the system SHALL return HTTP 200 with `JsendResponse.success`
  containing a `UserResponse`

### Scenario: User not found returns 404

- **WHEN** an authenticated user sends `GET /api/v1/users/{id}` with a UUID that
  does not exist or belongs to a soft-deleted user
- **THEN** the system SHALL return HTTP 404 with `JsendResponse.fail` containing
  `USER_NOT_FOUND`

### Scenario: Unauthenticated request is rejected

- **WHEN** `GET /api/v1/users/{id}` is called without a valid Bearer token
- **THEN** the system SHALL return HTTP 401

---

## Requirement: Authenticated user can list active users as a paginated slice

The system SHALL expose `GET /api/v1/users` for authenticated users. The
endpoint SHALL return a `Slice`-based paginated list of active (non-deleted)
users wrapped in `SliceHttpResponse`. The endpoint SHALL support optional
filtering by `email` (case-insensitive substring match) and `authProvider`
(exact match: `EMAIL`, `GOOGLE`, or `BOTH`). The endpoint SHALL support `page`
(0-indexed, default `0`) and `size` (default `20`, max `100`) query parameters.
Results SHALL be ordered by `createdAt` descending.

### Scenario: List users with default pagination

- **WHEN** an authenticated user sends `GET /api/v1/users`
- **THEN** the system SHALL return HTTP 200 with `JsendResponse.success`
  containing a `SliceHttpResponse` with up to 20 active users ordered by
  `createdAt` descending

### Scenario: Filter by email substring

- **WHEN** an authenticated user sends `GET /api/v1/users?email=nguyen`
- **THEN** the system SHALL return only active users whose email contains
  `"nguyen"` (case-insensitive)

### Scenario: Filter by authProvider

- **WHEN** an authenticated user sends `GET /api/v1/users?authProvider=GOOGLE`
- **THEN** the system SHALL return only active users with
  `authProvider = "GOOGLE"`

### Scenario: Combined filters

- **WHEN** an authenticated user sends
  `GET /api/v1/users?email=alice&authProvider=EMAIL`
- **THEN** the system SHALL return only active users matching both filters

### Scenario: Empty result set

- **WHEN** filters match no active users
- **THEN** the system SHALL return HTTP 200 with an empty `content` array and
  `hasNext: false`

### Scenario: Soft-deleted users are excluded

- **WHEN** `GET /api/v1/users` is called
- **THEN** the system SHALL NOT include users whose `deletedAt` is non-null

### Scenario: Unauthenticated request is rejected

- **WHEN** `GET /api/v1/users` is called without a valid Bearer token
- **THEN** the system SHALL return HTTP 401

# ADDED Requirements

## Requirement: User can log out by revoking their refresh token

The system SHALL accept a raw refresh token and revoke it by setting
`revoked_at = now()` in the `refresh_tokens` table. The request SHALL require a
valid JWT access token in the `Authorization` header. After logout, the revoked
refresh token SHALL NOT be usable for token rotation. The system SHALL return
success even if the token is already revoked (idempotent logout).

### Scenario: Successful logout

- **WHEN** a POST request is sent to `/api/v1/auth/logout` with a valid
  `Authorization: Bearer <accessToken>` header and a body containing a valid
  `refreshToken`
- **THEN** the system SHALL set `revoked_at = now()` on the matching
  `refresh_tokens` row and return HTTP 200 with `JsendResponse.success(null)`

### Scenario: Logout is idempotent for already-revoked token

- **WHEN** a POST request is sent to `/api/v1/auth/logout` with a refresh token
  that is already revoked
- **THEN** the system SHALL return HTTP 200 with `JsendResponse.success(null)`
  without error

### Scenario: Logout requires authenticated access token

- **WHEN** a POST request is sent to `/api/v1/auth/logout` without a valid
  `Authorization` header or with an expired access token
- **THEN** the system SHALL return HTTP 401 before processing the refresh token

### Scenario: Unknown refresh token on logout

- **WHEN** a POST request is sent to `/api/v1/auth/logout` with a refresh token
  whose SHA-256 hash does not match any row in `refresh_tokens`
- **THEN** the system SHALL return HTTP 401 and respond with
  `JsendResponse.fail` containing error code `REFRESH_TOKEN_NOT_FOUND`

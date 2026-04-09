# ADDED Requirements

## Requirement: Client can obtain a new token pair using a valid refresh token

The system SHALL accept a raw refresh token, compute its SHA-256 hash, and look
it up in the `refresh_tokens` table. If valid (not expired, not revoked), the
system SHALL atomically revoke the old token (set `revoked_at = now()`) and
issue a new access token + refresh token pair. The system SHALL implement reuse
detection: if a previously revoked token is presented, the system SHALL revoke
ALL active refresh tokens for that user and return error code
`REFRESH_TOKEN_REUSE_DETECTED`.

### Scenario: Successful token rotation

- **WHEN** a POST request is sent to `/api/v1/auth/refresh` with a valid,
  non-expired, non-revoked refresh token
- **THEN** the system SHALL return HTTP 200 with a new `accessToken` and
  `refreshToken`, and the old refresh token's `revoked_at` SHALL be set to the
  current timestamp

### Scenario: Expired refresh token rejected

- **WHEN** a POST request is sent to `/api/v1/auth/refresh` with a refresh token
  whose `expires_at` is in the past
- **THEN** the system SHALL return HTTP 401 and respond with
  `JsendResponse.fail` containing error code `REFRESH_TOKEN_EXPIRED`

### Scenario: Unknown refresh token rejected

- **WHEN** a POST request is sent to `/api/v1/auth/refresh` with a token whose
  SHA-256 hash does not match any row in `refresh_tokens`
- **THEN** the system SHALL return HTTP 401 and respond with
  `JsendResponse.fail` containing error code `REFRESH_TOKEN_NOT_FOUND`

### Scenario: Reuse detection triggers full session revocation

- **WHEN** a POST request is sent to `/api/v1/auth/refresh` with a refresh token
  that has already been revoked (i.e., `revoked_at IS NOT NULL`)
- **THEN** the system SHALL revoke ALL refresh tokens for the associated
  `user_id` (set `revoked_at = now()` on all active tokens) and return HTTP 401
  with error code `REFRESH_TOKEN_REUSE_DETECTED`

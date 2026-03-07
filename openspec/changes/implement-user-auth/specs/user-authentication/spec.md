# ADDED Requirements

## Requirement: User can log in with email and password

The system SHALL authenticate a user by verifying their email and
Argon2id-hashed password. On success, the system SHALL issue a JWT access token
(HS256, 15-minute expiry) and a refresh token (cryptographically random 32
bytes, Base64URL-encoded, 30-day expiry). The raw refresh token SHALL be
returned to the client; only its SHA-256 hash SHALL be stored in the
`refresh_tokens` table. On failure, the system SHALL return error code
`INVALID_CREDENTIALS` without revealing whether the email or password was wrong.

### Scenario: Successful login

- **WHEN** a POST request is sent to `/api/v1/auth/login` with a valid email and
  correct password
- **THEN** the system SHALL return HTTP 200 and respond with
  `JsendResponse.success` containing `accessToken`, `refreshToken`, and
  `expiresIn` (seconds until access token expiry)

### Scenario: Wrong password rejected

- **WHEN** a POST request is sent to `/api/v1/auth/login` with a valid email but
  incorrect password
- **THEN** the system SHALL return HTTP 401 and respond with
  `JsendResponse.fail` containing error code `INVALID_CREDENTIALS`

### Scenario: Unknown email rejected

- **WHEN** a POST request is sent to `/api/v1/auth/login` with an email that
  does not exist in the `users` table
- **THEN** the system SHALL return HTTP 401 and respond with
  `JsendResponse.fail` containing error code `INVALID_CREDENTIALS` (same
  response as wrong password — no user enumeration)

### Scenario: Refresh token stored as hash only

- **WHEN** a user successfully logs in
- **THEN** the `refresh_tokens` table SHALL contain a row with `token_hash`
  equal to `SHA-256(rawRefreshToken)` and `revoked_at` SHALL be NULL

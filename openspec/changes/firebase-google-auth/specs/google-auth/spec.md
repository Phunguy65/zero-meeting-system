# ADDED Requirements

## Requirement: User can sign in with Google via Firebase ID token

The system SHALL accept a Firebase ID token at `POST /api/v1/auth/google-login`,
verify it using Firebase Admin SDK, and return a `LoginResponse` containing an
access token, refresh token, expiry, and user preferences. If no account exists
for the verified email, the system SHALL create one automatically. If an account
already exists (registered via email/password), the system SHALL link the Google
UID to that account and return tokens for the existing account.

### Scenario: First-time Google login creates a new user

- **WHEN** `POST /api/v1/auth/google-login` is called with a valid Firebase ID
  token for an email that does not exist in the `users` table
- **THEN** the system SHALL create a new user with `auth_provider = 'GOOGLE'`,
  `google_uid` set to the Firebase UID, `password_hash = NULL`, and default
  preferences, and return HTTP 200 with `LoginResponse`

### Scenario: Returning Google user receives tokens

- **WHEN** `POST /api/v1/auth/google-login` is called with a valid Firebase ID
  token for an email that already exists with `auth_provider = 'GOOGLE'`
- **THEN** the system SHALL return HTTP 200 with a new `LoginResponse` without
  modifying the user record

### Scenario: Google login links to existing email/password account

- **WHEN** `POST /api/v1/auth/google-login` is called with a valid Firebase ID
  token whose email matches an existing `auth_provider = 'EMAIL'` user
- **THEN** the system SHALL set `google_uid` and update `auth_provider = 'BOTH'`
  on that user, and return HTTP 200 with `LoginResponse`

### Scenario: Invalid Firebase ID token is rejected

- **WHEN** `POST /api/v1/auth/google-login` is called with a malformed, expired,
  or tampered Firebase ID token
- **THEN** the system SHALL return HTTP 401 with `JsendResponse.fail` containing
  error code `INVALID_FIREBASE_TOKEN`

### Scenario: Soft-deleted user cannot sign in via Google

- **WHEN** `POST /api/v1/auth/google-login` is called with a valid token whose
  email matches a soft-deleted user (`deleted_at IS NOT NULL`)
- **THEN** the system SHALL return HTTP 401 with error code `USER_DELETED`

## Requirement: LoginResponse includes user preferences

The system SHALL include a `preferences` object in every `LoginResponse` (both
email/password and Google login). If the user has no stored preferences, the
system SHALL return default values: `theme = "system"`, `defaultMic = true`,
`defaultCamera = true`.

### Scenario: Login response contains preferences for user with stored preferences

- **WHEN** a user with non-null `preferences` JSON in the database successfully
  logs in via any method
- **THEN** the `LoginResponse.preferences` field SHALL contain the parsed
  `theme`, `defaultMic`, and `defaultCamera` values from the stored JSON

### Scenario: Login response contains default preferences for new user

- **WHEN** a user is created via Google login for the first time (no stored
  preferences)
- **THEN** the `LoginResponse.preferences` field SHALL contain
  `{ "theme": "system", "defaultMic": true, "defaultCamera": true }`

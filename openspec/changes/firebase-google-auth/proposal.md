# Why

The user-management service currently supports only email/password
authentication. Adding Google Sign-In via Firebase Authentication reduces
friction for new users and aligns with modern meeting-platform expectations. The
`preferences` column already exists in the schema but has no typed contract or
API surface — this change formalises it so clients can persist and retrieve
per-user settings (theme, mic, camera defaults) immediately after login.

## What Changes

- Add Firebase Admin SDK to `user-management` to verify Google ID tokens issued
  by the Firebase client SDK on the web/Android clients.
- Introduce a new `POST /api/v1/auth/google-login` endpoint that accepts a
  Firebase ID token, finds-or-creates the user, and returns the same
  `LoginResponse` shape already used by email/password login (extended with
  `preferences`).
- **BREAKING** — `password_hash` column becomes nullable to support Google-only
  accounts that have no password.
- Add `google_uid VARCHAR(128)` and `auth_provider VARCHAR(20)` columns to
  `users` (Flyway V5).
- Extend `LoginResponse` with a `preferences` field (`UserPreferencesDto`) so
  clients receive theme/mic/camera settings in a single round-trip after any
  login.
- Add `GET /api/v1/users/me/preferences` and `PUT /api/v1/users/me/preferences`
  endpoints for post-login preference management.
- Web client: add `firebase` npm package (auth module only) and implement Google
  Sign-In flow that sends the ID token to the backend.

## Capabilities

### New Capabilities

- `google-auth`: Firebase-backed Google Sign-In flow — ID token verification,
  find-or-create user, account linking by email, JWT issuance.
- `user-preferences`: Typed read/write API for per-user preferences (theme,
  defaultMic, defaultCamera) stored in the existing JSONB column.

### Modified Capabilities

<!-- No existing spec files — no delta specs required. -->

## Impact

**Backend (user-management)**

- New dependency: `com.google.firebase:firebase-admin:9.8.0`
- New Flyway migration V5: nullable `password_hash`, add `google_uid`,
  `auth_provider`
- New classes: `FirebaseConfig`, `LoginWithGoogleUseCase`, `UserPreferencesDto`,
  `GetUserPreferencesUseCase`, `UpdateUserPreferencesUseCase`
- Modified: `User` domain model, `UserJpaEntity`, `LoginResponse`,
  `AuthController`, `SecurityConfig`, `AuthErrorCode`

**Frontend (web)**

- New dependency: `firebase` (npm, auth module only)
- New files: `src/lib/firebase.ts`, `src/hooks/useGoogleAuth.ts`,
  `src/components/GoogleLoginButton.tsx`
- New env vars: `NEXT_PUBLIC_FIREBASE_API_KEY`,
  `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`, `NEXT_PUBLIC_FIREBASE_PROJECT_ID`

**Infrastructure**

- New env vars on backend: `FIREBASE_PROJECT_ID`,
  `GOOGLE_APPLICATION_CREDENTIALS`
- `compose.yaml` updated to mount service-account JSON secret

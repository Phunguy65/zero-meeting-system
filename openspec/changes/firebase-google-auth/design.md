# Context

`user-management` currently supports email/password authentication only. The
`users` table has a `preferences JSONB` column that exists in the schema but is
never read or written through any API. The frontend (Next.js) and Android client
have no auth implementation yet — both are fresh scaffolds.

The change introduces two orthogonal concerns that are delivered together
because they share the same login response contract:

1. **Google Sign-In** — Firebase ID token verification on the backend,
   find-or-create user, JWT issuance using the existing `JwtTokenProvider`.
2. **User Preferences API** — typed read/write surface for the existing JSONB
   column, with preferences returned inline on every login response.

## Goals / Non-Goals

**Goals:**

- Allow users to sign in with their Google account via Firebase Authentication.
- Link Google accounts to existing email/password accounts by email address.
- Return typed `UserPreferencesDto` (theme, defaultMic, defaultCamera) in every
  `LoginResponse` so clients need zero extra round-trips after login.
- Expose `GET /PUT /api/v1/users/me/preferences` for post-login preference
  updates.
- Keep the existing email/password flow completely unchanged.

**Non-Goals:**

- Firebase Authentication for any service other than `user-management`.
- Social login providers other than Google (GitHub, Apple, etc.).
- Server-side session management — the system remains stateless (JWT only).
- Android client implementation (out of scope for this change; backend contract
  is sufficient for Android to integrate independently).
- Role-based access control or permission changes.

## Decisions

### D1 — Firebase Admin SDK verifies tokens; backend issues its own JWT

**Decision**: The backend verifies the Firebase ID token using
`FirebaseAuth.verifyIdToken()`, then issues its own short-lived JWT (HS256, 15
min) + refresh token — identical to the email/password flow.

**Rationale**: Clients already handle the existing JWT/refresh-token contract.
Keeping Firebase as a verification-only step means no Firebase dependency leaks
into downstream services or the JWT filter chain. Firebase tokens expire in 1
hour and cannot be revoked server-side without Firebase Admin calls; our own JWT
gives us full control.

**Alternative considered**: Accept Firebase ID tokens directly on every request
(no app JWT). Rejected — would require every protected endpoint to call
Firebase, adding latency and a hard external dependency on the hot path.

---

### D2 — Account linking by email (not by Firebase UID)

**Decision**: When a Google login arrives, look up the user by `email`. If
found, link the Google UID to that account (`google_uid`,
`auth_provider = BOTH`). If not found, create a new user.

**Rationale**: Users expect "sign in with Google" to access the same account
they registered with their email. Treating them as separate accounts would be
confusing and create duplicate records.

**Alternative considered**: Strict UID-based lookup (no linking). Rejected —
breaks the common case where a user registered with email first.

---

### D3 — `password_hash` becomes nullable

**Decision**: Alter `password_hash` to `VARCHAR(255) NULL` (Flyway V5).
Google-only users have `password_hash = NULL`. `LoginUserUseCase` guards against
null before calling `passwordHasher.verify()`.

**Rationale**: Storing a sentinel/random hash is misleading and makes it
impossible to distinguish "has password" from "Google-only" at the domain level.
Nullable is the honest representation.

**Alternative considered**: Sentinel hash (`GOOGLE_AUTH_<uuid>`). Rejected —
leaks implementation detail into the domain, breaks `verify()` semantics.

---

### D4 — `UserPreferences` as a typed record, serialised to JSON string in domain

**Decision**: Introduce `UserPreferencesDto` (Java record) in the application
layer. The domain `User` keeps `String preferences` (raw JSON). The use case
layer deserialises/serialises via `ObjectMapper`. The JPA entity keeps
`@JdbcTypeCode(SqlTypes.JSON)`.

**Rationale**: The domain model is intentionally persistence-ignorant. Keeping
`preferences` as a `String` in the domain avoids coupling the aggregate to
Jackson. The application layer (use cases / DTOs) is the right place for
serialisation.

**Alternative considered**: Typed `UserPreferences` value object in the domain.
Rejected — adds Jackson dependency to the domain layer, violates hexagonal
boundaries.

---

### D5 — Preferences returned inline in `LoginResponse`

**Decision**: Extend `LoginResponse` with `UserPreferencesDto preferences`. If
the stored JSON is null or unparseable, return `UserPreferencesDto.defaults()`.

**Rationale**: Clients need preferences immediately after login to render the
correct theme and configure media devices. A separate `GET /preferences` call
would add a round-trip on every app start.

---

### D6 — Firebase credentials via `GOOGLE_APPLICATION_CREDENTIALS` env var

**Decision**: Follow the existing `${ENV_VAR:default}` pattern in
`application.properties`. `FirebaseConfig` reads `app.firebase.credentials-path`
(mapped from `GOOGLE_APPLICATION_CREDENTIALS`) and falls back to
`GoogleCredentials.getApplicationDefault()` for local dev (gcloud ADC).

**Rationale**: Consistent with how `JWT_SECRET` and `KAFKA_BOOTSTRAP_SERVERS`
are already handled. No new secret-management infrastructure needed.

## Risks / Trade-offs

- **Firebase outage on login** → `FirebaseAuth.verifyIdToken()` makes a network
  call to Google on first use (then caches public keys). If Firebase is
  unreachable, Google logins fail. Mitigation: return `503` with a clear error
  code; email/password login is unaffected.
- **Email collision edge case** → Two users could theoretically register with
  the same email via different providers in a race. Mitigation: `email` column
  has a `UNIQUE` constraint; the second insert will fail with a DB constraint
  violation, which the use case catches and maps to `EMAIL_ALREADY_EXISTS`.
- **`password_hash` nullable migration** → Existing rows all have non-null
  hashes, so the `ALTER COLUMN` is safe. `LoginUserUseCase` must null-check
  before calling `passwordHasher.verify()` to avoid NPE.
- **Firebase Admin SDK jar size** (~5 MB transitive) → Acceptable for a backend
  service; no impact on frontend bundle.

## Migration Plan

1. Deploy Flyway V5 migration (nullable `password_hash`, add `google_uid`,
   `auth_provider`). Safe — existing rows unaffected, new columns are
   nullable/have defaults.
2. Deploy updated `user-management` service with new endpoint and
   `LoginResponse` shape. `LoginResponse` gains a new `preferences` field —
   existing clients that ignore unknown JSON fields are unaffected.
3. Deploy web client with Firebase SDK and Google login button.
4. No rollback complexity — new endpoint is additive; old endpoints unchanged.

## Open Questions

- Should `auth_provider` be an enum column (`EMAIL`, `GOOGLE`, `BOTH`) or a
  free-text `VARCHAR`? Enum is safer but requires a Postgres `CREATE TYPE`.
  Recommend `VARCHAR(20)` with application-level validation for simplicity.
- Token expiry for Google-authenticated users: same 15-min access / 30-day
  refresh as email users? Assumed yes — no reason to differentiate.

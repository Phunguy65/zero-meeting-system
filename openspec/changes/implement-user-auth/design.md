# Context

The `user-management` service is a greenfield Spring Boot 4 / Java 25
microservice within the zero-meeting-system. It has Spring Security and JJWT
already on the classpath via convention plugins, but zero business logic
implemented. The shared module provides `AggregateRoot<ID>`, `Result<T,E>`,
`ValueObject`, `JsendResponse`, and `GlobalExceptionHandler` — all of which this
service must use consistently.

The service must expose a stateless JWT-based auth API consumed by both the
Android native app and the Next.js web app.

## Goals / Non-Goals

**Goals:**

- Implement clean architecture (domain → application → infrastructure →
  presentation) with strict layer boundaries
- Deliver register, login, logout, and refresh-token endpoints
- Secure passwords with Argon2id; secure refresh tokens with SHA-256 hashing
- Issue short-lived JWT access tokens (15 min) and long-lived refresh tokens (30
  days)
- Implement refresh token rotation with reuse detection (revoke all tokens on
  reuse attempt)
- Define all error conditions as typed `ErrorCode` enum constants — no hardcoded
  strings anywhere
- Use UUIDv7 for all primary keys
- Manage schema with Flyway migrations

**Non-Goals:**

- OAuth2 / social login (future change)
- Email verification flow (future change)
- Password reset / forgot password (future change)
- Role-based access control (future change)
- Rate limiting (infrastructure concern, handled at gateway level)

## Decisions

### D1: Clean Architecture Package Structure

**Decision**: Four top-level packages under
`io.github.phunguy65.zms.usermanagement`:

- `domain` — pure Java, zero Spring/JPA annotations
- `application` — use cases, DTOs, orchestration
- `infrastructure` — JPA entities, repositories, security adapters
- `presentation` — REST controllers

**Rationale**: Enforces dependency rule (outer layers depend on inner, never
reverse). Domain stays testable without Spring context.

**Alternative considered**: Feature-based packaging (`auth/`, `profile/`) —
rejected because it blurs layer boundaries and makes dependency enforcement
harder.

---

### D2: Password Hashing — Argon2id

**Decision**: Use `Argon2PasswordEncoder` from Spring Security (built-in since
Spring Security 5.8, available in Spring Security 7).

**Rationale**: Argon2id is the 2025 OWASP recommendation. It is memory-hard and
resists GPU/ASIC brute-force attacks better than BCrypt. Spring Security
provides it out of the box — no extra dependency.

**Alternative considered**: BCrypt — still acceptable but inferior for new
implementations.

---

### D3: JWT Signing — HS256 with configurable secret

**Decision**: Use HMAC-SHA256 (HS256) with a secret key loaded from
`application.properties` / environment variable.

**Rationale**: All token verification in this system is done by this single
service (or via shared secret passed to other services). HS256 is simpler to
operate than RS256 for this stage. The secret is externalized via config — easy
to rotate.

**Alternative considered**: RS256 (asymmetric) — better for multi-service
verification without sharing secrets. Deferred to a future change when other
services need to verify tokens independently.

---

### D4: Refresh Token Transport

**Decision**: Return both `access_token` and `refresh_token` in the JSON
response body. Clients are responsible for storage.

**Rationale**: The Android native app cannot use `HttpOnly` cookies in the same
way a browser can. A body-based approach works uniformly for both Android and
web clients. The web client should store the refresh token in memory or
`HttpOnly` cookie at the BFF layer if needed.

**Alternative considered**: `HttpOnly` cookie for refresh token — better XSS
protection for web, but incompatible with Android native client without extra
complexity.

---

### D5: Error Codes — Typed Enum

**Decision**: Define an `AuthErrorCode` enum (and extend the shared `ErrorCode`
pattern) for all auth failure scenarios. Controllers and use cases reference
enum constants only — no hardcoded string literals.

**Rationale**: Prevents typos, enables exhaustive switch handling, makes error
contracts explicit and refactor-safe. Clients can rely on stable string
representations of enum names.

**Error codes defined**:

```
EMAIL_ALREADY_EXISTS
INVALID_CREDENTIALS
REFRESH_TOKEN_NOT_FOUND
REFRESH_TOKEN_EXPIRED
REFRESH_TOKEN_REVOKED
REFRESH_TOKEN_REUSE_DETECTED
USER_NOT_FOUND
```

---

### D6: Refresh Token Rotation + Reuse Detection

**Decision**: On every `/auth/refresh` call:

1. SHA-256 hash the incoming raw token
2. Look up by hash in `refresh_tokens`
3. If found and `revoked_at IS NOT NULL` → reuse detected → revoke ALL tokens
   for that `user_id` → return `REFRESH_TOKEN_REUSE_DETECTED`
4. If found and `expires_at < now()` → return `REFRESH_TOKEN_EXPIRED`
5. If valid → set `revoked_at = now()` on old token → issue new token pair

**Rationale**: Detects token theft. If a stolen token is used after the
legitimate client already rotated it, the server sees a revoked token being
reused and invalidates the entire session family.

---

### D7: UUIDv7 via `uuid-creator`

**Decision**: Add `com.github.f4b6a3:uuid-creator` dependency. Generate UUIDv7
in domain constructors.

**Rationale**: UUIDv7 is time-ordered, which maintains PostgreSQL B-tree index
performance (avoids page splits from random UUIDv4). The `uuid-creator` library
is the most complete Java implementation.

---

### D8: Database Migrations — Flyway

**Decision**: Use Flyway with SQL migrations under
`src/main/resources/db/migration/`.

**Rationale**: Spring Boot auto-configures Flyway when it's on the classpath.
SQL migrations are explicit, reviewable, and version-controlled. Two initial
migrations: `V1__create_users.sql` and `V2__create_refresh_tokens.sql`.

---

### D9: JSONB `preferences` field

**Decision**: Map `preferences` as `String` (raw JSON) in the JPA entity using
`@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 7 native). Expose as
`Map<String, Object>` in the domain/DTO layer via Jackson deserialization.

**Rationale**: Avoids over-engineering a typed preferences model at this stage.
The schema comment shows it's a simple flat object (`default_mic`,
`default_cam`, `theme`).

## Risks / Trade-offs

- **HS256 secret rotation** → If the secret leaks, all issued tokens are
  compromised. Mitigation: load secret from environment variable, never commit
  to source. Future: migrate to RS256.
- **Refresh token table growth** → Long-lived tokens accumulate. Mitigation: add
  a scheduled cleanup job (future task) to purge
  `revoked_at IS NOT NULL OR expires_at < now() - interval '30 days'`.
- **No access token revocation** → Short-lived (15 min) access tokens cannot be
  revoked mid-flight. Mitigation: acceptable for this stage; logout only revokes
  the refresh token. Future: token blacklist via Redis if needed.
- **Argon2id memory cost on high concurrency** → Memory-hard hashing is
  CPU/memory intensive. Mitigation: tune parameters (memory=65536, iterations=3,
  parallelism=4) to balance security and throughput. Login is not a hot path.

## Open Questions

- Should the `preferences` field be validated on write (e.g., only allow known
  keys)? → Deferred; accept any JSON for now.
- What is the JWT secret rotation strategy in production? → Needs ops input;
  document as a deployment concern.

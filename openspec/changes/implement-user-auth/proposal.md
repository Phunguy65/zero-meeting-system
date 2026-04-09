# Why

The `user-management` service currently has no implementation — only a
scaffolded Spring Boot entry point. To support any authenticated feature in the
zero-meeting-system (joining meetings, managing profiles, etc.), a secure
identity foundation must be in place first. This change delivers the core auth
capabilities: registration, login, logout, and token-based security.

## What Changes

- Implement `User` aggregate and `RefreshToken` aggregate in the domain layer
- Add `register`, `login`, `logout`, and `refresh-token` use cases in the
  application layer
- Add JPA persistence adapters for `users` and `refresh_tokens` tables
- Configure Spring Security with stateless JWT filter chain
- Implement Argon2id password hashing and SHA-256 refresh token hashing
- Expose REST endpoints under `/api/v1/auth/*`
- Define typed error codes (no hardcoded strings) for all auth failure scenarios
- Add Flyway migrations for `users` and `refresh_tokens` tables

## Capabilities

### New Capabilities

- `user-registration`: Allow new users to create an account with email,
  password, and full name
- `user-authentication`: Authenticate existing users and issue JWT access
  token + refresh token pair
- `token-refresh`: Rotate refresh tokens securely with reuse detection
- `user-logout`: Revoke a refresh token to invalidate a session

### Modified Capabilities

<!-- None — this is a greenfield implementation -->

## Impact

- **New code**: All layers under `services/user-management/src/` (domain,
  application, infrastructure, presentation)
- **Database**: Two new tables (`users`, `refresh_tokens`) via Flyway migrations
- **Security**: Spring Security filter chain configured; all non-auth endpoints
  require valid JWT
- **Dependencies**: `uuid-creator` (UUIDv7), Flyway, Spring Security (already
  present), JJWT (already present)
- **API**: New endpoints `POST /api/v1/auth/register`, `/login`, `/logout`,
  `/refresh`
- **Shared module**: Relies on existing `AggregateRoot`, `Result<T,E>`,
  `JsendResponse`, `GlobalExceptionHandler` primitives

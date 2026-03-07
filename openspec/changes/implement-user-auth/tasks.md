# Tasks

## 1. Project Setup & Dependencies

- [x] 1.1 Add `com.github.f4b6a3:uuid-creator` dependency to `build.gradle.kts`
      for UUIDv7 generation
- [x] 1.2 Add `org.flywaydb:flyway-core` and
      `org.flywaydb:flyway-database-postgresql` dependencies
- [x] 1.3 Configure `application.properties`: datasource URL, JPA settings
      (`ddl-auto=validate`), Flyway, JWT secret, token expiry values
- [x] 1.4 Update `compose.yaml` database name/credentials to match
      `application.properties`

## 2. Database Migrations

- [x] 2.1 Create `V1__create_users.sql`: `users` table with `id` (uuid PK),
      `email` (unique), `password_hash`, `full_name`, `avatar_url`,
      `preferences` (jsonb), `created_at`, `updated_at`
- [x] 2.2 Create `V2__create_refresh_tokens.sql`: `refresh_tokens` table with
      `id`, `user_id` (FK → users), `token_hash` (varchar 255), `expires_at`,
      `revoked_at`, `created_at`; add index on `token_hash` and `user_id`

## 3. Error Codes

- [x] 3.1 Create `AuthErrorCode` enum with constants: `EMAIL_ALREADY_EXISTS`,
      `INVALID_CREDENTIALS`, `REFRESH_TOKEN_NOT_FOUND`, `REFRESH_TOKEN_EXPIRED`,
      `REFRESH_TOKEN_REVOKED`, `REFRESH_TOKEN_REUSE_DETECTED`, `USER_NOT_FOUND`
- [x] 3.2 Ensure `AuthErrorCode` integrates with the shared `ErrorCode` /
      `JsendResponse` pattern from the shared module

## 4. Domain Layer

- [x] 4.1 Create `Email` value object: validates format, case-normalizes to
      lowercase, implements `ValueObject`
- [x] 4.2 Create `HashedPassword` value object: wraps Argon2id hash string,
      implements `ValueObject`
- [x] 4.3 Create `FullName` value object: validates non-blank, max 255 chars,
      implements `ValueObject`
- [x] 4.4 Create `User` aggregate: extends `AggregateRoot<UUID>`, fields
      `email`, `hashedPassword`, `fullName`, `avatarUrl`, `preferences`,
      `createdAt`, `updatedAt`; factory method
      `User.register(email, hashedPassword, fullName)` generates UUIDv7 id
- [x] 4.5 Create `RefreshToken` aggregate: extends `AggregateRoot<UUID>`, fields
      `userId`, `tokenHash`, `expiresAt`, `revokedAt`, `createdAt`; factory
      method `RefreshToken.issue(userId, tokenHash, expiresAt)` generates UUIDv7
      id; method `isExpired()`, `isRevoked()`, `revoke()`
- [x] 4.6 Create `UserRepository` port interface:
      `findByEmail(Email): Optional<User>`, `findById(UUID): Optional<User>`,
      `save(User): User`, `existsByEmail(Email): boolean`
- [x] 4.7 Create `RefreshTokenRepository` port interface:
      `findByTokenHash(String): Optional<RefreshToken>`,
      `save(RefreshToken): RefreshToken`, `revokeAllByUserId(UUID): void`
- [x] 4.8 Create `PasswordHasher` port interface:
      `hash(String rawPassword): HashedPassword`,
      `verify(String rawPassword, HashedPassword hash): boolean`

## 5. Infrastructure — Persistence

- [x] 5.1 Create `UserJpaEntity` with `@Entity @Table("users")`, all columns
      mapped; `preferences` mapped with `@JdbcTypeCode(SqlTypes.JSON)` as
      `String`
- [x] 5.2 Create `RefreshTokenJpaEntity` with
      `@Entity @Table("refresh_tokens")`, all columns mapped; `@ManyToOne` or
      plain `userId` UUID field
- [x] 5.3 Create `UserJpaRepository` extending
      `JpaRepository<UserJpaEntity, UUID>` with `findByEmail(String)` and
      `existsByEmail(String)` query methods
- [x] 5.4 Create `RefreshTokenJpaRepository` extending
      `JpaRepository<RefreshTokenJpaEntity, UUID>` with
      `findByTokenHash(String)` and `updateRevokedAtByUserId(UUID, Instant)`
      methods
- [x] 5.5 Create `UserRepositoryAdapter` implementing domain `UserRepository`
      port; maps between `UserJpaEntity` and `User` aggregate
- [x] 5.6 Create `RefreshTokenRepositoryAdapter` implementing domain
      `RefreshTokenRepository` port; maps between `RefreshTokenJpaEntity` and
      `RefreshToken` aggregate

## 6. Infrastructure — Security

- [x] 6.1 Create `Argon2PasswordHasher` implementing domain `PasswordHasher`
      port using `Argon2PasswordEncoder` from Spring Security
- [x] 6.2 Create `JwtTokenProvider`:
      `generateAccessToken(UUID userId, String email): String`,
      `validateToken(String token): boolean`,
      `extractUserId(String token): UUID`; uses JJWT with HS256; reads secret
      and expiry from config
- [x] 6.3 Create `JwtAuthFilter` extending `OncePerRequestFilter`: extracts
      `Bearer` token from `Authorization` header, validates via
      `JwtTokenProvider`, sets `SecurityContextHolder`
- [x] 6.4 Create `SecurityConfig` (`@Configuration @EnableWebSecurity`):
      stateless session (`STATELESS`), disable CSRF, permit `/api/v1/auth/**`
      without auth, require auth for all other paths, register `JwtAuthFilter`
      before `UsernamePasswordAuthenticationFilter`

## 7. Application Layer — Use Cases

- [x] 7.1 Create `RegisterUserUseCase`: accepts
      `RegisterRequest(email, password, fullName)`; checks `existsByEmail`;
      hashes password; creates `User` aggregate; saves; returns
      `Result<RegisterResponse, AuthErrorCode>`
- [x] 7.2 Create `LoginUserUseCase`: accepts `LoginRequest(email, password)`;
      finds user by email; verifies password; generates JWT access token;
      generates raw refresh token (32 random bytes, Base64URL); SHA-256 hashes
      it; saves `RefreshToken`; returns `Result<LoginResponse, AuthErrorCode>`
- [x] 7.3 Create `RefreshTokenUseCase`: accepts
      `RefreshTokenRequest(refreshToken)`; SHA-256 hashes input; finds by hash;
      checks revoked (reuse detection → revoke all); checks expired; revokes old
      token; issues new token pair; returns
      `Result<LoginResponse, AuthErrorCode>`
- [x] 7.4 Create `LogoutUserUseCase`: accepts `LogoutRequest(refreshToken)`;
      SHA-256 hashes input; finds by hash; sets `revoked_at`; returns
      `Result<Void, AuthErrorCode>` (idempotent — already-revoked is success)
- [x] 7.5 Create DTOs: `RegisterRequest`, `RegisterResponse`, `LoginRequest`,
      `LoginResponse` (accessToken, refreshToken, expiresIn),
      `RefreshTokenRequest`, `LogoutRequest`

## 8. Presentation Layer

- [x] 8.1 Create `AuthController`
      (`@RestController @RequestMapping("/v1/auth")`): inject all 4 use cases
- [x] 8.2 Implement `POST /register`: validate `@Valid RegisterRequest`, call
      `RegisterUserUseCase`, map `Result` to `JsendResponse` (201 on success,
      409 on `EMAIL_ALREADY_EXISTS`, 400 on validation errors)
- [x] 8.3 Implement `POST /login`: validate `@Valid LoginRequest`, call
      `LoginUserUseCase`, map `Result` to `JsendResponse` (200 on success, 401
      on `INVALID_CREDENTIALS`)
- [x] 8.4 Implement `POST /refresh`: validate `@Valid RefreshTokenRequest`, call
      `RefreshTokenUseCase`, map `Result` to `JsendResponse` (200 on success,
      401 on token errors)
- [x] 8.5 Implement `POST /logout`: validate `@Valid LogoutRequest`, call
      `LogoutUserUseCase`, map `Result` to `JsendResponse` (200 on success, 401
      on errors)
- [x] 8.6 Complete `GlobalExceptionHandler` in shared module (or override in
      this service): map `MethodArgumentNotValidException` to
      `JsendResponse.fail` with `Violation` list; map unhandled exceptions to
      `JsendResponse.error`

## 9. Testing

- [x] 9.1 Unit test `RegisterUserUseCase`: duplicate email, successful
      registration, password hashing called
- [x] 9.2 Unit test `LoginUserUseCase`: wrong password, unknown email,
      successful login, refresh token hash stored
- [x] 9.3 Unit test `RefreshTokenUseCase`: expired token, revoked token (reuse
      detection), valid rotation
- [x] 9.4 Unit test `LogoutUserUseCase`: unknown token, already-revoked
      (idempotent), successful revocation
- [x] 9.5 Unit test `JwtTokenProvider`: token generation, validation, expiry,
      claims extraction
- [x] 9.6 Integration test `AuthController` (Spring Boot test slice with H2):
      full register → login → refresh → logout flow

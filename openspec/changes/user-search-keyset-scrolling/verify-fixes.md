# Verify Fixes Log

## [2026-03-12] Round 1 (from opsx-apply auto-verify)

### opsx-arch-verifier

- Fixed: [CRITICAL] Duplicate class declaration in `UserRepositoryAdapter.java`
  — rewrote file with single class definition
- Fixed: [CRITICAL] Domain port `UserRepository` imported `CursorEncoder.Cursor`
  from infrastructure layer — extracted `ScrollCursor` as a pure domain record
  in `shared/domain/ScrollCursor.java`; updated `UserRepository`,
  `UserRepositoryAdapter`, `SearchUsersUseCase`, and `CursorEncoder` to use
  `ScrollCursor`
- Fixed: [CRITICAL] HMAC truncation was 4 bytes (32-bit) — increased to 16 bytes
  (128-bit) in `CursorEncoder.java`
- Fixed: [WARNING] `SearchUsersUseCase.execute()` returned
  `CursorScrollResponse` directly instead of `Result<T,E>` — changed return type
  to `Result<CursorScrollResponse<UserResponse>, AuthErrorCode>`; updated
  `UserController.searchUsers()` to pattern-match on `Result`
- Fixed: [WARNING] `CursorPageResult.nextCursor` was a nullable String used as a
  boolean sentinel — changed to `boolean hasNext` field; updated
  `CursorPageResult`, `UserRepositoryAdapter.searchUsers()`, and
  `SearchUsersUseCase`
- Fixed: [WARNING] `app.cursor.secret` had a weak default fallback in
  `application.properties` — removed fallback so Spring fails fast if
  `CURSOR_SECRET` env var is not set

## [2026-03-13] Round 2 (from opsx-apply re-verify)

### opsx-arch-verifier

- Fixed: [WARNING] ILIKE query did not escape special characters (`%`, `_`, `\`)
  — added `escapeLike()` helper in `UserRepositoryAdapter` that escapes before
  passing to JPA query
- Fixed: [WARNING] Missing trigram indexes for ILIKE search on `email` and
  `username` — added `CREATE EXTENSION IF NOT EXISTS pg_trgm` and two GIN
  trigram indexes to `V7__add_users_keyset_index.sql`
- Skipped (pre-existing pattern): Application layer imports from
  `shared.infrastructure.web` — same pattern used by all other use cases
- Skipped (pre-existing pattern): `AuthErrorCode implements ErrorCode` from
  infrastructure — pre-existing domain design, not introduced by this change
- Skipped (intentional): `Result<T, AuthErrorCode>` always returns success —
  consistent with existing use case pattern; kept for API consistency

## [2026-03-13] Round 3 (from opsx-verify + opsx-apply fix)

### opsx-arch-verifier

- Fixed: [CRITICAL] `CursorTokenEncoder` (domain port) imported
  `CommonErrorCode` from `shared.infrastructure.web` — violated hexagonal
  architecture dependency rule. Created `shared/domain/CursorErrorCode.java`
  implementing `ErrorCode`; updated `CursorTokenEncoder.decode()` to return
  `Result<ScrollCursor, CursorErrorCode>`; updated `CursorEncoder`
  implementation and `UserController` to use `CursorErrorCode`
- Fixed: [CRITICAL] `InvalidCursorException` and
  `GlobalExceptionHandler.handleInvalidCursor()` were dead code after decode()
  was changed to return Result — deleted `InvalidCursorException.java`, removed
  `handleInvalidCursor()` method from `GlobalExceptionHandler`, updated
  `keyset-pagination/spec.md` scenarios 78 & 84 to reflect Result-based behavior
- Fixed: [CRITICAL] `SearchUsersUseCase` injected `CursorTokenEncoder` but never
  used it — removed field and constructor parameter; updated
  `SearchUsersUseCaseTest` constructor call accordingly

### opsx-test-verifier

- Fixed: [CRITICAL] No unit tests for `CursorEncoder` — created
  `shared/src/test/.../infrastructure/web/CursorEncoderTest.java` with 10 tests
  covering: encode determinism, Base64url format, decode round-trip, epoch
  precision, tampered signature, different secret, invalid Base64url, empty
  string, no colon, invalid UUID in payload
- Fixed: [CRITICAL] No unit tests for `CursorPageResult` factory methods —
  created `shared/src/test/.../domain/CursorPageResultTest.java` with 6 tests
  covering: `of()` fields, `hasNext` flag, defensive copy, immutability,
  `empty()` fields, `empty()` page size
- Fixed: [CRITICAL] No integration test for invalid `pageToken` → HTTP 400 —
  added `searchUsers_invalidPageToken_returns400WithInvalidCursorCode()` and
  `searchUsers_tamperedPageToken_returns400()` to `UserProfileIntegrationTest`
- Fixed: [CRITICAL] No integration tests for size defaults/clamping — added
  `searchUsers_sizeOmitted_defaultsTo20()` and
  `searchUsers_sizeGreaterThan100_clampsTo100()` to `UserProfileIntegrationTest`
- Fixed: [CRITICAL] No integration test for empty results — added
  `searchUsers_noMatchingQuery_returnsEmptyContentAndNullNextPageToken()`
- Fixed: [CRITICAL] No integration test for OR search logic — added
  `searchUsers_queryMatchesUsername_returnsUser()` and
  `searchUsers_queryMatchesEmailOrUsername_returnsBothUsers()`
- Fixed: [WARNING] `SearchUsersUseCaseTest` passed `null` for
  `cursorTokenEncoder` — removed the argument (constructor no longer accepts it
  after ARCH-3 fix)
- Fixed: [WARNING] No multi-page traversal test — added
  `searchUsers_multiPageTraversal_fetchesConsecutivePages()` registering 5 users
  and fetching two pages with size=2

## [2026-03-13] Round 4 (from opsx-verify re-verify)

### opsx-arch-verifier

- Fixed: [WARNING] `UserController.searchUsers()` used `instanceof` check +
  unsafe cast instead of project-standard `switch` pattern matching — refactored
  to use exhaustive `switch` expression; extracted `executeSearch()` private
  helper to enable early return on failure branch

### opsx-test-verifier

- Fixed: [CRITICAL] `SearchUsersRequest` fields `pageTokenRaw` and `queryRaw`
  were not bound to HTTP params `pageToken` and `query` — added
  `@RequestParam(value = "pageToken", required = false)` and
  `@RequestParam(value = "query", required = false)` annotations; this caused 3
  integration tests to fail (invalid cursor not rejected, query filter silently
  ignored) and 2 tests to pass as false positives

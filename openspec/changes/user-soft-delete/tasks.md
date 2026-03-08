# Tasks

## 1. Database Migration

- [x] 1.1 Create `V3__add_user_soft_delete.sql`: add
      `deleted_at TIMESTAMPTZ DEFAULT NULL` column to `users` table, drop
      `uq_users_email` constraint, create partial unique index
      `uq_active_users_email ON users(email) WHERE deleted_at IS NULL`
- [x] 1.2 Create `db/h2-migration/V3__add_user_soft_delete.sql`: H2-compatible
      version — add `deleted_at TIMESTAMP DEFAULT NULL`, drop old constraint,
      add standard unique constraint (no partial index support in H2)

## 2. Domain Model

- [x] 2.1 Add `Instant deletedAt` field to `User` aggregate
- [x] 2.2 Add `delete()` method: sets `deletedAt = Instant.now()` and updates
      `updatedAt`
- [ ] 2.3 Add `undelete()` method: sets `deletedAt = null` and updates
      `updatedAt`
- [x] 2.4 Add `isDeleted()` predicate: returns `deletedAt != null`
- [x] 2.5 Update `User.reconstitute()` static factory to accept and map
      `deletedAt`

## 3. Persistence Layer

- [x] 3.1 Add `@Column(name = "deleted_at") Instant deletedAt` to
      `UserJpaEntity`
- [x] 3.2 Update `UserRepositoryAdapter.toDomain()` to map `deletedAt`
- [x] 3.3 Update `UserRepositoryAdapter.toEntity()` to map `deletedAt`
- [x] 3.4 Add `findByEmailAndDeletedAtIsNull(String email)` to
      `UserJpaRepository` (replaces `findByEmail`)
- [x] 3.5 Add `existsByEmailAndDeletedAtIsNull(String email)` to
      `UserJpaRepository` (replaces `existsByEmail`)
- [x] 3.6 Add `findByIdAndDeletedAtIsNull(UUID id)` to `UserJpaRepository`
- [x] 3.7 Update `UserRepository` port interface: add `findActiveById(UUID id)`
      and `findActiveByEmail(Email email)` methods
- [x] 3.8 Update `UserRepositoryAdapter` to implement new port methods using the
      new JPA queries

## 4. Error Codes

- [x] 4.1 Add `USER_DELETED` to `AuthErrorCode` enum (used when deleted user
      attempts login or JWT check fails)
- [x] 4.2 Add `USER_ALREADY_DELETED` to `AuthErrorCode` enum (used in
      `DeleteAccountUseCase` guard)
- [ ] 4.3 Add `USER_NOT_DELETED` to `AuthErrorCode` enum (used in
      `UndeleteUserUseCase` guard)

## 5. Security Filter Update

- [x] 5.1 Update `JwtAuthFilter`: after extracting `userId` from JWT, call
      `userRepository.findActiveById(userId)`; if empty (user not found or
      deleted), reject with `401 Unauthorized`

## 6. Existing Use Case Updates

- [x] 6.1 Update `LoginUserUseCase`: use `findActiveByEmail` instead of
      `findByEmail`; return `USER_DELETED` error if user exists but is deleted
- [x] 6.2 Update `RegisterUserUseCase`: use `existsByEmailAndDeletedAtIsNull` so
      deleted users' emails are available for re-registration

## 7. New Use Cases

- [x] 7.1 Create `DeleteAccountUseCase`: find user by id via `findActiveById`,
      guard `USER_ALREADY_DELETED` if already deleted, call `user.delete()`,
      save user, call `refreshTokenRepository.revokeAllByUserId(userId)`, return
      `Result.success()`
- [ ] 7.2 Create `UndeleteUserUseCase`: accept
      `UndeleteUserRequest(UUID userId)`, find user by id (including deleted —
      use raw `findById`), guard `USER_NOT_FOUND` if absent, guard
      `USER_NOT_DELETED` if `deletedAt` is null, call `user.undelete()`, save
      user, return `Result.success()`
- [ ] 7.3 Create `UndeleteUserRequest` DTO record with `@NotNull UUID userId`

## 8. Controller

- [x] 8.1 Add `DELETE /{version}/auth/me` to `AuthController`: extract `userId`
      from `SecurityContextHolder`, call `DeleteAccountUseCase`, return
      `204 No Content` on success or `409 Conflict` on `USER_ALREADY_DELETED`
- [ ] 8.2 Add `POST /{version}/auth/me:undelete` to `AuthController`: accept
      `@RequestBody UndeleteUserRequest`, call `UndeleteUserUseCase`, return
      `204 No Content` on success, `404` on `USER_NOT_FOUND`, `409` on
      `USER_NOT_DELETED`

## 9. Tests

- [x] 9.1 Unit test `DeleteAccountUseCase`: success path, `USER_NOT_FOUND`,
      `USER_ALREADY_DELETED`, verify token revocation is called
- [ ] 9.2 Unit test `UndeleteUserUseCase`: success path, `USER_NOT_FOUND`,
      `USER_NOT_DELETED`
- [x] 9.3 Integration test `DELETE /auth/me`: valid JWT → 204, deleted user JWT
      → 401
- [ ] 9.4 Integration test `POST /auth/me:undelete`: restore deleted user → 204,
      non-deleted user → 409, unknown user → 404
- [x] 9.5 Integration test login with deleted user: correct credentials → 401
      `USER_DELETED`
- [x] 9.6 Integration test registration with previously-deleted email: should
      succeed (201)

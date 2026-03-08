# Tasks

## 1. Domain — DTOs (Output)

- [x] 1.1 Create `UserPreferencesResponse` record in `application/dto/`: fields
      `String theme`, `boolean defaultMic`, `boolean defaultCamera`
- [x] 1.2 Create `UserResponse` record in `application/dto/`: fields `UUID id`,
      `String email`, `String fullName`, `@Nullable String avatarUrl`,
      `String authProvider`, `UserPreferencesResponse preferences`,
      `Instant createdAt`, `Instant updatedAt`

## 2. Application — Update UserPreferencesParser

- [x] 2.1 Add `parseAsResponse(Optional<String>)` method to
      `UserPreferencesParser` that returns `UserPreferencesResponse` (reuse
      existing parse logic, fall back to defaults)

## 3. Domain — Event

- [x] 3.1 Create `UserUpdatedEvent` record in `domain/event/`: fields
      `UUID eventId`, `UUID aggregateId`, `String email`, `String fullName`,
      `@Nullable String avatarUrl`, `String authProvider`, `Instant updatedAt`;
      implement `PublishableEvent`;
      `eventType = "io.github.phunguy65.zms.user.updated.v1"`;
      `topic = "user-management.user.updated"`
- [x] 3.2 Add unit test for `UserUpdatedEvent` in `UserDomainEventsTest`

## 4. Domain — User Aggregate

- [x] 4.1 Add
      `updateProfile(@Nullable FullName fullName, @Nullable String avatarUrl)`
      method to `User`: apply non-null args, update `updatedAt`, call
      `registerEvent(new     UserUpdatedEvent(...))` with current state after
      mutation
- [x] 4.2 Add unit test for `User.updateProfile()` verifying field mutation and
      event registration

## 5. Domain — Repository Port

- [x] 5.1 Create `UserFilter` record in `domain/port/` (or `domain/model/`):
      fields `@Nullable String emailContains`, `@Nullable String authProvider`
- [x] 5.2 Add `findActiveUsers(int page, int size, UserFilter filter)` returning
      `PageResult<User>` to `UserRepository` port interface

## 6. Infrastructure — Persistence

- [x] 6.1 Add JPQL query method to `UserJpaRepository`:
      `java     @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL " +            "AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +            "AND (:provider IS NULL OR u.authProvider = :provider) " +            "ORDER BY u.createdAt DESC")     Slice<UserJpaEntity> findActiveFiltered(         @Param("email") String email,         @Param("provider") String provider,         Pageable pageable);     `
- [x] 6.2 Implement `findActiveUsers(int page, int size, UserFilter filter)` in
      `UserRepositoryAdapter`: build `PageRequest`, call `findActiveFiltered`,
      map to `PageResult<User>` via `PageResult.of()`

## 7. Application — DTOs (Input)

- [x] 7.1 Create `PatchUserRequest` record in `application/dto/`: fields
      `JsonNullable<@Size(max=255) @NotBlank String> fullName`,
      `JsonNullable<@Size(max=2048) String> avatarUrl`,
      `JsonNullable<Map<String, Object>> preferences`; initialize all to
      `JsonNullable.undefined()` in compact constructor default
- [x] 7.2 Create `PatchPreferencesRequest` record in `application/dto/`: fields
      `JsonNullable<@Pattern(regexp="dark|light|system") String> theme`,
      `JsonNullable<Boolean> defaultMic`, `JsonNullable<Boolean> defaultCamera`;
      initialize all to `JsonNullable.undefined()`

## 8. Application — Use Cases

- [x] 8.1 Create `GetUserUseCase` in `application/usecase/`: accepts
      `UUID userId`, calls `userRepository.findActiveById(userId)`, maps to
      `UserResponse` using `UserPreferencesParser.parseAsResponse()`; returns
      `Result<UserResponse, AuthErrorCode>`
- [x] 8.2 Create `GetUsersSliceUseCase` in `application/usecase/`: accepts
      `int page`, `int     size`, `UserFilter filter`; calls
      `userRepository.findActiveUsers()`; maps `PageResult<User>` to
      `SliceHttpResponse<UserResponse>`; returns
      `Result<SliceHttpResponse<UserResponse>, AuthErrorCode>`
- [x] 8.3 Create `PatchUpdateUserUseCase` in `application/usecase/`: accepts
      `UUID userId`, `PatchUserRequest dto`; loads user via `findActiveById`;
      applies `fullName` and `avatarUrl` patches via `user.updateProfile()` only
      when `isPresent()`; applies `preferences` replace via
      `user.updatePreferences()` when `preferences.isPresent()`; saves;
      publishes domain events; returns `Result<UserResponse, AuthErrorCode>`
- [x] 8.4 Create `PatchUpdatePreferencesUseCase` in `application/usecase/`:
      accepts `UUID     userId`, `PatchPreferencesRequest dto`; loads user;
      parses current preferences via `UserPreferencesParser.parseAsResponse()`;
      merges present fields from dto; serializes back to JSON; calls
      `user.updatePreferences(json)`; saves; returns
      `Result<UserPreferencesResponse, AuthErrorCode>`

## 9. Application — Unit Tests

- [x] 9.1 Unit test `GetUserUseCase`: user found → returns `UserResponse`; user
      not found → `USER_NOT_FOUND`
- [x] 9.2 Unit test `GetUsersSliceUseCase`: returns mapped slice; empty result
- [x] 9.3 Unit test `PatchUpdateUserUseCase`: absent fields not applied; present
      fields applied; null avatarUrl clears field; preferences replaced when key
      present; preferences unchanged when key absent; empty body is no-op (no
      event); `UserUpdatedEvent` published on change
- [x] 9.4 Unit test `PatchUpdatePreferencesUseCase`: absent fields not merged;
      present fields merged; invalid theme rejected at validation layer; user
      not found → `USER_NOT_FOUND`

## 10. Presentation — Controller

- [x] 10.1 Add `GET /{version}/users/me` to `UserController` (version `"1.0"`):
      call `getUserUseCase.execute(currentUserId())`; map `Result` to `200 OK`
      or `404`
- [x] 10.2 Add `GET /{version}/users/{id}` to `UserController` (version
      `"1.0"`): call `getUserUseCase.execute(id)`; map `Result` to `200 OK` or
      `404`
- [x] 10.3 Add `GET /{version}/users` to `UserController` (version `"1.0"`):
      accept `@RequestParam(required=false) String email`,
      `@RequestParam(required=false) String      authProvider`,
      `@RequestParam(defaultValue="0") int page`,
      `@RequestParam(defaultValue="20") int size`; call `getUsersSliceUseCase`;
      return `200 OK`
- [x] 10.4 Add `PATCH /{version}/users/me` to `UserController` (version
      `"1.0"`): accept `@Valid @RequestBody PatchUserRequest`; call
      `patchUpdateUserUseCase`; return `200 OK` or `404`
- [x] 10.5 Add `PATCH /{version}/users/me/preferences` to `UserController`
      (version `"1.0"`): accept `@Valid @RequestBody PatchPreferencesRequest`;
      call `patchUpdatePreferencesUseCase`; return `200 OK` or `404`

## 11. Integration Tests

- [x] 11.1 Add `@SpringBootTest` integration test for `GET /api/v1/users/me`:
      authenticated → 200 with `UserResponse`; unauthenticated → 401
- [x] 11.2 Add integration test for `GET /api/v1/users/{id}`: found → 200; not
      found → 404; unauthenticated → 401
- [x] 11.3 Add integration test for `GET /api/v1/users`: default pagination →
      200 with slice; email filter; authProvider filter; unauthenticated → 401
- [x] 11.4 Add integration test for `PATCH /api/v1/users/me`: partial update
      applied; empty body no-op; blank fullName → 400; unauthenticated → 401
- [x] 11.5 Add integration test for `PATCH /api/v1/users/me/preferences`:
      partial preference update; invalid theme → 400; unauthenticated → 401

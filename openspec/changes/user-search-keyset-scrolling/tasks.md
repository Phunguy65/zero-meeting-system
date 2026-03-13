# Tasks

## 1. Shared Module — Keyset Pagination Abstractions

- [x] 1.1 Add `ScrollParams` interface to `shared/domain/` (mirrors
      `SliceParams`: `pageSize()`, `pageToken()`, `query()`)
- [x] 1.2 Add `CursorPageResult<T>` record to `shared/domain/` (fields: `items`,
      `pageSize`, `nextCursor`; factory `of()` and `empty()`)
- [x] 1.3 Add `CursorScrollResponse<T>` record to `shared/infrastructure/web/`
      (fields: `content`, `size`, `nextPageToken`)
- [x] 1.4 Add `ScrollRequest` record to `shared/infrastructure/web/`
      implementing `ScrollParams` (default size 20, max 100, no filters)
- [x] 1.5 Add `InvalidCursorException` to `shared/infrastructure/web/` (runtime
      exception, maps to HTTP 400)
- [x] 1.6 Add `CursorEncoder` to `shared/infrastructure/web/` — HMAC-SHA256
      signed Base64url encode/decode of `(createdAt, id)` cursor; reads secret
      from `app.cursor.secret` config property
- [x] 1.7 Register `InvalidCursorException` → HTTP 400 handler in
      `GlobalExceptionHandler`

## 2. User-Management — Remove Legacy Code

- [x] 2.1 Delete `application/dto/GetUsersRequest.java`
- [x] 2.2 Delete `application/usecase/GetUsersSliceUseCase.java`
- [x] 2.3 Delete `domain/port/UserFilter.java`
- [x] 2.4 Remove `findActiveUsers()` method from
      `domain/port/UserRepository.java`
- [x] 2.5 Remove `findActiveUsers()` implementation from
      `infrastructure/persistence/UserRepositoryAdapter.java`
- [x] 2.6 Remove `findActiveFiltered()` query from
      `infrastructure/persistence/UserJpaRepository.java`

## 3. User-Management — Domain Layer

- [x] 3.1 Add `UserScrollFilter` record to `domain/port/` (field: `query` —
      nullable String)
- [x] 3.2 Add `searchUsers(Cursor cursor, int size, UserScrollFilter filter)`
      method to `domain/port/UserRepository.java` returning
      `CursorPageResult<User>`

## 4. User-Management — Application Layer

- [x] 4.1 Add `SearchUsersRequest` record to `application/dto/` implementing
      `ScrollParams` (fields: `size`, `pageToken`, `query`; validation: size
      [1,100])
- [x] 4.2 Add `SearchUsersUseCase` to `application/usecase/` — decode
      `pageToken` via `CursorEncoder`, call `userRepository.searchUsers()`, map
      to `CursorScrollResponse<UserResponse>`, encode `nextPageToken`

## 5. User-Management — Infrastructure Layer

- [x] 5.1 Implement `searchUsers()` in `UserRepositoryAdapter` — build keyset
      native SQL with optional `(created_at, id) < (?, ?)` clause and optional
      ILIKE filter; fetch `size + 1` rows; return `CursorPageResult`
- [x] 5.2 Add keyset native query to `UserJpaRepository` —
      `SELECT * FROM users WHERE deleted_at IS NULL [AND (created_at, id) < (?, ?)] [AND (email ILIKE ? OR username ILIKE ?)] ORDER BY created_at DESC, id DESC LIMIT ?`
- [x] 5.3 Add Flyway migration `V?__add_users_keyset_index.sql` —
      `CREATE INDEX idx_users_keyset ON users (created_at DESC, id DESC) WHERE deleted_at IS NULL`

## 6. User-Management — Presentation Layer

- [x] 6.1 Replace `GET /{version}/users` mapping in `UserController` with
      `GET /{version}/users:search` bound to `SearchUsersRequest`
- [x] 6.2 Wire `SearchUsersUseCase` into controller; remove
      `GetUsersSliceUseCase` dependency

## 7. Configuration

- [x] 7.1 Add `app.cursor.secret` property to `application.properties`
      (placeholder value) and `application-test.properties` (fixed test value)

# Tasks

## 1. Database Migration

- [x] 1.1 Tạo `V6__add_username.sql` (PostgreSQL):
      `ALTER TABLE users ADD COLUMN username VARCHAR(30)`, backfill
      `UPDATE users SET username = 'user_' || SUBSTRING(id::text, 1, 8) WHERE username IS NULL`,
      tạo partial unique index
      `uq_active_users_username ON users(username) WHERE deleted_at IS NULL`
- [x] 1.2 Tạo H2-compatible migration `V6__add_username.sql` trong
      `src/test/resources/db/h2-migration/`: chỉ `ALTER TABLE` và `UPDATE`, bỏ
      qua partial index (H2 không support)

## 2. Domain Layer

- [x] 2.1 Tạo `Username.java` Value Object: record implementing `ValueObject`,
      validate regex `^[a-zA-Z0-9_-]{3,30}$` trong compact constructor, static
      factory `Username.of(String)`, static factory
      `Username.generateForGoogle()` trả về `"user_" + 8 hex chars từ UUID`
- [x] 2.2 Cập nhật `User.java`: thêm field
      `private @Nullable Username username`, cập nhật constructor, thêm getter
      `Optional<Username> getUsername()`
- [x] 2.3 Cập nhật `User.register()`: thêm tham số `Username username`, assign
      field
- [x] 2.4 Cập nhật `User.registerWithGoogle()`: thêm tham số
      `Username username`, assign field
- [x] 2.5 Cập nhật `User.reconstitute()`: thêm tham số
      `@Nullable String username`, wrap với `Username.of()` nếu non-null
- [x] 2.6 Cập nhật `User.updateProfile()` (overloads): thêm tham số
      `@Nullable Username newUsername`, apply nếu non-null
- [x] 2.7 Thêm `USERNAME_ALREADY_EXISTS` vào `AuthErrorCode` enum (sau
      `EMAIL_ALREADY_EXISTS`)
- [x] 2.8 Cập nhật `UserRegisteredEvent`: thêm field `@Nullable String username`
- [x] 2.9 Cập nhật `UserUpdatedEvent`: thêm field `@Nullable String username`
- [x] 2.10 Cập nhật `UserRepository` port: thêm
      `boolean existsActiveByUsername(Username username)` và
      `Optional<User> findActiveByUsername(Username username)`

## 3. Infrastructure Layer

- [x] 3.1 Cập nhật `UserJpaEntity`: thêm
      `@Column(length = 30) private @Nullable String username`, cập nhật
      constructor, thêm getter
- [x] 3.2 Cập nhật `UserJpaRepository`: thêm
      `boolean existsByUsernameAndDeletedAtIsNull(String username)` và
      `Optional<UserJpaEntity> findByUsernameAndDeletedAtIsNull(String username)`
- [x] 3.3 Cập nhật `UserRepositoryAdapter.toDomain()`: pass
      `entity.getUsername()` vào `User.reconstitute()`
- [x] 3.4 Cập nhật `UserRepositoryAdapter.toEntity()`: set
      `entity.setUsername(user.getUsername().map(Username::value).orElse(null))`
- [x] 3.5 Implement `existsActiveByUsername()` và `findActiveByUsername()` trong
      `UserRepositoryAdapter` (delegate tới JPA methods)

## 4. Application Layer — Registration

- [x] 4.1 Cập nhật `RegisterRequest`: thêm
      `@NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String username`
- [x] 4.2 Cập nhật `RegisterUserUseCase`: tạo `Username.of(request.username())`,
      check `existsActiveByUsername()` →
      `Result.failure(USERNAME_ALREADY_EXISTS)`, pass username vào
      `User.register()`
- [x] 4.3 Cập nhật `RegisterResponse`: thêm field `String username`
- [x] 4.4 Cập nhật `RegisterUserUseCase.toResponse()` (hoặc mapping): include
      `user.getUsername().map(Username::value).orElse(null)`

## 5. Application Layer — Google OAuth

- [x] 5.1 Cập nhật `LoginWithGoogleUseCase`: thêm retry loop gọi
      `Username.generateForGoogle()` với check
      `existsActiveByUsername(candidate)` trước khi gọi
      `User.registerWithGoogle()`
- [x] 5.2 Pass generated `Username` vào `User.registerWithGoogle()`

## 6. Application Layer — Patch Update

- [x] 6.1 Cập nhật `PatchUserRequest`: thêm
      `JsonNullable<@Size(min = 3, max = 30) @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String> username`
- [x] 6.2 Cập nhật `PatchUpdateUserUseCase`: thêm
      `|| dto.username().isPresent()` vào `anyChange` check, thêm uniqueness
      check cho username mới (skip nếu same as current), call
      `user.updateProfile()` với username
- [x] 6.3 Cập nhật `PatchUpdateUserUseCase.toResponse()`: include username

## 7. Application Layer — Query

- [x] 7.1 Cập nhật `UserResponse`: thêm field `@Nullable String username`
- [x] 7.2 Cập nhật `GetUserUseCase.toResponse()`: include
      `user.getUsername().map(Username::value).orElse(null)`
- [x] 7.3 Cập nhật `GetUsersSliceUseCase.toResponse()`: include username

## 8. Tests

- [x] 8.1 Unit test `Username` value object: valid cases, invalid format, too
      short, too long
- [x] 8.2 Unit test `RegisterUserUseCase`: username required, duplicate username
      → `USERNAME_ALREADY_EXISTS`, success path includes username
- [x] 8.3 Unit test `LoginWithGoogleUseCase`: new Google user gets
      auto-generated username, collision retry logic
- [x] 8.4 Unit test `PatchUpdateUserUseCase`: update username success, duplicate
      username → `USERNAME_ALREADY_EXISTS`, same username is idempotent
- [x] 8.5 Integration test `AuthController` (register): missing username → 400,
      duplicate username → 409, success → 201 with username
- [x] 8.6 Integration test `UserController` (patch): update username success,
      duplicate → 409
- [x] 8.7 Integration test `UserController` (get/slice): response includes
      username field

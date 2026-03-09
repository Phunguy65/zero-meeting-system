# Context

`user-management` service hiện tại không có field `username`. User chỉ được định
danh bằng `email` (private) và `id` (UUID — không thân thiện). Cần thêm
`username` như một định danh công khai, unique, human-readable.

Service dùng Hexagonal Architecture với DDD: domain aggregate `User`, value
objects, domain events qua outbox → Kafka. Soft delete đã được implement (V3
migration) với partial unique index trên `email WHERE deleted_at IS NULL`.

## Goals / Non-Goals

**Goals:**

- Thêm `username` vào User aggregate với validation đầy đủ
- Bắt buộc khi đăng ký qua email; tự generate tại domain layer
  (`Username.generateForGoogle()`) cho Google OAuth
- Cho phép cập nhật qua `PATCH /users/me` với uniqueness check
- Expose `username` trong tất cả user response DTOs
- Propagate `username` qua domain events (`UserRegisteredEvent`,
  `UserUpdatedEvent`)
- DB migration với partial unique index (soft-delete aware), backfill existing
  users

**Non-Goals:**

- Username history / audit log
- Username reservation / squatting protection
- Case-insensitive uniqueness (scope hiện tại: case-sensitive)
- Public profile page theo username (feature riêng)

## Decisions

### D1: Username generation cho Google OAuth — Domain model, không phải Application layer

**Quyết định**: Logic tạo candidate username (`"user_" + uuid(0..7)`) đặt trong
`Username.generateForGoogle()` (static factory trên domain Value Object).
Application layer (`LoginWithGoogleUseCase`) chỉ gọi method này và thực hiện
retry loop với `existsActiveByUsername()`.

**Lý do**: Generation logic là domain knowledge — format `"user_" + 8 hex chars`
là business rule, không phải infrastructure concern. Đặt trong `Username` giúp:

- Testable độc lập (unit test trên VO, không cần mock)
- Tái sử dụng nếu có thêm OAuth provider sau này
- Consistent với nguyên tắc "domain logic trong domain layer"

**Alternatives considered**:

- Application layer generate: Logic bị leak ra ngoài domain, khó test độc lập
- DB trigger: Không testable, coupling với DB, không thể enforce business
  uniqueness rule
- DB DEFAULT: Không thể check uniqueness trước khi insert

**Pattern**:

```
// Domain layer (Username.java):
public static Username generateForGoogle() {
    String uuid = UUID.randomUUID().toString().replace("-", "");
    return new Username("user_" + uuid.substring(0, 8));
}

// Application layer (LoginWithGoogleUseCase):
Username candidate = Username.generateForGoogle();
while (userRepository.existsActiveByUsername(candidate)) {
    candidate = Username.generateForGoogle();  // retry (collision cực hiếm)
}
User.registerWithGoogle(..., candidate)
```

---

### D2: Partial unique index — consistent với email pattern (V3)

**Quyết định**: Không dùng `UNIQUE` constraint thông thường. Dùng partial index:

```sql
CREATE UNIQUE INDEX uq_active_users_username
    ON users(username)
    WHERE deleted_at IS NULL;
```

**Lý do**: Soft-deleted users không nên block username reuse — consistent với
cách email được xử lý ở V3. Một user bị xóa và user mới có thể có cùng username.

**H2 (test)**: H2 không support partial index. Migration H2 sẽ bỏ qua constraint
này (precedent đã có ở V3 H2 migration).

---

### D3: Backfill existing users — trong Flyway migration

**Quyết định**: Backfill bằng `UPDATE` trong `V6__add_username.sql`:

```sql
UPDATE users
SET username = 'user_' || SUBSTRING(id::text, 1, 8)
WHERE username IS NULL;
```

**Lý do**: Đơn giản, atomic với schema change, không cần code riêng. UUIDv7 đảm
bảo substring đủ unique cho backfill. Không cần uniqueness check vì `id` là
unique — substring collision cực kỳ hiếm và acceptable cho backfill.

**Alternatives considered**:

- Application startup migration: Phức tạp hơn, không atomic
- Manual script: Dễ bị bỏ sót

---

### D4: Username là mutable (có thể update)

**Quyết định**: Username có thể thay đổi qua `PATCH /users/me`.

**Lý do**: Yêu cầu từ product. Uniqueness check trong `PatchUpdateUserUseCase`
trước khi apply.

**Lưu ý**: `UserUpdatedEvent` sẽ carry `username` mới — consumers downstream cần
handle.

---

### D5: Username Value Object — validate tại domain layer

**Quyết định**: Tạo `Username` record implementing `ValueObject`, validate trong
compact constructor.

**Pattern** (consistent với `Email`, `FullName`):

- Regex: `^[a-zA-Z0-9_-]{3,30}$`
- Throws `IllegalArgumentException` nếu invalid
- DTO layer dùng Jakarta Bean Validation annotations làm first-pass filter

---

### D6: `USERNAME_ALREADY_EXISTS` error code

**Quyết định**: Thêm vào `AuthErrorCode` enum, sau `EMAIL_ALREADY_EXISTS`.

**HTTP mapping**: `409 Conflict` (consistent với `EMAIL_ALREADY_EXISTS`).

## Risks / Trade-offs

**[Risk] Username collision khi generate cho Google OAuth** → Mitigation: Retry
loop trong use case. Collision probability với 8 hex chars (~4 tỷ combinations)
là cực thấp. Nếu cần, tăng lên 12 chars.

**[Risk] Backfill `SUBSTRING(id::text, 1, 8)` có thể collision** → Mitigation:
Acceptable cho backfill vì đây là generated placeholder, không phải user-chosen.
Nếu collision xảy ra, migration sẽ fail và cần manual intervention — nhưng xác
suất rất thấp với UUIDv7.

**[Risk] Breaking change trên `POST /register`** → Mitigation: Document rõ trong
API changelog. Clients (Android, Web) cần update trước khi deploy.

**[Risk] H2 test environment không có uniqueness constraint** → Mitigation: Đã
có precedent (V3). Unit tests cho uniqueness logic dùng mock repository, không
phụ thuộc DB constraint.

**[Trade-off] Case-sensitive username** → Đơn giản hơn nhưng `JohnDoe` và
`johndoe` có thể tồn tại song song. Acceptable cho scope hiện tại; có thể thêm
case-insensitive index sau nếu cần.

## Migration Plan

1. Deploy migration `V6__add_username.sql` (Flyway auto-run on startup):
    - `ALTER TABLE users ADD COLUMN username VARCHAR(30)`
    - `UPDATE users SET username = 'user_' || SUBSTRING(id::text, 1, 8)`
      (backfill)
    - `CREATE UNIQUE INDEX uq_active_users_username ON users(username) WHERE deleted_at IS NULL`
2. Deploy service với code changes
3. Clients update để gửi `username` khi đăng ký

**Rollback**: Drop column `username` và index. Không có data loss vì column mới.

## Open Questions

- Case-insensitive uniqueness có cần không? (Hiện tại: case-sensitive)
- Có cần expose endpoint `GET /users?username={username}` để lookup theo
  username không?

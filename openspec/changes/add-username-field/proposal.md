# Why

User profiles hiện tại thiếu `username` — một định danh duy nhất, con người có
thể đọc được, dùng để nhận diện người dùng trong hệ thống (mention, hiển thị
profile, tìm kiếm). Thêm `username` giúp tách biệt định danh công khai khỏi
`email` (thông tin riêng tư) và là nền tảng cho các tính năng xã hội sau này.

## What Changes

- **Thêm field `username`** vào User aggregate: bắt buộc khi đăng ký qua email,
  tự generate cho Google OAuth
- **Đăng ký (email)**: `POST /api/v1/auth/register` yêu cầu thêm field
  `username` (bắt buộc, unique)
- **Google OAuth**: `username` được tự generate tại application layer
  (`"user_" + uuid(0..7)`) khi tạo user mới — không phải tại DB
- **Patch update**: `PATCH /api/v1/users/me` cho phép cập nhật `username`
  (optional, unique check)
- **Get/Slice**: Tất cả response trả về user data đều bao gồm `username`
- **Domain events**: `UserRegisteredEvent` và `UserUpdatedEvent` bổ sung field
  `username`
- **Database**: Thêm cột `username VARCHAR(30)`, partial unique index
  `WHERE deleted_at IS NULL` (consistent với pattern email ở V3), backfill
  existing users tại application layer khi service khởi động hoặc qua migration
  script

## Capabilities

### New Capabilities

- `user-username`: Quản lý username người dùng — đăng ký, cập nhật, validation,
  uniqueness enforcement, và expose qua API responses và domain events

### Modified Capabilities

<!-- Không có capability hiện tại nào thay đổi requirements ở spec level -->

## Impact

**Code**:

- `services/user-management/` — 16 files thay đổi (1 Value Object mới, 1
  migration mới, 14 updates)
- Layers: Domain → Application → Infrastructure → Database

**APIs**:

- `POST /api/v1/auth/register` — **BREAKING**: thêm required field `username`
- `PATCH /api/v1/users/me` — non-breaking: thêm optional field `username`
- `GET /api/v1/users/me`, `GET /api/v1/users/{id}`, `GET /api/v1/users` —
  non-breaking: response thêm field `username`

**Events (Kafka)**:

- `UserRegisteredEvent` — thêm field `username` (consumers cần update nếu dùng
  field này)
- `UserUpdatedEvent` — thêm field `username`

**Database**:

- Thêm cột `username` với partial unique index (soft-delete aware)
- Backfill existing rows với generated username

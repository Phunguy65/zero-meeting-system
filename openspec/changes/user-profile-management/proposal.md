# Why

The user-management service currently exposes only auth-oriented endpoints
(register, login, preferences PUT/GET). There is no way to partially update a
user's profile fields, retrieve a single user by ID, or list users with
filtering — capabilities required by both the Android and Web clients as the
product grows. Additionally, downstream services (meeting-management,
chat-management) need a reliable event stream to keep their local user
projections in sync when profile data changes.

## What Changes

- Add `PATCH /api/v1/users/me` — partial update of `fullName` and `avatarUrl`
  using `JsonNullable<T>`; `preferences` key replaces the entire preferences
  object if present
- Add `PATCH /api/v1/users/me/preferences` — partial update of individual
  preference fields (`theme`, `defaultMic`, `defaultCamera`) using
  `JsonNullable<T>`
- Add `GET /api/v1/users/me` — retrieve the authenticated user's full profile
- Add `GET /api/v1/users/{id}` — retrieve any user by ID (authenticated users
  only, no admin role)
- Add `GET /api/v1/users` — paginated slice of active users with optional
  filtering by email substring and `authProvider`
- Introduce `UserUpdatedEvent` (ECST fat event) published to Kafka on every
  successful profile patch, carrying `fullName`, `avatarUrl`, and `authProvider`
  so downstream services can sync without calling back
- Introduce `UserPreferencesResponse` DTO (output-only) to replace the misuse of
  `UserPreferencesRequest` as a response type in `UserResponse`

## Capabilities

### New Capabilities

- `user-profile-patch`: Partial update of user profile info (fullName,
  avatarUrl, preferences replace) and individual preference fields via
  `JsonNullable<T>`; fires `UserUpdatedEvent`
- `user-profile-query`: Retrieve a single user by ID or self (`/me`), and list
  active users as a `Slice` with email/authProvider filtering

### Modified Capabilities

<!-- No existing specs to modify — openspec/specs/ is currently empty -->

## Impact

- **Domain**: `User` aggregate gains `updateProfile()` method; new
  `UserUpdatedEvent` domain event (ECST)
- **Application**: Four new use cases; new `UserResponse`,
  `UserPreferencesResponse`, `PatchUserRequest`, `PatchPreferencesRequest` DTOs
- **Infrastructure**: `UserRepository` port gains
  `findActiveUsers(page, size, filter)`; `UserJpaRepository` gains a filtered
  `Slice` query; `UserRepositoryAdapter` implements the new port method
- **Presentation**: `UserController` extended with five new endpoints
- **Events**: New Kafka topic `user-management.user.updated` (CloudEvents 1.0)
- **No new dependencies** — `jackson-databind-nullable` already on classpath;
  `PageResult` and `SliceHttpResponse` already in shared module

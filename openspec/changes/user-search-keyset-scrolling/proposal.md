# Why

The current `GET /v1/users` endpoint uses offset-based pagination which degrades
in performance as the dataset grows (OFFSET scans and discards rows). It also
lacks search capability. Replacing it with a keyset-scrolling search endpoint
provides stable, efficient cursor-based traversal and username/email search in a
single, clean API.

## What Changes

- **BREAKING** Remove `GET /v1/users` endpoint (offset-based pagination)
- **BREAKING** Remove `GetUsersRequest`, `GetUsersSliceUseCase`, `UserFilter`
  from user-management service
- Add `GET /v1/users:search` endpoint with keyset scrolling and `query` search
  param
- Add reusable keyset pagination abstractions to the shared module
  (`ScrollParams`, `CursorPageResult`, `CursorScrollResponse`, `ScrollRequest`,
  `CursorEncoder`)
- Response shape changes from `{content, page, size, hasNext, hasPrevious}` to
  `{content, size, nextPageToken}`

## Capabilities

### New Capabilities

- `user-search-scroll`: Cursor-based user search endpoint —
  `GET /v1/users:search` with `query` (OR search on username + email), `size`,
  and `pageToken` params; returns `CursorScrollResponse` with HMAC-signed
  `nextPageToken`
- `keyset-pagination`: Reusable shared-module abstractions for keyset/cursor
  pagination — `ScrollParams` interface, `CursorPageResult<T>` domain record,
  `CursorScrollResponse<T>` HTTP envelope, `ScrollRequest` generic DTO,
  `CursorEncoder` HMAC-SHA256 utility

### Modified Capabilities

## Impact

- **Removed**: `GetUsersSliceUseCase`, `GetUsersRequest`, `UserFilter`,
  `UserRepository#findActiveUsers()`
- **Modified**: `UserRepository` port (new `searchUsers()` method),
  `UserRepositoryAdapter`, `UserJpaRepository`, `UserController`
- **Added to shared module**: `ScrollParams`, `CursorPageResult`,
  `CursorScrollResponse`, `ScrollRequest`, `CursorEncoder`
- **New DB migration**: composite index on
  `users(created_at DESC, id DESC) WHERE deleted_at IS NULL`
- **API consumers**: any client calling `GET /v1/users` must migrate to
  `GET /v1/users:search`

# Context

The user-management service currently exposes `GET /v1/users` using offset-based
pagination (`PageResult`, `SliceHttpResponse`). As the `users` table grows,
`OFFSET` queries degrade because the database must scan and discard all
preceding rows. The endpoint also has no search capability. This design replaces
it with a keyset-scrolling search endpoint and introduces reusable cursor
pagination abstractions in the shared module.

Current state:

- `GetUsersSliceUseCase` →
  `UserRepository#findActiveUsers(page, size, UserFilter)` → native SQL with
  `OFFSET`
- `SliceHttpResponse` returns `{content, page, size, hasNext, hasPrevious}`
- No search on username or email

## Goals / Non-Goals

**Goals:**

- Replace offset pagination with keyset (cursor) scrolling for stable, O(log n)
  page traversal
- Add `query` search param that performs OR match on `username` and `email`
- Introduce reusable shared-module abstractions (`ScrollParams`,
  `CursorPageResult`, `CursorScrollResponse`, `ScrollRequest`, `CursorEncoder`)
  so other use cases can adopt keyset pagination without reimplementing it
- HMAC-signed cursor tokens to prevent tampering and hide internal DB values
- Remove all offset-pagination code for the users list (no dual-mode support)

**Non-Goals:**

- Total count / pagination metadata beyond `nextPageToken`
- Sorting by fields other than `(created_at DESC, id DESC)`
- Backward pagination (previous page)
- Cursor expiry / TTL
- Full-text search (ILIKE substring match is sufficient)

## Decisions

### 1. Keyset columns: `(created_at DESC, id DESC)`

`created_at` is immutable and indexed. `id` is UUIDv7 (time-ordered), providing
a stable tiebreaker when two rows share the same `created_at`. Together they
form a unique, monotonically ordered key suitable for keyset pagination.

Alternative considered: `id` alone — rejected because UUIDv7 ordering is not
guaranteed to be perfectly monotonic across concurrent inserts.

### 2. Cursor encoding: HMAC-SHA256 signed Base64url

```
payload  = "<createdAt_epoch_ms>:<uuid>"
signature = HMAC-SHA256(payload, secret)[0..7]  (first 8 hex chars, 32-bit)
token    = Base64url( payload + ":" + signature )
```

Rationale: prevents clients from crafting arbitrary cursors that could expose
internal data or skip rows. Lightweight — no external store needed.

Alternative considered: plain Base64 — rejected (exposes DB timestamps and
UUIDs, no tamper protection). Redis opaque token — rejected (adds infra
dependency for a non-critical feature).

### 3. Reusable shared abstractions mirror existing offset pattern

The existing pattern is `SliceParams` → `SliceRequest` → `PageResult` →
`SliceHttpResponse`. The new keyset pattern mirrors it exactly:

```
ScrollParams (interface)
  └── ScrollRequest (generic impl, no filters)
      └── SearchUsersRequest (domain-specific, adds query field)

CursorPageResult<T>   (domain, mirrors PageResult)
CursorScrollResponse<T> (HTTP envelope, mirrors SliceHttpResponse)
CursorEncoder         (shared utility, lives in shared/infrastructure/web)
```

This keeps the shared module's API surface consistent and makes adoption by
other services straightforward.

### 4. `UserScrollFilter` replaces `UserFilter`

`UserFilter` had `emailContains` + `authProvider` (AND logic). The new search
uses a single `query` string with OR logic across `email` and `username`. These
are semantically different enough to warrant a separate type. `UserFilter` is
removed entirely since the only consumer (`GetUsersSliceUseCase`) is also
removed.

### 5. Endpoint: `GET /v1/users:search` (Google custom method style)

Chosen over `/users/search` to avoid ambiguity with `GET /users/{id}` path
parameter matching. The `:` custom method suffix is supported by Spring MVC via
`@GetMapping("/{version}/users:search")`.

## Risks / Trade-offs

- **HMAC secret management** → The secret must be injected via config
  (`app.cursor.secret`). If rotated, all in-flight tokens become invalid.
  Mitigation: document rotation procedure; clients should treat `INVALID_CURSOR`
  as "start from beginning".
- **No backward pagination** → Clients cannot go to a previous page. Mitigation:
  acceptable for infinite-scroll / load-more UX patterns; document this
  constraint clearly.
- **Filter + keyset interaction** → When `query` is present, the keyset
  `WHERE (created_at, id) < (?, ?)` clause must be combined with the ILIKE
  filter. The composite index on
  `(created_at DESC, id DESC) WHERE deleted_at IS NULL` may not be used when the
  query filter is selective. Mitigation: add a separate partial index on
  `(email, created_at DESC, id DESC)` and `(username, created_at DESC, id DESC)`
  if query performance degrades.
- **Breaking change** → Removes `GET /v1/users`. Any existing client must
  migrate. Mitigation: document in proposal; coordinate with frontend teams.

## Migration Plan

1. Add Flyway migration `V?__add_keyset_index.sql` — composite index on
   `users(created_at DESC, id DESC) WHERE deleted_at IS NULL`
2. Deploy shared module changes (additive — no existing classes removed)
3. Deploy user-management service changes (removes old endpoint, adds new one)
4. Frontend clients update to `GET /v1/users:search`

Rollback: revert service deployment; shared module additions are
backward-compatible.

# Why

Users currently cannot delete their own accounts — there is no self-service
deletion endpoint. When a user is removed, the operation is permanent and
irreversible, with no audit trail. Implementing soft delete allows users to
deactivate their own accounts while preserving data integrity and enabling
admin-side recovery.

## What Changes

- Add `deleted_at` timestamp column to the `users` table (Flyway migration)
- Add soft-delete behavior to the `User` domain model (`delete()`,
  `isDeleted()`, `undelete()`)
- Add `DELETE /api/v1/auth/me` endpoint — authenticated user soft-deletes their
  own account
- Add `POST /api/v1/auth/me:undelete` endpoint — admin restores a soft-deleted
  user (no RBAC yet; deferred)
- Cascade: revoke all refresh tokens when a user is soft-deleted
- Guard: reject JWT requests from soft-deleted users (check `deleted_at` in
  `JwtAuthFilter`)
- Guard: prevent soft-deleted users from logging in (`LoginUserUseCase`)
- Guard: `existsByEmail` and `findByEmail` queries filter to active users only

## Capabilities

### New Capabilities

- `user-self-delete`: Authenticated user can soft-delete their own account via
  `DELETE /auth/me`. The operation is one-way for the user — no self-restore.
  All refresh tokens are revoked immediately. Subsequent JWT requests are
  rejected with 401.
- `user-undelete`: Admin can restore a soft-deleted user via
  `POST /auth/me:undelete`. Returns 204 No Content. No RBAC enforcement in this
  iteration (deferred).

### Modified Capabilities

<!-- No existing specs to modify -->

## Impact

- **Database**: `users` table gains `deleted_at TIMESTAMPTZ` column; email
  unique constraint replaced with partial unique index
  (`WHERE deleted_at IS NULL`)
- **Domain**: `User` aggregate updated with soft-delete fields and methods
- **Auth flow**: `LoginUserUseCase` must reject deleted users; `JwtAuthFilter`
  must check `deleted_at` on every authenticated request
- **Registration**: `RegisterUserUseCase` / `existsByEmail` must filter to
  active users only
- **Refresh tokens**: Cascade revocation on user deletion (reuses existing
  `revokeAllByUserId` pattern)
- **No new dependencies** required

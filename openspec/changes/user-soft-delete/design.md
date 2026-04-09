# Context

The `user-management` service currently has no mechanism for users to delete
their own accounts. The `User` aggregate has no soft-delete field, and all user
queries return all records without any deletion filter. The `RefreshToken`
aggregate already uses a nullable `revokedAt` timestamp pattern for logical
deletion — this same pattern will be applied to `User`.

The service uses hexagonal architecture: domain model → port interface → JPA
adapter. JWT auth is stateless; the `JwtAuthFilter` sets the authenticated
`userId` (as String) as the principal in `SecurityContextHolder`. There is no
RBAC in the current codebase.

## Goals / Non-Goals

**Goals:**

- Allow an authenticated user to soft-delete their own account via
  `DELETE /api/v1/auth/me`
- Immediately revoke all refresh tokens on deletion (cascade, reusing existing
  `revokeAllByUserId`)
- Reject subsequent JWT requests from deleted users (guard in `JwtAuthFilter`)
- Prevent deleted users from logging in (`LoginUserUseCase`)
- Prevent email re-registration collision with soft-deleted accounts (partial
  unique index)
- Provide an admin-restore endpoint `POST /api/v1/auth/me:undelete` (no RBAC
  enforcement in this iteration)

**Non-Goals:**

- Password confirmation / re-authentication before deletion (not required in
  this iteration)
- RBAC / role-based guard on the undelete endpoint (deferred)
- Grace period or scheduled hard-delete purge job (deferred)
- Audit trail of who performed the undelete (deferred)
- Frontend implementation (Android / Web)

## Decisions

### Decision 1: Nullable timestamp (`deletedAt`) over boolean flag

Follow the existing `RefreshToken.revokedAt` pattern. A nullable
`Instant deletedAt` encodes both the deletion state and the deletion time in a
single column, avoiding a separate `is_deleted` boolean. Filtering is
`WHERE deleted_at IS NULL` for active records.

Alternatives considered:

- `isDeleted` boolean + `deletedAt` timestamp: redundant; two columns for one
  state.
- Hibernate `@SoftDelete` annotation: opaque, hard to bypass for admin queries,
  requires Hibernate 6.4+. Rejected for consistency with existing codebase
  patterns.

### Decision 2: Check `deletedAt` in `JwtAuthFilter` for immediate token invalidation

On every authenticated request, `JwtAuthFilter` loads the user's `deletedAt`
from the DB (via `UserRepository.findById`) and rejects with 401 if non-null.
This provides immediate invalidation without Redis or a token blacklist.

Alternatives considered:

- Redis token blacklist: immediate but adds infrastructure dependency not
  present in the project.
- Short-lived access tokens only: not immediate — deleted user retains access
  until token expiry.
- Token version claim in JWT: requires JWT re-issuance infrastructure changes.

The DB-check approach is consistent with the existing filter pattern and
requires no new dependencies.

### Decision 3: Partial unique index on `email WHERE deleted_at IS NULL`

Replace the current `CONSTRAINT uq_users_email UNIQUE (email)` with a PostgreSQL
partial unique index. This allows a new user to register with an email
previously used by a soft-deleted account.

```sql
DROP CONSTRAINT uq_users_email;
CREATE UNIQUE INDEX uq_active_users_email ON users(email) WHERE deleted_at IS NULL;
```

### Decision 4: `POST /auth/me:undelete` with `userId` in request body, returns 204

Google API Design Guide colon-verb pattern for custom actions. Since there is no
RBAC yet, the endpoint is authenticated-only (any valid JWT). The `userId` to
restore is passed in the request body. Returns 204 No Content (no body needed
for a state toggle).

### Decision 5: No password confirmation on self-delete

Deferred for simplicity. The endpoint requires a valid JWT, which is sufficient
for this iteration.

## Risks / Trade-offs

- **DB query on every request** → `JwtAuthFilter` now hits the DB for every
  authenticated call. Mitigation: add an index on `users.id` (already the PK);
  acceptable overhead for this service's scale.
- **Undelete endpoint has no RBAC** → any authenticated user can restore any
  deleted user by ID. Mitigation: document as known limitation; enforce RBAC in
  a follow-up change when roles are added to JWT.
- **Partial index is PostgreSQL-specific** → H2 (used in tests) does not support
  partial unique indexes. Mitigation: use a separate H2-compatible migration
  script under `db/h2-migration/` that omits the partial index and uses a
  standard unique constraint instead.
- **`existsByEmail` in `RegisterUserUseCase` must be updated** → if not updated,
  registration will incorrectly block emails belonging to soft-deleted users.
  Mitigation: covered in tasks.

## Migration Plan

1. Deploy Flyway migration `V3__add_user_soft_delete.sql` — adds `deleted_at`
   column, drops old unique constraint, creates partial unique index. Column is
   nullable with no default, so existing rows are unaffected
   (`deleted_at = NULL` = active).
2. Deploy updated application code. No data backfill required.
3. Rollback: `V3` can be reversed by dropping the column and re-adding the
   original unique constraint (no data loss since `deleted_at` is additive).

## Open Questions

- When RBAC is added, should the undelete endpoint move to a dedicated admin
  controller or stay in `AuthController`?
- Should a purge job (hard-delete after N days) be part of a follow-up change?

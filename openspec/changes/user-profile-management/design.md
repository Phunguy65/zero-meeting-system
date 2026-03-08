# Context

The `user-management` service follows Hexagonal Architecture with four layers:
`domain`, `application`, `infrastructure`, and `presentation`. Domain events are
published via the transactional outbox pattern to Kafka as CloudEvents 1.0.

Current state relevant to this change:

- `User` aggregate has `fullName`, `avatarUrl`, `preferences` (raw JSON string),
  `authProvider`
- `UserPreferencesRequest` is a typed record (`theme`, `defaultMic`,
  `defaultCamera`) used for both PUT input and (incorrectly) as a response type
  in `LoginResponse`
- `UserRepository` port has no paginated query methods
- `PageResult<T>` and `SliceHttpResponse<T>` exist in the shared module but are
  unused in user-management
- `jackson-databind-nullable` is on the classpath and `JsonNullableModule` is
  registered in `JacksonConfig`
- No `UserUpdatedEvent` exists; downstream services have no way to react to
  profile changes

## Goals / Non-Goals

**Goals:**

- Introduce partial update (PATCH) for profile fields and preference fields
  using `JsonNullable<T>`
- Publish `UserUpdatedEvent` (ECST) on every successful profile patch so
  downstream services can maintain local projections without HTTP callbacks
- Expose GET endpoints for single user and paginated user slice with filtering
- Fix the `UserPreferencesRequest`-as-response anti-pattern by introducing a
  dedicated `UserPreferencesResponse` DTO

**Non-Goals:**

- Admin role or elevated permissions — all endpoints are self-service
  (authenticated user only) or open to any authenticated user (GET by ID, GET
  slice)
- Email or password update — these remain auth-domain concerns
- Cursor-based pagination — offset-based `Slice` is sufficient at current scale
- Preference schema evolution — `theme`, `defaultMic`, `defaultCamera` remain
  the only fields

## Decisions

### D1 — `JsonNullable<T>` for PATCH DTOs (Jackson 3 variant)

**Decision**: Use `JsonNullable<T>` from the local `jackson-databind-nullable`
module for all PATCH request fields. Initialize all fields to
`JsonNullable.undefined()` by default. Use `JsonNullableJackson3Module` (already
registered via `JacksonConfig`) since the project uses Jackson 3
(`tools.jackson`).

**Rationale**: Distinguishes three states — field absent (skip), field present
with value (apply), field present as null (clear). Standard `Optional<T>` cannot
represent this in JSON deserialization.

**Alternative considered**: Separate endpoints per field (e.g.,
`PATCH /users/me/fullName`). Rejected — chatty API, poor client ergonomics.

---

### D2 — Two PATCH endpoints: profile vs preferences

**Decision**:

- `PATCH /api/v1/users/me` — updates `fullName`, `avatarUrl`; if `preferences`
  key is present, replaces the entire preferences object (RFC 7386 JSON Merge
  Patch semantics)
- `PATCH /api/v1/users/me/preferences` — updates individual preference fields
  (`theme`, `defaultMic`, `defaultCamera`) using `JsonNullable<T>`; merges into
  existing preferences

**Rationale**: Profile fields and preference fields have different semantics.
Profile patch replaces preferences wholesale when the key is present (simple,
predictable). Preference patch allows granular per-field updates without sending
all three fields every time.

**Alternative considered**: Single `PATCH /users/me` with nested
`JsonNullable<Map>` for preferences. Rejected — loses type safety and validation
on individual preference fields.

---

### D3 — `UserUpdatedEvent` as ECST fat event, registered via domain method

**Decision**: `UserUpdatedEvent` carries `fullName`, `avatarUrl`, and
`authProvider` alongside the standard `eventId`, `aggregateId`, `email`,
`updatedAt`. It is registered inside `User.updateProfile()` via
`registerEvent()`, consistent with `UserRegisteredEvent` and `UserDeletedEvent`.
The use case publishes it after save via `getDomainEvents()` → `publishEvent()`
→ `clearDomainEvents()`.

**Rationale**: ECST allows `meeting-management` and `chat-management` to update
their local user projections (display name, avatar) without HTTP callbacks to
`user-management`. Registering inside the domain method ensures the event is
never forgotten when the mutation occurs.

`preferences` is intentionally excluded from the event — it is a private user
setting with no relevance to downstream services.

**Alternative considered**: Thin event (only `userId` + `updatedAt`). Rejected —
forces downstream services to call back, creating HTTP coupling and latency.

---

### D4 — `UserPreferencesResponse` as a dedicated output DTO

**Decision**: Introduce
`UserPreferencesResponse(String theme, boolean defaultMic, boolean defaultCamera)`
as an output-only DTO. `UserResponse` uses `UserPreferencesResponse`, not
`UserPreferencesRequest`. `UserPreferencesParser` gains an overload (or is
updated) to return `UserPreferencesResponse`.

**Rationale**: `UserPreferencesRequest` carries `@Pattern` validation
annotations and a `defaults()` factory intended for input processing. Using it
as a response type conflates input and output contracts, making future changes
to either side risky. `LoginResponse` currently misuses `UserPreferencesRequest`
— this change does not fix that existing tech debt to keep scope contained.

**Alternative considered**: Reuse `UserPreferencesRequest` for output (status
quo). Rejected — perpetuates the anti-pattern and couples input validation to
output shape.

---

### D5 — Slice-based pagination with JPQL filtering

**Decision**: `UserRepository` port gains
`findActiveUsers(int page, int size, UserFilter filter)` returning
`PageResult<User>`. `UserFilter` is a plain Java record with nullable
`emailContains` and `authProvider` fields. The JPA implementation uses a
`@Query` JPQL with `IS NULL OR` guards for optional parameters, returning
`Slice<UserJpaEntity>`.

**Rationale**: `PageResult<T>` and `SliceHttpResponse<T>` are already in the
shared module and documented for exactly this use. `Slice` avoids the expensive
`COUNT(*)` query. JPQL with nullable guards is simpler than
`JpaSpecificationExecutor` for two filter fields.

**Alternative considered**: `JpaSpecificationExecutor` + `Specification`
pattern. Rejected — over-engineered for two filter fields; adds boilerplate
without benefit at this scale.

---

### D6 — `UserResponse` field set

**Decision**:

```
UserResponse(
  UUID    id,
  String  email,
  String  fullName,
  @Nullable String  avatarUrl,
  String  authProvider,
  UserPreferencesResponse preferences,
  Instant createdAt,
  Instant updatedAt
)
```

`preferences` is always populated (defaults applied via `UserPreferencesParser`
if null in DB). `hashedPassword` and `googleUid` are never exposed.

**Rationale**: Clients need all profile fields in one call to render the profile
screen. Preferences are included because the client needs them immediately after
loading the profile (same reason `LoginResponse` includes them).
Security-sensitive fields are excluded.

## Risks / Trade-offs

- **`UserPreferencesParser` returns `UserPreferencesRequest`** — updating it to
  return `UserPreferencesResponse` requires updating
  `GetUserPreferencesUseCase`, `LoginUserUseCase`, `LoginWithGoogleUseCase`, and
  `RefreshTokenUseCase` which all use the parser. This is intentional cleanup
  but touches multiple use cases. → Mitigation: update all call sites in the
  same change; existing tests will catch regressions.

- **ECST event schema is a public contract** — once `UserUpdatedEvent` is
  published to Kafka, downstream consumers depend on its field set. Adding
  fields later is safe (additive); removing or renaming fields is breaking. →
  Mitigation: version the event type
  (`io.github.phunguy65.zms.user.updated.v1`); document the schema in the event
  Javadoc.

- **Preferences patch merges into existing JSON** — if the stored JSON is
  malformed, `UserPreferencesParser` falls back to defaults before merging. The
  patch then writes defaults
    - the patched fields, silently discarding the malformed data. → Acceptable:
      malformed preferences are already treated as defaults by the GET endpoint.

## Open Questions

<!-- None — all decisions resolved during exploration -->

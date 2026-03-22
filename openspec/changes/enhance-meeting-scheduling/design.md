# Context

The `meeting-management` service currently supports basic meeting scheduling
(`INSTANT` / `SCHEDULED`) with a minimal `MeetingSettings` JSONB (`waitingRoom`,
`allowGuest`, `muteOnEntry`). There is no description field, no participant
limit, no duration validation, and no way to pre-invite users at scheduling
time.

The `user-management` service exposes user lookup via REST only. No
cross-service gRPC infrastructure exists yet, though the build system already
has the `protobuf-gradle-plugin` configured and empty proto source directories
in place.

Consul is fully wired for service discovery and dynamic config
(`spring.cloud.consul.config.watch.enabled=true`), but no
`@ConfigurationProperties` beans currently consume dynamic values.

## Goals / Non-Goals

**Goals:**

- Add `description` (free-text) to meetings
- Extend `MeetingSettings` with `maxParticipants`, `recordingEnabled`,
  `requireApproval`, `screenShareMode`, `chatEnabled`
- Enforce meeting duration constraints (min/max) validated against Consul-driven
  dynamic config
- Allow hosts to invite users by email or username at scheduling time; only
  existing users accepted (option B)
- Introduce gRPC for `meeting-management` → `user-management` cross-service user
  resolution
- Enforce `maxParticipants` at join time with race-condition safety
- Publish `MeetingInvitationsSentEvent` for downstream notification service

**Non-Goals:**

- RSVP / accept-decline flow (invitees are stored as `PENDING`; no endpoint to
  respond)
- Recurring meetings
- Calendar integration (iCal, Google Calendar export)
- Participant limit tiers (single configurable ceiling from Consul, not
  per-user-tier)
- Email delivery (notification service consumes the event; out of scope here)
- gRPC TLS / mTLS (plaintext for internal cluster communication)

## Decisions

### D1 — gRPC library: `net.devh` grpc-spring-boot-starter

**Decision**: Use `net.devh:grpc-spring-boot-starter` (community, stable) rather
than the official `spring-grpc` (1.0.0 GA Dec 2025).

**Rationale**: The project is on Spring Boot 4.0.3. The official `spring-grpc`
1.0.0 targets Spring Boot 3.x and its Spring Boot 4.x compatibility is
unverified at this time. `net.devh` has a proven track record and explicit
Spring Boot 3/4 support. Migration path to official Spring gRPC can happen
later.

**Alternatives considered**: Official `spring-grpc` — rejected due to Spring
Boot 4.x compatibility uncertainty.

---

### D2 — Proto location: `services/shared/src/main/proto/`

**Decision**: Place `.proto` files in the existing `shared` library module, not
in a separate `proto-common` module.

**Rationale**: The project already has a `services/shared` library consumed by
all services. Adding a dedicated proto module would require new Gradle module
wiring. The `shared` module is the natural home for cross-service contracts. The
`protobuf-gradle-plugin` is already configured in `build-logic/service.base`
convention plugin — `shared` inherits it.

**Alternatives considered**: Separate `libs/proto` module — rejected to avoid
unnecessary module proliferation.

---

### D3 — gRPC message: `UserSnapshot` (full state capture)

**Decision**: The `BatchGetUser` RPC returns `UserSnapshot` — a full capture of
user state mirroring `UserResponse.java` — rather than a minimal projection.

**Rationale**: `meeting-management` needs `fullName` and `email` for
`meeting_invitees.display_name`. Future use cases (notification service, audit)
will need `avatarUrl`, `authProvider`, timestamps. Returning full state now
avoids a second RPC or proto version bump later. Fields excluded for security:
`hashedPassword`, `googleUid`, `deletedAt`.

**Preferences field**: Use `google.protobuf.Struct` — exact proto equivalent of
`Map<String, Object>` from `UserPreferencesResponse`.

---

### D4 — Invitee resolution: fail-fast on unknown identifier

**Decision**: If any email or username in the `invitees[]` list does not resolve
to an existing active user, `ScheduleMeetingUseCase` returns
`MeetingError.InviteeNotFound` immediately (HTTP 422). The meeting is not
created.

**Rationale**: User explicitly chose option B. Storing unresolved emails
silently would create orphaned invitees with no `user_id`, complicating the
join-time matching logic.

**Alternatives considered**: Store unresolved emails as pending (option A) —
rejected per user decision.

---

### D5 — Participant capacity enforcement: `SELECT FOR UPDATE` on meeting row

**Decision**: `JoinMeetingUseCase` acquires a pessimistic write lock on the
`meetings` row before counting active `participation_logs`. Count is compared
against `settings.maxParticipants`.

**Rationale**: Prevents the TOCTOU race where two users simultaneously see "N-1
/ N" and both join. Spring Data JPA `@Lock(PESSIMISTIC_WRITE)` maps cleanly to
`SELECT ... FOR UPDATE`. No advisory locks (PgBouncer compatibility unknown). No
counter column (would require sync on leave).

**Alternatives considered**: Counter column with atomic UPDATE — viable but
requires counter maintenance on leave; deferred to future optimization if lock
contention becomes measurable.

---

### D6 — Dynamic config: `@RefreshScope` + `@ConfigurationProperties`

**Decision**: Introduce `MeetingLimitsConfig` as a `@RefreshScope` +
`@ConfigurationProperties(prefix = "meeting.limits")` bean. Consul KV path:
`config/meeting-management/data` (YAML format, already configured).

**Rationale**: Consul watch is already enabled. `@RefreshScope` provides
hot-reload without restart. Default fallback values in the bean ensure the
service starts without Consul.

**Config keys**:

```yaml
meeting:
    limits:
        max-participants-ceiling: 500
        max-duration-minutes: 480
        min-duration-minutes: 15
```

---

### D7 — `meeting_invitees` schema: email as unique key per meeting

**Decision**: `UNIQUE (meeting_id, email)` constraint. `user_id` is stored but
nullable (populated from gRPC resolution). `display_name` is denormalized from
`UserSnapshot.fullName` at invite time.

**Rationale**: Email is the stable identifier for an invite. `user_id` can
change (account merge) but email is the invite target. `display_name` is
denormalized to avoid a join to user service at read time.

---

### D8 — `MeetingInvitationsSentEvent`: aggregate-level, not per-invitee

**Decision**: Publish one `MeetingInvitationsSentEvent` per
`ScheduleMeetingUseCase` execution, carrying the full list of invitee emails and
user IDs. Not one event per invitee.

**Rationale**: Notification service can batch-send emails from a single event.
Reduces outbox table rows. Consistent with existing event granularity
(`MeetingScheduledEvent` is one event per meeting).

## Risks / Trade-offs

- **gRPC call in scheduling transaction**: `UserServicePort.resolveUsers()` is
  called inside `@Transactional`. A slow or unavailable user-management service
  will hold the DB transaction open. Mitigation: set a short gRPC deadline
  (e.g., 2s); map `DEADLINE_EXCEEDED` / `UNAVAILABLE` to
  `MeetingError.UserServiceUnavailable` and return HTTP 503.

- **`SELECT FOR UPDATE` lock contention**: Under high join concurrency for a
  popular meeting, row-level locks on `meetings` will serialize joins.
  Mitigation: acceptable for current scale; revisit with counter column if p99
  join latency degrades.

- **`UserSnapshot` staleness**: `display_name` in `meeting_invitees` is captured
  at invite time. If the user later changes their name, the stored
  `display_name` is stale. Mitigation: acceptable — invite display name is a
  point-in-time snapshot. Live display name is fetched from user service at join
  time.

- **Proto in `shared` module**: All services that depend on `shared` will
  compile the proto stubs, even if they don't use gRPC. Mitigation: proto
  compilation is fast; if it becomes a concern, extract to a separate module
  later.

## Migration Plan

1. Deploy `user-management` with gRPC server enabled (new port, e.g., 9090) —
   backward compatible, existing REST unchanged.
2. Deploy `meeting-management` with gRPC client configured pointing to
   `user-management:9090`.
3. Apply Flyway V5 migration (additive: new column + new table — no data
   migration needed).
4. Set Consul KV `config/meeting-management/data` with initial limits before or
   after deploy (defaults in bean ensure safe startup either way).
5. **Rollback**: Remove gRPC client config from `meeting-management`; revert V5
   migration (drop `meeting_invitees`, drop `description` column). No data loss
   risk since V5 is additive.

## Open Questions

- Should `screenShareMode` be an enum column or a string in JSONB? → Keeping as
  string in JSONB (`"ALL"` | `"HOST_ONLY"` | `"DISABLED"`) with
  application-level validation. No DB enum to avoid migration cost on future
  values.
- gRPC port for `user-management`: use `9090` (standard) or a different port? →
  `9090` assumed; configurable via `grpc.server.port` property.
- Should `MeetingInvitationsSentEvent` include the full `UserSnapshot` per
  invitee or just `(userId, email)` pairs? → Just `(userId, email, displayName)`
  tuples — notification service only needs these to send emails.

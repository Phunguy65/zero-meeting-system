# Context

The meeting-management service currently allows any user with a meeting link to
join a LIVE meeting immediately (after password check if configured). The
`JoinMeetingUseCase` has a commented-out TODO at line 54-56 for
`requiredApproval` support, indicating this was planned but never implemented.

**Current state:**

- `MeetingSettings` has `boolean requiredApproval` in the domain model
- The API and persistence layers use `waitingRoom: boolean` (naming
  inconsistency across layers)
- `JoinMeetingUseCase` bypasses the TODO entirely, generating LiveKit tokens
  unconditionally
- No mechanism exists for hosts to review or approve participants

**Constraints:**

- Spring MVC (not WebFlux) — SSE uses `SseEmitter`
- `HeaderAuthFilter` reads `X-User-ID` from request headers (injected by Kong
  gateway) — browser `EventSource` API cannot set custom headers
- Redis (Valkey) already present but only used for caching (`RedisCacheManager`)
- All aggregates use PostgreSQL via JPA; Redis used only as cache
- Virtual threads enabled (`spring.threads.virtual.enabled=true`)
- Kafka outbox pattern for cross-service durable events

## Goals / Non-Goals

**Goals:**

- Replace `boolean requiredApproval` with extensible `AdmissionPolicy` enum
  (`ALLOW_ALL`, `MANUAL_APPROVAL`)
- Implement Redis-backed join request queue with configurable TTL (default 5
  minutes)
- Deliver real-time join request notifications to hosts via SSE
- Deliver join request status to participants via polling
- Auto-deny pending requests when meeting ends
- Support guest join requests (identified by `deviceId`)
- Google API-style endpoints (`:requestJoin`, `:approve`, `:deny`,
  `:approveAll`)
- Design the `AdmissionPolicy` enum as an extension point for future trust
  levels (domain-trusted, invited-only, restricted)

**Non-Goals:**

- Organization-based trust (domain-matching, SAML/SSO) — future capability
- Pre-invited guest whitelist — future capability
- Restricted/host-only admission policy — future capability
- Mobile push notifications for join requests
- Co-host role or delegation of approval rights
- Audit log persistence for join request history (Redis-only, ephemeral)
- Frontend/UI implementation

## Decisions

### D1: `AdmissionPolicy` enum over boolean

**Decision:** Replace `boolean requiredApproval` with `AdmissionPolicy` enum
starting with `ALLOW_ALL` and `MANUAL_APPROVAL`.

**Rationale:** A boolean cannot express future trust levels (domain-trusted,
invited-only, restricted). An enum is an explicit extension point — adding
`DOMAIN_TRUSTED` later requires only a new enum value and a handler, not a
schema change.

**Alternative considered:** Nested policy object (e.g.,
`{type: "manual", config: {...}}`). Rejected as over-engineering for current
scope — enum is simpler and sufficient.

**Naming:** API and persistence use `admissionPolicy` (snake-case in JSON:
`admission_policy`). Fixes the existing `waitingRoom` / `requiredApproval`
inconsistency across layers.

---

### D2: Redis-only storage for join requests (no DB persistence)

**Decision:** Store join requests exclusively in Redis using Sorted Set
(queue) + Hash (metadata). No PostgreSQL persistence.

**Rationale:** Join requests are ephemeral by design (5-minute TTL). They are
not business-critical domain objects — losing them on Redis restart is
acceptable (participants re-request). High write throughput and low-latency
reads fit Redis well. Avoids DB schema migration complexity for transient data.

**Alternative considered:** Store in PostgreSQL with TTL-based cleanup job.
Rejected — adds schema complexity and unnecessary durability for transient
state.

**Redis schema:**

```
ZSET  join_request:{meetingId}          score=expiresAt(ms), member=requestId
HASH  join_request_meta:{requestId}     fields: meetingId, userId, displayName,
                                                deviceId, status, requestedAt, expiresAt
TTL   join_request_meta:{requestId}     = timeout + 1h buffer
```

---

### D3: SSE for host, polling for participants

**Decision:** SSE (`GET /meetings/{id}/events`) for real-time host
notifications; polling (`GET /joinRequests/{requestId}`) for participant status.

**Rationale:** `HeaderAuthFilter` reads `X-User-ID` from request headers (set by
Kong). The browser `EventSource` API cannot set custom headers, making SSE
authentication impossible for clients going through Kong without changes.
However, SSE for the **host** is feasible because:

- The host client can pass `X-User-ID` through Kong (Kong injects it)
- If needed, the SSE endpoint can be secured at the Kong layer

Participants only need to know their own request status (binary:
pending/approved/denied/expired) — polling every 2 seconds is sufficient and
simple. No special auth mechanism needed for polling since `requestId` acts as a
capability token.

**Alternative considered:** WebSocket for both host and participants. Rejected —
adds WebSocket infrastructure for what is essentially a unidirectional use case
(server → host).

**Alternative considered:** Global user-level SSE (`/users/{id}/events`).
Rejected — per-meeting SSE is simpler, naturally scoped to meeting lifecycle,
and avoids routing complexity.

---

### D4: Redis Pub/Sub for SSE fan-out (multi-instance support)

**Decision:** Use Redis Pub/Sub channel `meeting:{meetingId}:events` to
broadcast join request events to all backend instances. Each instance holds SSE
connections in a local `ConcurrentHashMap<UUID, List<SseEmitter>>` and relays
Redis messages to connected emitters.

**Rationale:** SSE connections are held in-process (local memory). In a
multi-instance deployment, the host's SSE connection may be on a different
instance than the one processing an approve/deny request. Redis Pub/Sub bridges
the instances so any instance can fan-out to the host regardless of which
instance holds the SSE connection.

**Scope:** Requires adding `RedisMessageListenerContainer` and
`StringRedisTemplate` beans to `meeting-management` infrastructure config.

---

### D5: Guest identification via `deviceId`

**Decision:** Guests (no `userId`) are identified in the join request queue by
`deviceId`. Duplicate prevention: before creating a new request, check if a
PENDING request already exists for the same `meetingId` + `deviceId`. If yes,
return the existing `requestId` (idempotent).

**Redis lookup for duplicate check:**

```
ZSCORE join_request:{meetingId} → scan metadata by deviceId
```

Since Sorted Set members are `requestId`s, duplicate detection requires either:

- A secondary index:
  `SET join_request_device:{meetingId}:{deviceId} {requestId}` (TTL = request
  TTL)
- Or scanning all metadata entries (O(N), impractical at scale)

**Decision:** Use secondary index `join_request_device:{meetingId}:{deviceId}` →
`requestId`.

---

### D6: Auto-deny on meeting end via domain event listener

**Decision:** Listen to `MeetingEndedEvent` in a
`@TransactionalEventListener(phase = AFTER_COMMIT)` handler. After meeting ends,
fetch all pending request IDs from `join_request:{meetingId}` ZSET, update each
status to `DENIED`, publish `JoinRequestDeniedEvent` to Redis Pub/Sub, then
delete the ZSET.

**Rationale:** Reuses existing domain event infrastructure. No new scheduler or
polling needed for this behavior.

---

### D7: Instant meetings default to `MANUAL_APPROVAL`

**Decision:** `CreateInstantMeetingUseCase` defaults
`admissionPolicy = MANUAL_APPROVAL`. Scheduled meetings accept `admissionPolicy`
from the API request (configurable).

**Rationale:** Instant meetings are typically private/sensitive — secure by
default. Scheduled meetings have explicit configuration.

---

### D8: Approval/denial idempotency

**Decision:**

- Approve already-APPROVED request → 200 OK (no-op, return existing token if
  stored)
- Deny already-DENIED request → 200 OK (no-op)
- Cross-transitions (approve DENIED, deny APPROVED) →
  `MeetingError.InvalidJoinRequestTransition`

**Note:** Approved tokens are not stored in Redis (stateless JWT). For
already-approved requests, re-generate a new token (idempotent from LiveKit's
perspective).

## Risks / Trade-offs

**[Risk] Redis data loss → Lost join requests** → Mitigation: Join requests are
ephemeral (5-min TTL). Participants re-request on reconnect. Acceptable for this
use case.

**[Risk] Redis Pub/Sub message loss (fire-and-forget)** → Mitigation:
Participants poll as fallback. Host SSE reconnects and fetches pending requests
via `GET /meetings/{id}/joinRequests`. No message replay needed.

**[Risk] SSE connection storms (many participants waiting)** → Mitigation:
Participants use polling, not SSE. SSE is host-only — at most 1–2 connections
per meeting per instance. Not a scalability concern.

**[Risk] Race condition: concurrent approve + deny on same request** →
Mitigation: Use Redis optimistic locking via `WATCH`/`MULTI`/`EXEC` on
`join_request_meta:{requestId}.status`. First writer wins; second returns
`InvalidJoinRequestTransition`.

**[Risk] `admissionPolicy` field breaks existing API consumers** → Mitigation:
Explicitly a breaking change (dev environment). No migration SQL needed.
Document in proposal.

**[Risk] Cleanup job misses expired requests** → Mitigation: Redis TTL on
`join_request_meta:{requestId}` as primary expiry. Cleanup job removes orphaned
ZSET entries as secondary sweep. Defense-in-depth.

**[Trade-off] No audit history for join requests** → Accepted: Ephemeral by
design. If audit is needed in the future, publish `JoinRequestExpiredEvent` /
`JoinRequestDeniedEvent` to Kafka outbox (already-defined event types make this
easy to add).

**[Trade-off] Polling vs SSE for participants** → Polling adds 0–2s latency for
participants learning their approval status. Acceptable for the UX (waiting
screen with spinner). SSE would be more responsive but requires auth
infrastructure changes.

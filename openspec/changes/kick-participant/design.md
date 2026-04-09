# Context

The meeting-management service already tracks active participants through
`participation_logs`, issues LiveKit tokens through `LiveKitPort`, and closes
active sessions through the `participant_left` webhook handled by
`LeaveMeetingUseCase`. Hosts can list participants, but they cannot currently
moderate a live room by removing someone who is already connected.

This change introduces host-driven participant kicks without changing the
existing join and leave lifecycle. The codebase already favors a hexagonal flow
of controller -> command -> use case -> ports/adapters, plus Kafka-backed SSE
fan-out for realtime host views. The design should preserve those patterns
rather than adding direct infrastructure calls from controllers or duplicating
lifecycle logic already handled by LiveKit webhooks.

## Goals / Non-Goals

**Goals:**

- Allow only the meeting host to kick an active participant from a live meeting.
- Support kicking registered users across all active devices in the same
  meeting.
- Support kicking guests by display name across all active guest sessions in the
  same meeting.
- Reuse the existing `participant_left` webhook flow to mark participation logs
  as left after a forced removal.
- Notify host-facing SSE subscribers that a participant was kicked so moderation
  views can refresh promptly.
- Return explicit API errors for invalid targets such as the host themselves or
  users who are not currently active in the room.

**Non-Goals:**

- Prevent a kicked participant from requesting to join again later.
- Add moderation roles beyond the meeting host.
- Add participant-level bans, mute actions, or waiting-room penalties.
- Distinguish kick vs. voluntary leave for every remote participant client; only
  the kicked client needs LiveKit's disconnect reason.

## Decisions

### 1. Kick is a soft removal through LiveKit, not a direct DB mutation

The use case will call a new `LiveKitPort.removeParticipant(roomName, identity)`
method for each active target session. It will not set `leftAt` directly in
`participation_logs`. That responsibility remains with the existing
`participant_left` webhook -> `LeaveMeetingUseCase` flow.

Why:

- It avoids racing with `LeaveMeetingUseCase`, whose domain aggregate rejects
  double-leave updates.
- It keeps LiveKit as the source of truth for connection teardown.
- It reuses the already-established webhook behavior for room lifecycle events.

Alternative considered:

- Directly marking logs as left in the kick use case. Rejected because it
  duplicates lifecycle logic and can conflict with the subsequent webhook.

### 2. Targeting differs for registered users vs. guests

The request model will allow either `userId` or `displayName` as the kick
target. Registered users are kicked by `userId`; guests are kicked by
`displayName`. The repository layer will therefore support two meeting-scoped
lookups:

- active logs by meeting + user ID
- active logs by meeting + display name

Why:

- Registered users must be kicked across all active devices, which is naturally
  keyed by `userId`.
- Guests have `userId = null`, so display name is the only meeting-visible
  identifier currently available through the moderation API.

Alternative considered:

- Kicking by `participantId` or `livekitIdentity`. Rejected because the chosen
  API contract from planning was resource action by meeting with a logical
  user/guest target, not an internal session identifier.

### 3. Kicks are allowed only while the meeting is LIVE

`KickParticipantUseCase` will verify the meeting exists and is currently `LIVE`
before calling LiveKit.

Why:

- A scheduled or ended meeting has no active room to moderate.
- It prevents stale UI actions from surfacing as infrastructure failures.

Alternative considered:

- Calling LiveKit regardless of status and mapping room-not-found errors.
  Rejected because the domain already knows whether moderation is valid.

### 4. Already-gone participants are treated as not active targets

The use case will first resolve active participation logs from the repository.
If no active logs are found, it will return `UserNotInMeeting`. During the
LiveKit removal loop, a per-session 404 from LiveKit will be tolerated so one
stale session does not fail the whole kick for the remaining active sessions.

Why:

- The repository is the primary source for whether someone is active in the
  meeting.
- There is a natural race between host moderation and users leaving on their
  own.

Alternative considered:

- Returning infrastructure-level `LiveKitParticipantNotFound`. Rejected because
  API clients need a domain-level answer about whether the target is still in
  the room.

### 5. Realtime fan-out follows the existing event path

The use case will publish a new `ParticipantKickedEvent`, and
`MeetingSseManager` will consume the Kafka event and send a new host-facing SSE
event such as `participant_kicked`.

Why:

- This matches the current join-request SSE design and keeps use cases decoupled
  from SSE infrastructure.
- It creates an auditable event that other consumers can use later.

Alternative considered:

- Calling `MeetingSseManager` directly from the use case. Rejected because it
  bypasses the event-driven boundary used elsewhere in the service.

### 6. No cooldown or ban is introduced

The system will not persist a kick block in Redis or reject later join attempts
from kicked users.

Why:

- The final product decision was that kicking only removes the current active
  sessions.
- This keeps scope focused on live moderation rather than access policy.

Alternative considered:

- Temporary rejoin blocks in Redis. Rejected as out of scope for this change.

## Risks / Trade-offs

- [Guest display names are not globally unique] -> Restrict guest lookup to the
  current meeting and accept that hosts may kick every active guest session
  sharing the same display name in that meeting.
- [Webhook timing is asynchronous] -> Accept a short delay between successful
  kick API response and participant list cleanup, because the authoritative
  leave update still arrives through LiveKit webhook processing.
- [Partial LiveKit removal success across multiple devices] -> Attempt all
  active sessions and only fail if no removal succeeds; document the behavior in
  tests so one stale session does not block kicking the rest.
- [Host SSE receives a separate moderation event before the participant list
  query catches up] -> Clients should treat `participant_kicked` as a refresh
  hint, not as the sole source of participant state.

## Migration Plan

- Add the new API endpoint, domain errors, repository queries, and LiveKit port
  method in the meeting-management service.
- Deploy the service update together with Kafka topic configuration for the new
  participant-kicked event if explicit topic setup is required in the
  environment.
- Rollback strategy: revert the service deployment. Existing join, leave, and
  participant-list flows remain unchanged because the feature is additive.

## Open Questions

- None.

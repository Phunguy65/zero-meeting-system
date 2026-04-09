# Verify Fixes Log

## [2026-04-03] Round 1 (from spx-apply auto-verify)

### spx-verifier + opsx-arch-verifier

- **Fixed (CRITICAL C1):** Changed `@Transactional(readOnly = true)` to
  `@Transactional` in `KickParticipantUseCase`. The use case publishes an
  application event, so it must run in a read-write transaction — consistent
  with all other event-publishing use cases in the service
  (`ApproveJoinRequestUseCase`, `DenyJoinRequestUseCase`, `RequestJoinUseCase`).
- **Fixed (WARNING W1):** Added proper failure handling in the LiveKit removal
  loop. The loop now accumulates results; if ALL sessions fail with
  `LiveKitUnavailable`, the error is propagated. Partial success (at least one
  session removed or 404'd) still publishes the event — matching design decision
  #4.
- **Fixed (WARNING W2):** Added explicit mutual-exclusivity validation for
  `userId`/`displayName` in `KickParticipantUseCase`. Added
  `MeetingError.InvalidKickTarget` record, added `INVALID_KICK_TARGET` to
  `MeetingErrorCode`, mapped to HTTP 400 in `BaseController`.

### opsx-test-verifier (Round 2)

- **Fixed (CRITICAL — unauthenticated edge case):** Added
  `kickParticipant_noAuthentication_returns401()` to `ParticipantControllerTest`
  — verifies HTTP 401 when no principal is provided, with
  `verifyNoInteractions(kickParticipantUseCase)`.
- **Fixed (WARNING W4 — missing JPA repo tests):** Added
  `findActiveByMeetingIdAndUserId_excludesGuestsAndLeftSessions` and
  `findActiveByMeetingIdAndDisplayName_returnsOnlyGuestsWithMatchingName` to
  `ParticipationLogJpaRepositoryTest`. Both tests verify the `userId IS NULL`
  guard on the guest query correctly excludes registered users and already-left
  sessions.
- **Fixed (SUGGESTION S3 — InvalidKickTarget at controller layer):** Added
  `kickParticipant_invalidTarget_returns400()` to `ParticipantControllerTest` —
  verifies HTTP 400 + `INVALID_KICK_TARGET` error code when neither target is
  provided.

### Notes (not fixed, by design decision):

- **SUGGESTION S2 (`"unknown"` display name sentinel):** The SSE payload sends
  `"unknown"` when `kickedDisplayName` is null (registered user kick). This is a
  frontend concern — the host UI can resolve the display name from
  `kickedUserId` via user service. No change to the SSE model.
- **WARNING W3 (silent SSE deserialization failure):** This is a systemic
  pattern shared by all existing SSE handlers in `MeetingSseManager`. Not
  addressed here — could be a separate improvement.
- **SUGGESTION: Rejoin after kick test:** This is an integration-level concern
  verifying that no Redis block or auth filter inadvertently prevents rejoining.
  The implementation is correct by design (no block added), but a full
  acceptance test would require a running environment.

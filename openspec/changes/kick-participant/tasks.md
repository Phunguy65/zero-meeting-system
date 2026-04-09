# Tasks

## 1. Domain and API contract

- [x] 1.1 Add kick command and request DTOs that support registered-user targets
      by `userId` and guest targets by `displayName`
- [x] 1.2 Add kick-specific domain errors and API error-code mappings for
      self-kick, inactive targets, and invalid request shapes
- [x] 1.3 Add the participant-kicked domain event and SSE payload model for
      host-facing moderation updates

## 2. LiveKit and persistence integration

- [x] 2.1 Extend `LiveKitPort` and `LiveKitAdapter` with a participant-removal
      operation that tolerates already-gone sessions while preserving existing
      error handling conventions
- [x] 2.2 Add participation-log repository queries and adapter support for
      active logs by meeting plus user ID and by meeting plus display name
- [x] 2.3 Update SSE fan-out infrastructure to consume the participant-kicked
      event and emit `participant_kicked` events to host subscribers

## 3. Kick flow implementation

- [x] 3.1 Implement `KickParticipantUseCase` with meeting existence checks,
      LIVE-status guard, host-only authorization, self-kick rejection, and
      multi-session removal behavior
- [x] 3.2 Add the kick endpoint to the participant API and map successful kicks
      to the agreed HTTP response shape
- [x] 3.3 Ensure the kick flow reuses the existing webhook-based leave lifecycle
      without adding any rejoin cooldown logic

## 4. Verification

- [x] 4.1 Add unit tests for `KickParticipantUseCase` covering host
      authorization, meeting-status validation, registered-user multi-device
      kicks, guest kicks by display name, self-kick rejection, and
      inactive-target errors
- [x] 4.2 Add adapter/controller tests for the new repository queries, LiveKit
      removal behavior, and the kick endpoint response/error mappings
- [x] 4.3 Run the meeting-management test suite and address any regressions
      introduced by the moderation flow

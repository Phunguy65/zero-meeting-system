# Why

The current meeting scheduling feature is minimal — no description field, no
participant limits, no way to invite specific users at scheduling time, and
meeting duration constraints are unenforced. As the platform grows, hosts need
richer control over their meetings and the ability to pre-invite attendees by
email or username before a meeting starts.

## What Changes

- Add `description` field to meetings (free-text agenda/notes)
- Extend `MeetingSettings` JSONB with: `maxParticipants`, `recordingEnabled`,
  `requireApproval`, `screenShareMode`, `chatEnabled`
- Add dynamic configuration via Consul for system-wide ceilings:
  `max-participants-ceiling`, `max-duration-minutes`, `min-duration-minutes`
- Add `meeting_invitees` table to track pre-scheduled invitations (email +
  optional user_id)
- Introduce gRPC cross-service call from `meeting-management` →
  `user-management` to resolve users by email/username at scheduling time
- Add `UserService.proto` with `BatchGetUser` RPC returning full `UserSnapshot`
  state
- Enforce participant capacity at join time using `SELECT FOR UPDATE` to prevent
  race conditions
- Add new domain errors: `MeetingFull`, `InviteeNotFound`,
  `InvalidMeetingDuration`
- Publish `MeetingInvitationsSentEvent` for downstream notification service

## Capabilities

### New Capabilities

- `meeting-scheduling-enhanced`: Extended scheduling with description, enriched
  settings, duration validation, and Consul-driven dynamic limits
- `meeting-invitees`: Pre-schedule invitations — invite users by email or
  username when creating a meeting; only existing users can be invited
  (non-existent identifiers return error)
- `user-grpc-service`: gRPC service on `user-management` exposing `BatchGetUser`
  RPC; returns full `UserSnapshot` (id, email, fullName, username, avatarUrl,
  authProvider, preferences, timestamps)
- `meeting-capacity-enforcement`: Enforce `maxParticipants` at join time with
  race-condition-safe locking

### Modified Capabilities

<!-- No existing specs to modify -->

## Impact

- **DB**: New Flyway migration V5 (`meeting_invitees` table, `description`
  column on `meetings`)
- **Domain**: `MeetingSettings`, `Meeting`, `MeetingError` updated; new
  `MeetingLimitsConfig` bean
- **gRPC**: New `.proto` file in `services/shared/src/main/proto/`; runtime gRPC
  deps added to `libs.versions.toml`; `user-management` gets `@GrpcService`
  impl; `meeting-management` gets `UserServicePort` + gRPC adapter
- **Use Cases**: `ScheduleMeetingUseCase` extended; `JoinMeetingUseCase` gets
  capacity check; new `MeetingRepository.findByIdWithLock()`
- **Config**: `MeetingLimitsConfig` `@ConfigurationProperties` + `@RefreshScope`
  reading from Consul KV `config/meeting-management/data`
- **Events**: New `MeetingInvitationsSentEvent` published to Kafka via outbox
- **APIs**: `POST /v1/meetings` request body extended with `description` and
  `invitees[]`

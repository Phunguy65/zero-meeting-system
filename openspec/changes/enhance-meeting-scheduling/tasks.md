# Tasks

## 1. Build Infrastructure — gRPC & Proto

- [x] 1.1 Add gRPC runtime dependencies to `gradle/libs.versions.toml`:
      `grpc-stub`, `grpc-protobuf`, `grpc-netty-shaded`,
      `net.devh:grpc-spring-boot-starter`
- [x] 1.2 Update
      `build-logic/src/main/kotlin/io.github.phunguy65.zms.plugin.service.base.gradle.kts`
      to include gRPC runtime deps and `google.protobuf.Struct` /
      `google.protobuf.Timestamp` well-known types
- [x] 1.3 Create `services/shared/src/main/proto/user/v1/user_service.proto`
      with `BatchGetUser` RPC, `BatchGetUserRequest`, `BatchGetUserResponse`,
      and `UserSnapshot` message (full state: id, email, full_name, username,
      avatar_url, auth_provider, preferences as Struct, created_at, updated_at)

## 2. Database Migration

- [x] 2.1 Create
      `services/meeting-management/src/main/resources/db/migration/V5__add_description_and_invitees.sql`:
      add `description TEXT` column to `meetings`; create `meeting_invitees`
      table with columns `id`, `meeting_id` (FK), `inviter_id`, `user_id`
      (nullable), `email`, `display_name`, `status` (PENDING/ACCEPTED/DECLINED),
      `invited_at`, `responded_at`; add indexes on `meeting_id`, `email`,
      `user_id`; add `UNIQUE (meeting_id, email)` constraint

## 3. Domain Model — meeting-management

- [x] 3.1 Update `MeetingSettings.java` record: add `maxParticipants` (int),
      `recordingEnabled` (boolean), `requireApproval` (boolean),
      `screenShareMode` (String), `chatEnabled` (boolean); update `defaults()`
      factory method with default values
- [x] 3.2 Update `Meeting.java`: add `description` field (`@Nullable String`);
      update `schedule()` factory method signature to accept `description`;
      update `reconstitute()` accordingly
- [x] 3.3 Add new error records to `MeetingError.java` sealed interface:
      `MeetingFull(UUID meetingId, int limit)`,
      `InviteeNotFound(String identifier)`,
      `InvalidMeetingDuration(long actualMinutes, int minMinutes, int maxMinutes)`,
      `UserServiceUnavailable(String detail)`, `InvalidSettings(String detail)`
- [x] 3.4 Add corresponding codes to `MeetingErrorCode.java` enum:
      `MEETING_FULL`, `INVITEE_NOT_FOUND`, `INVALID_MEETING_DURATION`,
      `USER_SERVICE_UNAVAILABLE`, `INVALID_SETTINGS`
- [x] 3.5 Create `MeetingInvitee.java` domain model (aggregate or entity) with
      fields: `id` (UUID), `meetingId` (UUID), `inviterId` (UUID), `userId`
      (@Nullable UUID), `email` (String), `displayName` (@Nullable String),
      `status` (InviteeStatus enum: PENDING/ACCEPTED/DECLINED), `invitedAt`
      (Instant), `respondedAt` (@Nullable Instant); add `InviteeStatus.java`
      enum
- [x] 3.6 Create `MeetingInvitationsSentEvent.java` implementing
      `PublishableEvent`: fields `eventId`, `aggregateId` (meetingId),
      `meetingTitle`, `startTime`, `invitees` (list of `InviteeInfo` records
      with `userId`, `email`, `displayName`); set `topic()` to
      `"meeting-management.meeting.invitations-sent"`

## 4. Domain Port — meeting-management

- [x] 4.1 Create `UserServicePort.java` interface in `domain/port/`: method
      `resolveUsers(List<String> emails, List<String> usernames)` returning
      `Map<String, ResolvedUser>`; define `ResolvedUser` record with `userId`,
      `email`, `displayName`, `username` (@Nullable), `avatarUrl` (@Nullable),
      `authProvider`
- [x] 4.2 Create `MeetingInviteeRepository.java` port interface:
      `saveAll(List<MeetingInvitee> invitees)`,
      `findByMeetingId(UUID meetingId)`,
      `countActiveByMeetingId(UUID meetingId)`

## 5. Dynamic Config — meeting-management

- [x] 5.1 Create `MeetingLimitsConfig.java` in `infrastructure/config/`:
      annotate with `@Component`, `@RefreshScope`,
      `@ConfigurationProperties(prefix = "meeting.limits")`; fields
      `maxParticipantsCeiling` (default 500), `maxDurationMinutes` (default
      480), `minDurationMinutes` (default 15) with getters/setters

## 6. gRPC Server — user-management

- [x] 6.1 Add `net.devh:grpc-spring-boot-starter` and gRPC runtime deps to
      `services/user-management/build.gradle.kts`; add `grpc.server.port=9090`
      to `services/user-management/src/main/resources/application.properties`
- [x] 6.2 Create `UserServiceGrpcImpl.java` in
      `services/user-management/src/main/java/.../infrastructure/grpc/`:
      annotate with `@GrpcService`; implement
      `UserServiceGrpc.UserServiceImplBase.batchGetUser()`; inject
      `UserRepository`; resolve by email and username; map `User` domain model
      to `UserSnapshot` proto message; omit deleted users; return partial
      results (missing keys absent from map)

## 7. gRPC Client Adapter — meeting-management

- [x] 7.1 Add `net.devh:grpc-spring-boot-starter` and gRPC runtime deps to
      `services/meeting-management/build.gradle.kts`; add
      `grpc.client.user-management.address` and
      `grpc.client.user-management.negotiation-type=plaintext` to
      `application.properties`
- [x] 7.2 Create `UserServiceGrpcAdapter.java` in `infrastructure/grpc/`:
      implement `UserServicePort`; inject
      `@GrpcClient("user-management") UserServiceGrpc.UserServiceBlockingStub`;
      call `batchGetUser` with 2s deadline; map proto `UserSnapshot` →
      `ResolvedUser`; map gRPC `UNAVAILABLE`/`DEADLINE_EXCEEDED` →
      `MeetingError.UserServiceUnavailable`

## 8. Persistence Adapters — meeting-management

- [x] 8.1 Create `MeetingInviteeJpaEntity.java` and
      `MeetingInviteeJpaRepository.java` in `infrastructure/persistence/`
- [x] 8.2 Create `MeetingInviteeRepositoryAdapter.java` implementing
      `MeetingInviteeRepository` port; include `toDomain()` / `toEntity()`
      mappers
- [x] 8.3 Update `MeetingRepositoryAdapter.java`: add
      `findByIdWithLock(UUID id)` method using
      `@Lock(LockModeType.PESSIMISTIC_WRITE)`; update `toDomain()` /
      `toEntity()` to handle new `description` field and extended
      `MeetingSettings`
- [x] 8.4 Update `MeetingJpaEntity.java`: add `description` column mapping;
      ensure `settings` JSONB mapping handles new `MeetingSettings` fields

## 9. Use Case Updates — meeting-management

- [x] 9.1 Update `ScheduleMeetingCommand.java` record: add `description`
      (@Nullable String), `invitees` (List of `InviteeInput` record with `email`
      and `username` fields, both nullable)
- [x] 9.2 Update `ScheduleMeetingUseCase.java`: inject `MeetingLimitsConfig`,
      `UserServicePort`, `MeetingInviteeRepository`; add duration validation
      (return `InvalidMeetingDuration` if out of bounds); add settings
      validation (`maxParticipants` ≤ ceiling, `screenShareMode` in allowed
      values); resolve invitees via `UserServicePort` (return `InviteeNotFound`
      if any identifier unresolved); save `MeetingInvitee` records; publish
      `MeetingInvitationsSentEvent` if invitees non-empty
- [x] 9.3 Update `JoinMeetingUseCase.java`: inject `MeetingInviteeRepository`;
      use `meetingRepository.findByIdWithLock()` instead of `findById()`; count
      active participants via `participationLogRepository`; skip capacity check
      for host; return `MeetingFull` if count ≥ `settings.maxParticipants`
- [x] 9.4 Update `MeetingResponse.java` record: add `description` (@Nullable
      String) field

## 10. Presentation Layer — meeting-management

- [x] 10.1 Update `ScheduleMeetingRequest.java`: add `description` (@Nullable,
      `@Size(max=2000)`), `invitees` (@Nullable, `@Size(max=100)` list of
      `InviteeRequest` record with `@Email email` and `username` fields); update
      `toCommand()` method
- [x] 10.2 Update `BaseController.java` (or equivalent error mapping): add HTTP
      status mappings for `MeetingFull` → 409, `InviteeNotFound` → 422,
      `InvalidMeetingDuration` → 400, `UserServiceUnavailable` → 503,
      `InvalidSettings` → 400

## 11. H2 Test Migrations

- [x] 11.1 Create H2-compatible version of V5 migration at
      `services/meeting-management/src/test/resources/db/h2-migration/V5__add_description_and_invitees.sql`
      (H2 syntax for UUID default, check constraints)

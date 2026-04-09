# Tasks

## 1. Domain Model — AdmissionPolicy & MeetingSettings

- [x] 1.1 Create `AdmissionPolicy.java` enum in `domain/model/` with values
      `ALLOW_ALL` and `MANUAL_APPROVAL`
- [x] 1.2 Update `MeetingSettings.java`: replace `boolean requiredApproval` with
      `AdmissionPolicy admissionPolicy`, add
      `@Nullable Duration joinRequestTimeout`, update `defaults()` to use
      `MANUAL_APPROVAL` and `Duration.ofMinutes(5)`
- [x] 1.3 Create `JoinRequestId.java` value object in
      `domain/model/valueobject/` wrapping UUID
- [x] 1.4 Create `JoinRequestStatus.java` enum in `domain/model/` with values
      `PENDING`, `APPROVED`, `DENIED`, `EXPIRED`
- [x] 1.5 Create `JoinRequest.java` aggregate in `domain/model/` (fields: id,
      meetingId, userId nullable, displayName, deviceId, status, requestedAt,
      expiresAt) with `approve()`, `deny()`, `expire()` methods

## 2. Domain — Events & Errors

- [x] 2.1 Create `JoinRequestCreatedEvent.java` in `domain/event/` implementing
      `PublishableEvent` (fields: eventId, meetingId, joinRequestId, userId
      nullable, displayName, deviceId, occurredAt)
- [x] 2.2 Create `JoinRequestApprovedEvent.java` in `domain/event/` implementing
      `PublishableEvent` (fields: eventId, meetingId, joinRequestId, approvedBy,
      occurredAt)
- [x] 2.3 Create `JoinRequestDeniedEvent.java` in `domain/event/` implementing
      `PublishableEvent` (fields: eventId, meetingId, joinRequestId, deniedBy
      nullable, occurredAt)
- [x] 2.4 Create `JoinRequestExpiredEvent.java` in `domain/event/` implementing
      `PublishableEvent` (fields: eventId, meetingId, joinRequestId, occurredAt)
- [x] 2.5 Add error records to `MeetingError.java`:
      `JoinRequestNotFound(UUID meetingId, UUID requestId)`,
      `JoinRequestExpired(UUID meetingId, UUID requestId)`,
      `InvalidJoinRequestTransition(JoinRequestStatus from, JoinRequestStatus to)`,
      `NotWaitingForApproval(UUID meetingId)`
- [x] 2.6 Add error codes to `MeetingErrorCode.java`: `JOIN_REQUEST_NOT_FOUND`,
      `JOIN_REQUEST_EXPIRED`, `INVALID_JOIN_REQUEST_TRANSITION`,
      `NOT_WAITING_FOR_APPROVAL`

## 3. Domain — Port

- [x] 3.1 Create `JoinRequestRepository.java` port in `domain/port/` with
      methods: `save(JoinRequest, Duration)`, `findById(UUID)`,
      `findByDeviceId(UUID meetingId, String deviceId)`,
      `findPendingByMeetingId(UUID)`, `updateStatus(UUID, JoinRequestStatus)`,
      `removeFromQueue(UUID meetingId, UUID requestId)`,
      `deleteAllByMeetingId(UUID)`

## 4. Infrastructure — Redis Configuration

- [x] 4.1 Create `RedisConfig.java` in `infrastructure/config/`: define
      `StringRedisTemplate` bean with `StringRedisSerializer` for both key and
      value, define `RedisMessageListenerContainer` bean using
      `RedisConnectionFactory`
- [x] 4.2 Create `JoinRequestRedisRepository.java` in
      `infrastructure/persistence/` implementing `JoinRequestRepository` port —
      `save()`: `ZADD join_request:{meetingId}` (score=expiresAt ms) +
      `HSET join_request_meta:{requestId}` +
      `SET join_request_device:{meetingId}:{deviceId}` with `EXPIRE`;
      `findById()`: `HGETALL`; `findByDeviceId()`:
      `GET join_request_device:{meetingId}:{deviceId}` then `HGETALL`;
      `findPendingByMeetingId()`: `ZRANGE` then batch `HGETALL`;
      `updateStatus()`: `HSET status`; `removeFromQueue()`: `ZREM` +
      `DEL meta` + `DEL device index`; `deleteAllByMeetingId()`: `ZRANGE` then
      delete all meta/device keys + `DEL` queue

## 5. Infrastructure — SSE Manager

- [x] 5.1 Create `MeetingSseManager.java` in `infrastructure/sse/`: holds
      `ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>`, registers
      `MessageListenerAdapter` on `meeting:*:events` channel, broadcasts Redis
      messages to matching emitters; `subscribe(UUID meetingId, UUID userId)`
      creates `SseEmitter(300_000L)` with `onCompletion`/`onTimeout` cleanup
- [x] 5.2 Create `RedisSseEventPublisher.java` in `infrastructure/sse/`: injects
      `StringRedisTemplate`, publishes JSON payload to
      `meeting:{meetingId}:events` channel; create `SseEvent` record (type, data
      as Object) for payload structure
- [x] 5.3 Create `JoinRequestCleanupJob.java` in `infrastructure/jobs/`:
      `@Scheduled(fixedDelay = 60_000)` — scan `ZRANGEBYSCORE join_request:*`
      with score < now, for each expired entry update status to `EXPIRED`,
      publish `JoinRequestExpiredEvent` to Redis Pub/Sub, and remove from queue

## 6. Persistence Layer — MeetingSettings Mapping

- [x] 6.1 Update `MeetingSettingsJson.java`: replace `boolean waitingRoom` with
      `String admissionPolicy`, add
      `@Nullable Integer joinRequestTimeoutSeconds`
- [x] 6.2 Update `MeetingRepositoryAdapter.toDomain()` (line 105): map
      `e.getSettings().admissionPolicy()` → `AdmissionPolicy.valueOf(...)` and
      `joinRequestTimeoutSeconds` → `Duration.ofSeconds(...)`
- [x] 6.3 Update `MeetingRepositoryAdapter.toEntity()` (line 139): map
      `m.getSettings().admissionPolicy().name()` and
      `joinRequestTimeout().toSeconds()`

## 7. Application — Commands & Responses

- [x] 7.1 Create `RequestJoinCommand.java` record:
      `(UUID meetingId, @Nullable UUID userId, String displayName, String deviceId, @Nullable String password)`
- [x] 7.2 Create `ApproveJoinRequestCommand.java` record:
      `(UUID meetingId, UUID requestId, UUID approvedBy)`
- [x] 7.3 Create `DenyJoinRequestCommand.java` record:
      `(UUID meetingId, UUID requestId, UUID deniedBy)`
- [x] 7.4 Create `ApproveAllJoinRequestsCommand.java` record:
      `(UUID meetingId, UUID approvedBy)`
- [x] 7.5 Create `RequestJoinResponse.java` record:
      `(UUID requestId, JoinRequestStatus status, @Nullable String token, @Nullable String roomName)`
- [x] 7.6 Create `JoinRequestResponse.java` record:
      `(UUID id, UUID meetingId, @Nullable UUID userId, String displayName, JoinRequestStatus status, Instant requestedAt, Instant expiresAt)`
- [x] 7.7 Create `ApproveAllResponse.java` record: `(int approvedCount)`

## 8. Application — Use Cases

- [x] 8.1 Create `RequestJoinUseCase.java`: load meeting (findByIdWithLock),
      validate LIVE status, check `allowGuest` for null userId, check password
      if protected, if `ALLOW_ALL` → generate token immediately (return APPROVED
      response); if `MANUAL_APPROVAL` → check duplicate via
      `findByDeviceId`/userId, create `JoinRequest`, save to Redis, publish
      `JoinRequestCreatedEvent` to Redis Pub/Sub and via
      ApplicationEventPublisher, return 202 PENDING response
- [x] 8.2 Create `ApproveJoinRequestUseCase.java`: load meeting, validate
      `isHost(approvedBy)`, load `JoinRequest` from Redis (`findById`), guard
      invalid transitions (DENIED → 409), generate LiveKit token, update status
      to `APPROVED`, publish `JoinRequestApprovedEvent` to Redis Pub/Sub, remove
      from queue, return token
- [x] 8.3 Create `DenyJoinRequestUseCase.java`: load meeting, validate
      `isHost(deniedBy)`, load `JoinRequest`, guard invalid transitions
      (APPROVED → 409), update status to `DENIED`, publish
      `JoinRequestDeniedEvent` to Redis Pub/Sub, remove from queue
- [x] 8.4 Create `ApproveAllJoinRequestsUseCase.java`: load meeting, validate
      `isHost`, load all pending requests via `findPendingByMeetingId`, for
      each: generate token + update status + publish event; clear queue; return
      `ApproveAllResponse(count)`
- [x] 8.5 Create `ListJoinRequestsUseCase.java`: load meeting, validate
      `isHost`, call `findPendingByMeetingId`, map to `JoinRequestDto` list
      ordered by `requestedAt` ascending
- [x] 8.6 Create `PollJoinRequestStatusUseCase.java`: call
      `findById(requestId)`, if empty return `JoinRequestNotFound`; if
      `APPROVED` generate new LiveKit token and return full response; else
      return status-only response
- [x] 8.7 Update `CreateInstantMeetingUseCase.java`: set default
      `admissionPolicy = MANUAL_APPROVAL` and
      `joinRequestTimeout = Duration.ofMinutes(5)` in settings construction
- [x] 8.8 Update `ScheduleMeetingUseCase.java`: pass `admissionPolicy` and
      `joinRequestTimeout` from command through to `MeetingSettings`

## 9. Application — Event Handler

- [x] 9.1 Create `MeetingEndedJoinRequestHandler.java` in `application/usecase/`
      (or `infrastructure/events/`):
      `@TransactionalEventListener(phase = AFTER_COMMIT)` on `MeetingEndedEvent`
      — call `deleteAllByMeetingId`, for each pending request publish
      `JoinRequestDeniedEvent` to Redis Pub/Sub

## 10. Presentation — API Updates

- [x] 10.1 Update `MeetingSettingsRequest.java`: replace `boolean waitingRoom`
      with
      `@NotBlank @Pattern(regexp = "ALLOW_ALL|MANUAL_APPROVAL") String admissionPolicy`,
      add `@Nullable @Min(30) @Max(600) Integer joinRequestTimeoutSeconds`;
      update `toDomain()` accordingly
- [x] 10.2 Update `MeetingSettingsResponse.java`: replace `boolean waitingRoom`
      with `String admissionPolicy`, add `Integer joinRequestTimeoutSeconds`;
      update `from()` factory method
- [x] 10.3 Create `JoinRequestRequest.java` request DTO (same fields as
      `JoinMeetingRequest` — `displayName`, `deviceId`, `password`); add
      `toCommand(UUID meetingId, @Nullable UUID userId)` method
- [x] 10.4 Create `JoinRequestController.java` extending `BaseController`:
      implement `POST /v1.0/meetings/{id}:requestJoin`,
      `GET /v1.0/meetings/{id}/joinRequests`,
      `POST /v1.0/meetings/{id}/joinRequests/{requestId}:approve`,
      `POST /v1.0/meetings/{id}/joinRequests/{requestId}:deny`,
      `POST /v1.0/meetings/{id}/joinRequests:approveAll`,
      `GET /v1.0/joinRequests/{requestId}` (no auth required),
      `GET /v1.0/meetings/{id}/events` (SSE)
- [x] 10.5 Update `BaseController.errorResponse()`: add cases for
      `JoinRequestNotFound` (404), `JoinRequestExpired` (410),
      `InvalidJoinRequestTransition` (409), `NotWaitingForApproval` (422)

## 11. Tests

- [ ] 11.1 Unit test `RequestJoinUseCase`: scenarios — ALLOW_ALL generates token
      immediately; MANUAL_APPROVAL creates pending request; duplicate request
      returns existing; guest denied when `allowGuest=false`; invalid password
      rejected; non-live meeting rejected
- [ ] 11.2 Unit test `ApproveJoinRequestUseCase`: approve PENDING succeeds;
      approve already-APPROVED is idempotent; approve DENIED returns
      `InvalidJoinRequestTransition`; non-host returns `NotAuthorized`
- [ ] 11.3 Unit test `DenyJoinRequestUseCase`: deny PENDING succeeds; deny
      already-DENIED is idempotent; deny APPROVED returns
      `InvalidJoinRequestTransition`
- [ ] 11.4 Unit test `ApproveAllJoinRequestsUseCase`: approves all pending; zero
      pending returns count 0
- [ ] 11.5 Unit test `MeetingEndedJoinRequestHandler`: all pending requests
      denied on meeting end; no-op when queue empty
- [ ] 11.6 Unit test `JoinRequestRedisRepository`:
      save/findById/updateStatus/removeFromQueue/deleteAllByMeetingId with
      embedded Redis (Testcontainers or `embedded-redis`)
- [ ] 11.7 Unit test `JoinRequestCleanupJob`: expired entries removed and events
      published
- [ ] 11.8 Integration test (`@SpringBootTest`) for full flow:
      `POST :requestJoin` (MANUAL_APPROVAL) → `GET /joinRequests/{id}` returns
      PENDING → `POST :approve` → `GET /joinRequests/{id}` returns APPROVED with
      token
- [ ] 11.9 Integration test for SSE: host connects to `/meetings/{id}/events`,
      participant posts join request, host SSE receives `join_request_created`
      event

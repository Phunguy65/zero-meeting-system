# Proposal: Chat-in-Meeting Service

## Summary

Triển khai `chat-management` service để cung cấp tính năng nhắn tin trong cuộc
họp. Chat chỉ hoạt động khi cuộc họp đang diễn ra (LIVE), message được lưu vào
MongoDB và broadcast real-time qua **LiveKit Data Messages** tới tất cả
participants đang ở trong LiveKit room.

---

## Context

Zero Meeting System hiện có:

- `meeting-management`: quản lý lifecycle cuộc họp (schedule → live → ended)
- `shared`: shared domain primitives (Result, DomainError, CursorPageResponse,
  JSend)
- LiveKit: video/audio conferencing platform

Thiếu: tính năng chat giữa participants trong lúc họp.

---

## Goals

1. Participants có thể gửi/nhận message trong lúc họp (meeting status = LIVE)
2. Message history được lưu trong MongoDB, tự động xóa sau 30 ngày (TTL)
3. Real-time delivery qua LiveKit Data Messages (không cần WebSocket riêng)
4. System messages tự động khi participant join/leave/kick
5. Rate limiting: tối đa 10 message/phút/user
6. Content length limit: 4000 ký tự

---

## Key Decisions

| Decision            | Choice                                   | Rationale                                                       |
| ------------------- | ---------------------------------------- | --------------------------------------------------------------- |
| Real-time delivery  | LiveKit Data Messages                    | Tận dụng existing LiveKit connection, tách biệt với WebSocket   |
| Message ordering    | MongoDB findAndModify counter (seqNum)   | Atomic $inc, monotonic per room, distributed-safe               |
| JWT validation      | Local JWT (jjwt)                         | Không cần gRPC call sang service khác                           |
| @Document placement | Keep in domain (fix ArchUnit rule)       | @Document không phức tạp như @Entity; fix CleanArchitectureTest |
| Participant events  | Thêm Kafka events vào meeting-management | Join/leave/kicked → system messages                             |

---

## Architecture Notes

### Kafka Consumer Groups

Chat-management KHÔNG phải SSE service — mỗi instance phải nhận mọi event (để xử
lý chat). Do đó dùng **fixed group ID** (vd: `chat-management-meeting`,
`chat-management-participant`), KHÔNG dùng `UUID.randomUUID()` như SSE pattern
của meeting-management.

### Event Publishing in meeting-management

`ParticipantJoinedEvent` và `ParticipantLeftEvent` được publish từ
`LiveKitWebhookController` (B-layer) sau khi
`AssignSidUseCase`/`LeaveMeetingUseCase` xử lý thành công, thông qua
`EventPublisher` (Kafka, KHÔNG phải `ApplicationEventPublisher`).

---

## Scope

### In Scope

- Chat REST API: send message, get message history (cursor pagination), get room
  info
- Kafka consumers: listen `meeting-management.meeting.started`, `.ended`,
  `.participant_joined`, `.participant_left`, `.participant_kicked`
- LiveKit Data Message broadcast
- MongoDB persistence (TTL 30 days)
- JWT authentication filter
- Unit + integration tests
- ArchUnit clean architecture tests

### Out of Scope

- Room settings (maxMessageLength, rateLimit configurable) — hardcoded constants
- WebSocket/SSE endpoints — LiveKit Data Messages cover real-time
- Typing indicators
- Message reactions
- File attachments
- Cross-room messaging
- Message editing

---

## Technical Constraints

- **Chat chỉ hoạt động khi meeting = LIVE** — gửi message khi meeting ended →
  `Unauthorized`
- **Idempotent room creation** — `MeetingStartedEvent` có thể được deliver nhiều
  lần → room creation phải idempotent
- **LiveKit payload limit**: 15 KB/message (RELIABLE mode)
- **SDK 0.12.1 KNPE bug**: `sendData().execute()` throw KNPE dù message đã gửi
  thành công → handle gracefully
- **Data source of truth**: MongoDB (LiveKit chỉ broadcast, không lưu)

---

## Risks & Mitigations

| Risk                      | Likelihood | Impact | Mitigation                                    |
| ------------------------- | ---------- | ------ | --------------------------------------------- |
| SDK 0.12.1 KNPE bug       | High       | Low    | Catch KNPE, treat as success                  |
| Duplicate Kafka events    | Medium     | Low    | Idempotent use cases                          |
| Late joiners miss history | Medium     | Low    | GET /rooms/{id}/messages on join              |
| Large message >15KB       | Low        | Medium | Reject at validation layer (4K limit << 15KB) |

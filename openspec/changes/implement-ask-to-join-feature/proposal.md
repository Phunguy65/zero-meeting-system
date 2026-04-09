# Why

The current meeting join flow allows anyone with the meeting link to join
immediately if the meeting is live, creating security and privacy concerns.
Users need control over who can enter their meetings, similar to Google Meet's
"Ask to Join" feature. This enables hosts to review and approve participants
before they enter, preventing unwanted interruptions and maintaining meeting
security.

## What Changes

- Replace boolean `requiredApproval` field with extensible `AdmissionPolicy`
  enum (ALLOW_ALL, MANUAL_APPROVAL) to support future trust levels
- Add Redis-backed join request queue for temporary storage of pending join
  requests
- Implement real-time host notifications via Server-Sent Events (SSE) for join
  request updates
- Add polling endpoint for participants to check join request status
- Implement configurable join request timeout per meeting (default 5 minutes)
- Auto-deny pending join requests when meeting ends
- Support guest join requests identified by deviceId
- Add Google-style batch approval endpoint (`:approveAll`)

## Capabilities

### New Capabilities

- `join-request-management`: Handle participant join requests with
  approval/denial workflow, including creation, status tracking, host
  approval/denial, batch operations, and automatic cleanup
- `admission-policy`: Control meeting access through configurable admission
  policies (ALLOW_ALL, MANUAL_APPROVAL) with support for future trust levels
- `sse-notifications`: Real-time server-sent events for host notifications about
  join requests and meeting events

### Modified Capabilities

- `meeting-settings`: Add `admissionPolicy` enum and `joinRequestTimeout`
  duration to meeting configuration, replacing the boolean `requiredApproval`
  field
- `meeting-join`: Update join flow to check admission policy and create join
  requests when manual approval is required

## Impact

**Backend Services:**

- `meeting-management`: Core changes to domain model, use cases, and API
  endpoints
    - Domain: New `JoinRequest` aggregate, `AdmissionPolicy` enum,
      `JoinRequestStatus` enum
    - Infrastructure: New Redis Pub/Sub configuration, SSE manager, Redis
      repository
    - Application: 6 new use cases (RequestJoin, ApproveJoinRequest,
      DenyJoinRequest, ApproveAll, ListJoinRequests, PollStatus)
    - Presentation: 7 new API endpoints

**Data Storage:**

- Redis: New data structures for join request queue (Sorted Set) and metadata
  (Hash)
- PostgreSQL: JSONB field change in `meetings.settings` (dev environment, no
  migration needed)

**APIs:**

- New endpoints: `POST /meetings/{id}:requestJoin`,
  `GET /meetings/{id}/joinRequests`,
  `POST /meetings/{id}/joinRequests/{reqId}:approve`,
  `POST /meetings/{id}/joinRequests/{reqId}:deny`,
  `POST /meetings/{id}/joinRequests:approveAll`, `GET /joinRequests/{reqId}`,
  `GET /meetings/{id}/events` (SSE)
- Modified: `POST /meetings/{id}:join` behavior changes based on admission
  policy

**Dependencies:**

- Spring Data Redis (already present)
- Redis Pub/Sub infrastructure (new configuration needed)

**Breaking Changes:**

- **BREAKING**: `MeetingSettings` API contract changes from
  `waitingRoom: boolean` to `admissionPolicy: string` enum
- **BREAKING**: Instant meetings default to `MANUAL_APPROVAL` instead of
  allowing all

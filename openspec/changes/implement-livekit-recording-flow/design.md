# Context

`meeting-management` already exposes recording start, stop, get, and list
endpoints, and its domain model already understands a webhook-driven lifecycle
through `PENDING`, `RECORDING`, `COMPLETED`, and `FAILED`. The missing piece is
the integration layer: the service does not call LiveKit Egress to start or stop
real recordings, it does not handle `egress_started` or `egress_ended` webhooks,
and its current stop path deletes the whole LiveKit room. The Kubernetes repo
also contains RustFS storage and LiveKit Helm values, but no explicit webhook
route for LiveKit to reach `meeting-management` through Kong.

This change spans application logic, infrastructure adapters, webhook handling,
cleanup jobs, and Kubernetes routing. It also carries security and operational
constraints: webhook authenticity must be verified, stop-recording semantics
must match a Google Meet-like user experience, and stale recording sessions must
not remain indefinitely in `PENDING`.

## Goals / Non-Goals

**Goals:**

- Implement a real end-to-end meeting recording flow backed by LiveKit Egress
  and RustFS.
- Preserve Google Meet-like behavior: stopping recording stops only egress,
  while ending a meeting stops active egress and then closes the room.
- Drive recording state from webhook-confirmed lifecycle transitions rather than
  optimistic local assumptions.
- Add stale recording cleanup for sessions that never leave `PENDING`.
- Add Kubernetes routing/configuration required for LiveKit webhook delivery.

**Non-Goals:**

- Deploy LiveKit Server or LiveKit Egress as part of this change.
- Build a new user-facing recording UI or change client-side recording controls.
- Introduce transcript generation, thumbnails beyond existing metadata fields,
  or recording post-processing pipelines.
- Solve long-running recovery for every possible LiveKit infrastructure outage
  beyond the defined stale-session cleanup path.

## Decisions

### 1. Use webhook-driven recording state transitions

The service will continue creating a local recording aggregate first, but
authoritative state changes will come from LiveKit webhooks.

- `StartRecordingUseCase` SHALL create a recording and invoke LiveKit Egress
  start.
- `egress_started` SHALL move the recording into `RECORDING`.
- `egress_ended` SHALL either complete the recording with file metadata or fail
  it when LiveKit reports an error.

This matches the existing domain model and avoids treating a requested egress
session as if it were already running.

Alternative considered:

- Mark the recording as `RECORDING` immediately after the start API call.
  Rejected because it hides startup failures and contradicts the existing
  aggregate lifecycle.

### 2. Separate room termination from recording termination

Stopping recording will call `stopEgress(egressId)` and SHALL NOT delete the
room. Ending a meeting will stop any active egress first, then delete the
LiveKit room.

This preserves the agreed Google Meet-like behavior and fixes the current bug
where stop-recording destroys the whole meeting.

Alternative considered:

- Rely on room deletion alone to terminate recording. Rejected because it
  couples two user actions with different meanings and makes recording
  completion less explicit.

### 3. Store recordings under meeting-scoped S3 paths

The output path prefix will be `meetings/{meetingId}/`, with the final object
name using the egress identifier produced by LiveKit. The stored `storagePath`
in the service will therefore be treated as
`meetings/{meetingId}/{egressId}.mp4` or the exact object path returned by
LiveKit in the completion webhook.

Alternative considered:

- Use a timestamp-based file name. Rejected because LiveKit Egress does not
  provide a clean timestamp template for exact object naming in this flow.

### 4. Verify webhook authenticity in application code

Webhook requests will continue to be accepted through a public Kong route, but
authenticity will be enforced in `meeting-management` using LiveKit's HMAC
verification with the shared API secret.

This keeps the verification close to the raw webhook payload and avoids adding a
second auth mechanism that LiveKit does not naturally provide.

Alternative considered:

- Kong key-auth in front of the webhook endpoint. Rejected because the chosen
  design standardizes on LiveKit's signed webhook model.

### 5. Clean up stale `PENDING` recordings with a scheduled job

The service will add a scheduled cleanup job that finds recordings stuck in
`PENDING` for more than 7 minutes and marks them as `FAILED`.

Seven minutes is intentionally more conservative than the initial five-minute
idea to reduce the chance of racing slightly delayed `egress_started` webhooks.

Alternative considered:

- No cleanup job. Rejected because stale rows would accumulate indefinitely and
  block retries.

### 6. Extend the existing LiveKit adapter instead of introducing a second external-service adapter

The current code already centralizes LiveKit integration behind `LiveKitPort`
and `LiveKitAdapter`. Egress start/stop operations will be added there by wiring
both `RoomServiceClient` and `EgressClient` from the same config module.

This keeps the dependency shape consistent with the service's current hexagonal
pattern.

## Risks / Trade-offs

- `PENDING` cleanup can race with delayed webhooks -> Use a 7-minute cutoff and
  make webhook handlers idempotent.
- LiveKit may retry webhook delivery on non-2xx responses -> Always return
  success once signature verification passes, while logging business-level
  mismatches.
- Object names are partially controlled by LiveKit -> Persist the final storage
  path returned by webhook payload instead of reconstructing it later.
- Self-hosted egress is CPU-intensive -> Keep egress deployment and capacity
  outside this change's scope, but preserve clear operational config in
  manifests.
- Secrets currently appear in multiple infra locations -> Keep the change
  compatible with the current setup, but route new configuration through
  Kubernetes secrets where possible.

## Migration Plan

1. Add Java-side egress integration, webhook handlers, repository query support,
   and stale cleanup.
2. Add Kong HTTPRoute and meeting-management environment configuration for
   webhook delivery.
3. Update LiveKit values to accept environment-specific webhook URL overrides
   without hardcoding production endpoints.
4. Deploy application changes before enabling real egress traffic so webhook
   handling is already in place.
5. Deploy or enable LiveKit Server and LiveKit Egress separately using the
   prepared values.
6. Roll back by reverting application and route changes; existing completed
   recording rows remain valid because this change is additive to persisted
   recording metadata.

## Open Questions

- None for this change. Deployment of LiveKit Server and Egress remains
  intentionally outside scope, but the required runtime configuration is part of
  the design.

# Why

The current meeting recording flow stops at creating a local `PENDING` record
and never drives a real LiveKit egress session, so recordings do not reliably
start, stop, complete, or persist media output. This change is needed now
because the project already has most of the domain model, RustFS storage, and
Kubernetes infrastructure shape in place, but the end-to-end recording contract
is incomplete and currently breaks expected meeting behavior.

## What Changes

- Add a complete server-side recording lifecycle for meetings using self-hosted
  LiveKit Egress and RustFS-backed S3 output.
- Update recording start and stop behavior so stopping a recording only stops
  egress, while ending a meeting stops any active egress and then closes the
  room.
- Handle `egress_started` and `egress_ended` LiveKit webhooks to drive recording
  state transitions, completion metadata, and failure handling.
- Add cleanup for stale recording sessions that never successfully transition
  out of `PENDING`.
- Add Kubernetes routing and configuration needed for LiveKit webhook delivery
  into `meeting-management`.

## Capabilities

### New Capabilities

- `meeting-recording`: End-to-end meeting recording lifecycle with LiveKit
  Egress, webhook-driven state updates, RustFS output storage, and cleanup of
  stale recording sessions.

### Modified Capabilities

None.

## Impact

- Affected services: `services/meeting-management`, `services/k8s/livekit`,
  `services/k8s/kong`, `services/k8s/services/meeting-management.yaml`
- Affected APIs: meeting recording start/stop endpoints, LiveKit webhook
  endpoint behavior
- Affected integrations: LiveKit Server SDK / Egress client, Kong HTTPRoute,
  RustFS S3-compatible storage
- Affected dependencies: existing `io.livekit:livekit-server` integration will
  be expanded to cover egress operations

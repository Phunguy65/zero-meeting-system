## [2026-04-02] Round 1 (from spx-apply auto-verify)

### spx-arch-verifier

- Fixed: Reworked `StartRecordingUseCase` to persist the recording before
  starting egress, mark persisted rows failed when egress startup fails, and
  stop egress on post-start persistence failure.
- Fixed: Made `StopRecordingUseCase` treat LiveKit `HTTP 404` stop responses as
  idempotent success for already-stopped egress sessions.
- Fixed: Lowered benign missing-recording webhook logs to debug in
  `ActivateRecordingUseCase` and `FinalizeRecordingUseCase`, and removed
  redundant egress-id reassignment calls.
- Fixed: Replaced the recording filepath placeholder in `LiveKitAdapter` with a
  literal-safe meeting-scoped filename.
- Fixed: Added explicit shared OkHttp timeouts and retry-on-connection-failure
  configuration for LiveKit room and egress clients in `LiveKitConfig`.
- Fixed: Removed insecure default recording storage credentials from
  `LiveKitProperties` so runtime secrets must be supplied explicitly.

### spx-test-verifier

- Fixed: Expanded `RecordingFlowUseCaseTest` to cover missing-meeting,
  authorization, status validation, duplicate-active, no-active-recording,
  idempotent stop, and delete-room failure scenarios.
- Fixed: Expanded `RecordingWebhookUseCaseTest` to cover the normal
  `egress_started` activation path, missing-recording no-ops, and both pending
  and already-recording finalize flows.
- Fixed: Expanded `RecordingCleanupJobTest` to verify recent pending recordings
  are preserved and not published as failures.

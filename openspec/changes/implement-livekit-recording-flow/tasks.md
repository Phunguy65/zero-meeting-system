# Tasks

## 1. LiveKit recording integration setup

- [x] 1.1 Enable scheduling in `meeting-management` and add any missing LiveKit
      recording configuration properties.
- [x] 1.2 Extend LiveKit configuration to expose both the existing room client
      and a new egress client bean.
- [x] 1.3 Extend `LiveKitPort` and `LiveKitAdapter` with room-composite egress
      start and stop operations.

## 2. Recording application flow

- [x] 2.1 Update `StartRecordingUseCase` to start LiveKit Egress and persist
      egress-aware recording state.
- [x] 2.2 Update `StopRecordingUseCase` so stopping a recording stops egress
      without deleting the room.
- [x] 2.3 Update `EndMeetingUseCase` so ending a meeting stops active egress
      before deleting the LiveKit room.

## 3. Recording lifecycle persistence and cleanup

- [x] 3.1 Add any missing repository query support for stale `PENDING`
      recordings and egress-driven lookups.
- [x] 3.2 Add a scheduled cleanup job that fails recordings stuck in `PENDING`
      for more than 7 minutes.
- [x] 3.3 Ensure recording completion and failure metadata persist the final
      webhook-reported storage path and error details.

## 4. Webhook processing

- [x] 4.1 Extend `LiveKitWebhookController` to handle `egress_started` events
      idempotently.
- [x] 4.2 Extend `LiveKitWebhookController` to handle `egress_ended` success and
      failure paths idempotently.
- [x] 4.3 Preserve and document HMAC verification behavior for recording webhook
      processing.

## 5. Kubernetes routing and runtime configuration

- [x] 5.1 Add a Kong `HTTPRoute` for the LiveKit webhook endpoint to reach
      `meeting-management`.
- [x] 5.2 Add meeting-management runtime configuration for environment-specific
      LiveKit webhook URLs.
- [x] 5.3 Update LiveKit and egress values so webhook URLs and storage
      credentials can be supplied safely per environment.

## 6. Verification coverage

- [x] 6.1 Add or update tests for recording start/stop/end use cases with egress
      interactions.
- [x] 6.2 Add or update tests for `egress_started` and `egress_ended` webhook
      handling, including failure and duplicate delivery.
- [x] 6.3 Add or update tests for stale `PENDING` recording cleanup.

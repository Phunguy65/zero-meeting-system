# Why

Hosts currently cannot remove disruptive participants from an active meeting
once they have joined the LiveKit room. This leaves moderation entirely to
social convention and makes it hard to recover from abuse, accidental joins from
multiple devices, or participants who should no longer remain in the room.

## What Changes

- Add a host-only API to kick an active participant from a meeting.
- Support kicking registered users by `userId` across all active devices in the
  same meeting.
- Support kicking guests by `displayName` across all active guest sessions in
  the same meeting.
- Disconnect kicked participants through the LiveKit room service and rely on
  the existing `participant_left` webhook flow to close participation logs.
- Publish a participant-kicked event and fan it out to host SSE subscribers so
  moderation views can refresh in real time.
- Return explicit validation and authorization errors for unsupported kick
  attempts such as kicking the host or kicking someone not currently in the
  room.

## Capabilities

### New Capabilities

- `participant-moderation`: Allow a meeting host to forcibly remove active
  participants from a live meeting and propagate the resulting moderation event
  to host-facing realtime streams.

### Modified Capabilities

- None.

## Impact

- Affected code: `ParticipantController`, participation-log repository and
  persistence adapter, `LiveKitPort`/`LiveKitAdapter`, `MeetingSseManager`,
  webhook-driven leave flow integration, error mapping in web controllers.
- Affected APIs: new moderation endpoint under the meeting participants API; new
  SSE event for host observers.
- Affected systems: LiveKit room administration, Kafka-backed SSE fan-out,
  participant tracking in meeting-management.

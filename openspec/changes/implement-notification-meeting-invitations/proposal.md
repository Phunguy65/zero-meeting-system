# Why

The system can persist meeting invitees during scheduling, but it does not yet
deliver invitation emails to those recipients. We need a dedicated notification
flow now so scheduled meetings actually reach attendees, including
password-protected meetings without forcing hosts to manually share the password
out-of-band.

## What Changes

- Add a new `notification` service flow that consumes
  `meeting-management.meeting.invitations-sent` CloudEvents from Kafka and sends
  invitation emails through Resend.
- Add HTML invitation email rendering with meeting title, start time, join
  call-to-action, and password-protected meeting guidance that supports
  one-click join.
- Enrich the `MeetingInvitationsSentEvent` payload from `meeting-management`
  with the fields required to build a join link for invitees, including the
  meeting short code and password-delivery data.
- Add notification-service configuration for Resend credentials, sender
  identity, and frontend join base URL using `@ConfigurationProperties`.
- Add Kafka topic infrastructure and test coverage for invitation event
  consumption, email rendering, and password-protected invitation scenarios.

## Capabilities

### New Capabilities

- `meeting-invitation-email-notifications`: Send transactional invitation emails
  for scheduled meetings by consuming meeting invitation events and rendering
  Resend-backed HTML emails, including password-protected one-click join links.

### Modified Capabilities

- None.

## Impact

- Affected code: `services/notification/**`, `services/meeting-management/**`,
  `services/k8s/kafka/kafka-topics.yaml`, `gradle/libs.versions.toml`
- Systems: Kafka CloudEvents consumer flow, Resend transactional email delivery,
  frontend join-link contract
- Dependencies: add Resend Java SDK; remove irrelevant starter/dependency usage
  from the notification service scaffold
- Behavioral impact: invitees will receive meeting emails automatically, and
  password-protected meetings will no longer depend on hosts manually sharing
  the password

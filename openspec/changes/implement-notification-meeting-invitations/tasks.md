# Tasks

## 1. Notification Module Setup

- [x] 1.1 Clean `services/notification/build.gradle.kts` so it reflects a
      notification consumer service and add the Resend Java SDK dependency in
      `gradle/libs.versions.toml`
- [x] 1.2 Create the notification service application entry point, package
      structure, and `application.properties` with typed Resend and join-link
      configuration
- [x] 1.3 Add notification-service Kafka consumer configuration for CloudEvents
      with a fixed consumer group for invitation delivery

## 2. Meeting Invitation Event Contract

- [x] 2.1 Extend `MeetingInvitationsSentEvent` with the meeting short code and
      protected-meeting password-delivery data needed by downstream email
      rendering
- [x] 2.2 Update `ScheduleMeetingUseCase` so the published invitation event
      contains the enriched join-link fields for both protected and unprotected
      meetings
- [x] 2.3 Ensure invitation event publishing and related logging avoid exposing
      raw password values or rendered protected join links

## 3. Notification Email Delivery Flow

- [x] 3.1 Implement notification-service configuration classes for Resend client
      initialization via `@ConfigurationProperties`
- [x] 3.2 Implement the invitation email send use case and a Resend-backed email
      sender adapter
- [x] 3.3 Implement the Kafka invitation consumer that deserializes
      `meeting-management.meeting.invitations-sent` CloudEvents and triggers one
      send per invitee

## 4. HTML Invitation Rendering

- [x] 4.1 Implement a deterministic HTML invitation renderer with required
      meeting metadata, CTA content, and fallback copy for missing optional
      fields
- [x] 4.2 Implement join-link construction for unprotected meetings using the
      configured frontend base URL and meeting short code
- [x] 4.3 Implement embedded-password join-link construction for
      password-protected meetings without exposing the raw password in logs

## 5. Infrastructure and Verification

- [x] 5.1 Add the Kafka topic manifest for
      `meeting-management.meeting.invitations-sent` with bounded retention
      appropriate for sensitive invitation payloads
- [x] 5.2 Add tests for enriched invitation event publishing, Kafka consumer
      deserialization, Resend send orchestration, and HTML rendering fallbacks
- [x] 5.3 Add tests for password-protected invitation behavior, including
      embedded join links and sensitive-data logging guards
- [x] 5.4 Run notification and meeting-management verification commands and
      confirm the change is ready for implementation review

# ADDED Requirements

## Requirement: Scheduled meeting invitations SHALL be delivered by email

When a scheduled meeting is created with one or more invitees, the system SHALL
publish an invitation event that the `notification` service consumes to send one
invitation email to each invitee email address.

### Scenario: Invitation event triggers one email per invitee

- **WHEN** `meeting-management` publishes
  `meeting-management.meeting.invitations-sent` with multiple invitees
- **THEN** `notification` SHALL send one invitation email to each invitee email
  address in the event payload

### Scenario: Empty invitee lists do not send email

- **WHEN** a meeting is created without invitees
- **THEN** the system SHALL NOT publish or process invitation email work for
  that meeting

## Requirement: Invitation events SHALL include join-link data required by downstream email delivery

The published invitation event SHALL include the meeting short code and the
password-delivery data needed to construct the invitee join link without
additional service lookups.

### Scenario: Non-protected meetings publish join data without password

- **WHEN** a meeting invitation event is published for a meeting that is not
  password protected
- **THEN** the event SHALL include the meeting short code and SHALL omit any raw
  password value

### Scenario: Password-protected meetings publish join data with password

- **WHEN** a meeting invitation event is published for a password-protected
  meeting
- **THEN** the event SHALL include the meeting short code and the raw password
  value needed to build the protected join link

## Requirement: Invitation emails SHALL provide one-click join for password-protected meetings

The notification service SHALL render invitation emails with a primary join link
built from the configured frontend join base URL and the event payload so
invitees can join password-protected meetings without manually requesting the
password from the host.

### Scenario: Protected meeting email contains embedded-password join link

- **WHEN** the notification service renders an email for a password-protected
  meeting invitation
- **THEN** the email SHALL contain a join link that includes both the meeting
  short code and the raw password from the invitation event

### Scenario: Unprotected meeting email contains standard join link

- **WHEN** the notification service renders an email for a meeting invitation
  without a password
- **THEN** the email SHALL contain a join link built from the configured
  frontend join base URL and the meeting short code only

## Requirement: Invitation emails SHALL be HTML transactional emails with required meeting details

Each invitation email SHALL be sent as an HTML email through Resend and SHALL
include enough visible content for invitees to understand the meeting they are
joining.

### Scenario: Email includes required meeting metadata

- **WHEN** the notification service sends an invitation email
- **THEN** the email SHALL include the meeting title, invitee-facing join
  call-to-action, and the meeting start time when one is present in the event
  payload

### Scenario: Email tolerates missing optional fields

- **WHEN** the invitation event omits optional fields such as `meetingTitle`,
  `startTime`, or invitee `displayName`
- **THEN** the notification service SHALL still send a valid invitation email
  using fallback copy without failing the whole event

## Requirement: Invitation delivery SHALL use Resend configuration managed by the notification service

The notification service SHALL load Resend API credentials, sender identity, and
join base URL from typed application configuration.

### Scenario: Service starts with configured Resend properties

- **WHEN** the notification service starts with valid Resend configuration
- **THEN** it SHALL initialize the email sender and be able to process
  invitation events

### Scenario: Service rejects invalid or missing email configuration

- **WHEN** required Resend or join-link configuration is missing
- **THEN** the notification service SHALL fail startup rather than silently
  discarding invitation emails

## Requirement: Invitation handling SHALL minimize password exposure

Because password-protected invitation events contain sensitive data, the system
SHALL avoid exposing that data outside the email-delivery path.

### Scenario: Consumer processing avoids logging sensitive payload values

- **WHEN** the notification service processes a password-protected invitation
  event
- **THEN** application logs SHALL NOT include the raw password or the fully
  rendered join link

### Scenario: Invitation topic retention is bounded for sensitive events

- **WHEN** infrastructure defines the Kafka topic for invitation events
- **THEN** the topic configuration SHALL use bounded retention instead of
  indefinite retention

# Context

`meeting-management` already persists invitees and publishes
`MeetingInvitationsSentEvent`, but no service consumes that event to deliver
invitation emails. The `notification` service exists only as an empty scaffold,
while the rest of the codebase already standardizes on Kafka CloudEvents, Spring
Boot configuration via `@ConfigurationProperties`, and hexagonal service
structure.

This change crosses multiple modules:

- `meeting-management` must emit enough invitation data for downstream email
  rendering.
- `notification` must become a Kafka consumer with a Resend-backed email
  adapter.
- Kafka topic manifests must include the invitation topic.

The main constraint is password-protected meetings. The domain only persists
`passwordHash`, and `RequestJoinUseCase` verifies a raw password on join.
Because the host does not want to manually communicate passwords, the system
must deliver the raw password at scheduling time through the invitation path
itself. That makes the invitation event payload security-sensitive and drives
retention and logging decisions.

## Goals / Non-Goals

**Goals:**

- Deliver invitation emails automatically after a meeting is scheduled with
  invitees.
- Use Resend as the transactional provider via the official Java SDK.
- Render HTML emails that include meeting metadata and a primary join action.
- Support password-protected meetings without requiring hosts to separately send
  the password.
- Keep the first iteration stateless in `notification` with no local
  persistence.
- Follow existing Kafka CloudEvents and Spring Boot conventions used elsewhere
  in the repo.

**Non-Goals:**

- Adding a general notification preference system, notification inbox, or
  delivery history UI.
- Building a generic templating platform for all future email types.
- Guaranteeing exactly-once email delivery across Kafka retries and provider
  retries.
- Redesigning the meeting join API beyond what is required to support invitation
  links.

## Decisions

### D1. Consume invitation events in `notification` with a fixed Kafka consumer group

`notification` will add a `@KafkaListener` consumer for
`meeting-management.meeting.invitations-sent` using the same
`CloudEventDeserializer` pattern as existing consumers. Unlike SSE fan-out
listeners, it will use a fixed group ID so only one service instance handles
each event.

- Chosen because email sending is work-queue behavior, not broadcast behavior.
- Rejected unique-per-instance group IDs because they would duplicate invitation
  sends across replicas.

### D2. Use the official Resend Java SDK behind a narrow adapter

The service will configure a singleton `Resend` client bean from
`@ConfigurationProperties` and wrap it in a small adapter such as
`ResendEmailSender`.

- Chosen because the SDK is the most direct, well-supported integration path and
  avoids custom HTTP plumbing.
- Rejected `spring-boot-starter-mail` because Resend is HTTP API based, not the
  repo's existing SMTP abstraction.
- Rejected a raw OkHttp client because it adds bespoke request/response handling
  with no benefit for this scope.

### D3. Keep the first iteration stateless: no notification database

The service will process Kafka events and call Resend directly. It will rely on
Kafka delivery semantics and listener error handling rather than persisting send
attempts locally.

- Chosen to keep the new service minimal and aligned with the user's explicit
  preference.
- Trade-off: duplicate delivery is still possible if a send succeeds but offset
  acknowledgement does not.
- Deferred alternative: add send-attempt persistence and idempotency keys in a
  later change if provider-level or business-level deduplication becomes
  mandatory.

### D4. Deliver password-protected invitations through embedded join links

For password-protected meetings, the invitation email will contain a join link
that includes the meeting short code and the raw password as URL parameters. The
notification renderer will build that URL from the event payload and configured
frontend join base URL.

- Chosen because it removes host-side manual password sharing and preserves
  one-click join UX, matching mainstream meeting product behavior.
- Rejected sending the password in a separate channel because the user
  explicitly does not want host-side manual handling.
- Rejected sending only a visual password in the email body because that still
  forces manual entry and creates a worse UX.

### D5. Enrich `MeetingInvitationsSentEvent` with invitation-join data at publish time

`meeting-management` must publish enough information for downstream email
rendering without extra lookups. The event payload will be extended to include
at least:

- meeting short code
- raw password presence and raw password value for protected meetings
- any meeting-level metadata needed in the email, such as title and start time

This data exists only at schedule time before the password is reduced to a hash,
so publish time is the only practical point to propagate it.

- Chosen because `notification` should not call back into `meeting-management`
  to reconstruct invitation details.
- Rejected deriving the password later from persisted meeting settings because
  only the hash is stored.

### D6. Treat invitation events as sensitive data and reduce exposure

Because protected-meeting events will carry raw password data, the
implementation must minimize exposure:

- do not log event payload bodies
- do not log rendered join links
- keep Kafka topic retention short for invitation events
- avoid storing raw password in notification-service persistence or files

- Chosen because embedded-password UX introduces a real confidentiality
  trade-off that must be bounded operationally.

### D7. Use a purpose-built HTML renderer with deterministic content

The email renderer will generate HTML in code from a fixed template rather than
introducing a general template engine now.

- Chosen because there is only one email type in scope and deterministic HTML is
  simpler to test.
- Rejected a larger template system because it would add framework and
  maintenance overhead before multiple templates exist.

## Risks / Trade-offs

- Raw password travels in Kafka event payloads -> Mitigation: use a dedicated
  invitation topic with short retention, suppress payload logging, and keep the
  password out of persistence.
- Email may be delivered more than once under retry edge cases -> Mitigation:
  keep subject/body deterministic, log provider message IDs, and reserve
  idempotency tracking for a follow-up change if duplicates become unacceptable.
- Embedded join links are forwardable -> Mitigation: clearly treat them as
  private invitation URLs and rely on existing meeting admission controls such
  as password checks and host approval policies.
- HTML email rendering differs across clients -> Mitigation: use table-based
  layout, inline styles, and include text-equivalent key content in the markup.
- Notification service scaffold currently contains unrelated dependencies ->
  Mitigation: clean the module build before adding notification-specific code so
  architecture remains minimal.

## Migration Plan

1. Add the new dependency and clean `services/notification/build.gradle.kts` so
   the service reflects its actual role.
2. Extend `MeetingInvitationsSentEvent` and its publish path in
   `meeting-management` to include join-link data.
3. Add the Kafka topic manifest for
   `meeting-management.meeting.invitations-sent` with retention appropriate for
   sensitive invitation payloads.
4. Implement `notification` service configuration, Kafka consumer, Resend
   adapter, and HTML renderer.
5. Add tests for event deserialization, send orchestration, HTML rendering, and
   password-protected invitation behavior.
6. Deploy `meeting-management` and `notification` together so event schema and
   consumer expectations stay compatible.

Rollback strategy:

- If notification sending fails after deployment, disable the notification
  consumer deployment or remove the listener configuration.
- If the event schema causes incompatibility, roll back both services together
  because the enriched invitation payload is a coupled contract.

## Open Questions

- None for this proposal. The change intentionally chooses embedded-password
  links as the first supported behavior for password-protected invitation
  emails.

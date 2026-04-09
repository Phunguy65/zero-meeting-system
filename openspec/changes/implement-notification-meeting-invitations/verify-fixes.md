## [2026-04-02] Round 1 (from spx-apply auto-verify)

### spx-arch-verifier

- Fixed: Moved notification property validation onto `@PostConstruct` so startup
  validation runs reliably for query-parameter guards and other required values.
- Fixed: Marked task 5.4 complete in `tasks.md` after re-running notification
  and meeting-management verification commands.

### spx-test-verifier

- Fixed: Made `MeetingInvitationsSentConsumer` continue sending remaining
  invitees when one send attempt fails, and added regression coverage for that
  failure path.
- Fixed: Added tests for blank protected-meeting passwords, log redaction, and
  subject-line newline sanitization.
- Fixed: Added startup-context coverage for invalid `join-base-url` query
  parameters.

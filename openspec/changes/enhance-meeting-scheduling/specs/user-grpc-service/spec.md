# ADDED Requirements

## Requirement: BatchGetUser RPC

The `user-management` service SHALL expose a gRPC `UserService` with a
`BatchGetUser` RPC that resolves users by a list of emails and/or usernames in a
single call.

### Scenario: Resolve users by email

- **WHEN** `BatchGetUser` is called with `emails = ["alice@example.com"]`
- **THEN** the response `by_email` map contains an entry
  `"alice@example.com" → UserSnapshot` if the user exists and is active

### Scenario: Resolve users by username

- **WHEN** `BatchGetUser` is called with `usernames = ["bob123"]`
- **THEN** the response `by_username` map contains an entry
  `"bob123" → UserSnapshot` if the user exists and is active

### Scenario: Mixed batch request

- **WHEN** `BatchGetUser` is called with both `emails` and `usernames` lists
- **THEN** both `by_email` and `by_username` maps are populated with resolved
  users

### Scenario: Unknown identifier omitted from response

- **WHEN** `BatchGetUser` is called with an email that does not match any active
  user
- **THEN** that email key is absent from `by_email` — no error is thrown;
  partial success is the contract

### Scenario: Deleted user not returned

- **WHEN** `BatchGetUser` is called with the email of a soft-deleted user
- **THEN** that email key is absent from `by_email`

---

## Requirement: UserSnapshot captures full user state

The `UserSnapshot` message returned by `BatchGetUser` SHALL capture the full
observable state of a user, mirroring `UserResponse.java`, excluding
security-sensitive fields.

### Scenario: UserSnapshot fields

- **WHEN** a user is resolved via `BatchGetUser`
- **THEN** the returned `UserSnapshot` SHALL contain: `id` (UUID string),
  `email`, `full_name`, optional `username`, optional `avatar_url`,
  `auth_provider` (`EMAIL`|`GOOGLE`|`BOTH`), `preferences` (Struct),
  `created_at` (Timestamp), `updated_at` (Timestamp)
- **THEN** the `UserSnapshot` SHALL NOT contain `password_hash`, `google_uid`,
  or `deleted_at`

---

## Requirement: gRPC server on user-management

The `user-management` service SHALL start a gRPC server (default port 9090)
alongside the existing HTTP server.

### Scenario: gRPC server starts with service

- **WHEN** `user-management` starts
- **THEN** a gRPC server is listening on the configured port

### Scenario: gRPC port configurable

- **WHEN** `grpc.server.port` property is set
- **THEN** the gRPC server binds to that port

# ADDED Requirements

## Requirement: User can register with email and password

The system SHALL allow a new user to create an account by providing a valid
email address, a password meeting complexity requirements, and a full name. The
system SHALL hash the password using Argon2id before persisting. The system
SHALL assign a UUIDv7 as the user's primary key. The system SHALL reject
registration if the email is already in use and return error code
`EMAIL_ALREADY_EXISTS`.

### Scenario: Successful registration

- **WHEN** a POST request is sent to `/api/v1/auth/register` with a valid unique
  email, a password of at least 8 characters, and a non-empty full name
- **THEN** the system SHALL create a new user record with a hashed password,
  return HTTP 201, and respond with `JsendResponse.success` containing `userId`,
  `email`, and `fullName`

### Scenario: Duplicate email rejected

- **WHEN** a POST request is sent to `/api/v1/auth/register` with an email that
  already exists in the `users` table
- **THEN** the system SHALL return HTTP 409 and respond with
  `JsendResponse.fail` containing error code `EMAIL_ALREADY_EXISTS`

### Scenario: Invalid input rejected

- **WHEN** a POST request is sent to `/api/v1/auth/register` with a missing or
  malformed email, a password shorter than 8 characters, or an empty full name
- **THEN** the system SHALL return HTTP 400 and respond with
  `JsendResponse.fail` containing a list of field-level `Violation` objects

### Scenario: Password is never stored in plain text

- **WHEN** a user is successfully registered
- **THEN** the `password_hash` column in the `users` table SHALL contain an
  Argon2id hash, never the raw password

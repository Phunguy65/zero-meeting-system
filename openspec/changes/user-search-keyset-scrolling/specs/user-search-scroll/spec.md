# ADDED Requirements

## Requirement: Search users with keyset scrolling

The system SHALL expose `GET /v1/users:search` that returns a cursor-scrollable
list of active users, optionally filtered by a `query` string matched against
`username` OR `email` (case-insensitive substring). Results SHALL be ordered by
`created_at DESC, id DESC`.

### Scenario: First page with no filters

- **WHEN** client sends `GET /v1/users:search?size=20` with no `pageToken` or
  `query`
- **THEN** system returns up to 20 active users ordered by
  `created_at DESC, id DESC`
- **THEN** response contains `nextPageToken` (non-null) if more users exist, or
  `null` if the result set is exhausted

### Scenario: Subsequent page using nextPageToken

- **WHEN** client sends `GET /v1/users:search?size=20&pageToken=<token>` with a
  valid token from a previous response
- **THEN** system returns the next page of users continuing from the cursor
  position
- **THEN** response contains a new `nextPageToken` or `null` if no more data

### Scenario: Search by query matching email

- **WHEN** client sends `GET /v1/users:search?query=alice`
- **THEN** system returns active users whose `email` contains "alice"
  (case-insensitive)

### Scenario: Search by query matching username

- **WHEN** client sends `GET /v1/users:search?query=alice`
- **THEN** system returns active users whose `username` contains "alice"
  (case-insensitive)

### Scenario: Search query matches email OR username

- **WHEN** a user has `email=bob@example.com` and `username=alice_b`
- **WHEN** client sends `GET /v1/users:search?query=alice`
- **THEN** that user is included in results (username matches)

### Scenario: Empty result set

- **WHEN** no active users match the given `query`
- **THEN** system returns `content: []` and `nextPageToken: null`

### Scenario: Invalid pageToken

- **WHEN** client sends a `pageToken` that fails HMAC verification or is
  malformed
- **THEN** system returns HTTP 400 with `status: "fail"` and error code
  `INVALID_CURSOR`

### Scenario: Size defaults and bounds

- **WHEN** client omits `size`
- **THEN** system uses default size of 20
- **WHEN** client sends `size` greater than 100
- **THEN** system clamps to 100

## Requirement: Response shape

The system SHALL return responses in JSend format with `CursorScrollResponse` as
the data payload.

### Scenario: Successful response structure

- **WHEN** the search succeeds
- **THEN** response body SHALL be:
    ```json
    {
        "status": "success",
        "data": {
            "content": [
                /* UserResponse objects */
            ],
            "size": 20,
            "nextPageToken": "<token or null>"
        }
    }
    ```

## Requirement: Remove legacy GET /v1/users endpoint

The system SHALL NOT expose `GET /v1/users` (offset-based pagination endpoint).

### Scenario: Legacy endpoint removed

- **WHEN** client sends `GET /v1/users`
- **THEN** system returns HTTP 404

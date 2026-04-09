# ADDED Requirements

## Requirement: ScrollParams interface

The shared module SHALL provide a `ScrollParams` interface that any cursor-based
pagination request DTO can implement, analogous to the existing `SliceParams`
interface.

### Scenario: Generic request implements ScrollParams

- **WHEN** a use case request DTO implements `ScrollParams`
- **THEN** it exposes `pageSize()`, `pageToken()` (Optional<String>), and
  `query()` (Optional<String>)

## Requirement: CursorPageResult domain record

The shared module SHALL provide a `CursorPageResult<T>` record as the
domain-layer result type for keyset queries, analogous to `PageResult<T>`.

### Scenario: Factory method detects end of data

- **WHEN** infrastructure adapter fetches `size + 1` rows and receives fewer
  than `size + 1`
- **THEN** `CursorPageResult.of(items, size, hasNext=false)` sets `nextCursor`
  to `null`
- **WHEN** infrastructure adapter receives exactly `size + 1` rows
- **THEN** `CursorPageResult.of(items, size, hasNext=true)` sets `nextCursor` to
  the last item's cursor key

## Requirement: CursorScrollResponse HTTP envelope

The shared module SHALL provide a `CursorScrollResponse<T>` record for the HTTP
layer, analogous to `SliceHttpResponse<T>`.

### Scenario: Serialized response fields

- **WHEN** `CursorScrollResponse` is serialized to JSON
- **THEN** it contains exactly: `content` (list), `size` (int), `nextPageToken`
  (string or null)

## Requirement: ScrollRequest generic DTO

The shared module SHALL provide a `ScrollRequest` record that implements
`ScrollParams` for endpoints that need only pagination with no domain-specific
filters.

### Scenario: Default size applied

- **WHEN** `ScrollRequest` is constructed with no size
- **THEN** `pageSize()` returns 20

### Scenario: Size clamped to max

- **WHEN** `ScrollRequest` is constructed with size > 100
- **THEN** `pageSize()` returns 100

## Requirement: CursorEncoder HMAC utility

The shared module SHALL provide a `CursorEncoder` utility that encodes and
decodes cursor tokens using HMAC-SHA256 signing.

### Scenario: Encode produces Base64url token

- **WHEN** `CursorEncoder.encode(createdAt, id)` is called
- **THEN** it returns a Base64url string containing the epoch millis, UUID, and
  HMAC signature

### Scenario: Decode verifies signature

- **WHEN** `CursorEncoder.decode(token)` is called with a valid token
- **THEN** it returns `Result.Success` containing a `ScrollCursor` record with
  `createdAt` (Instant) and `id` (UUID)

### Scenario: Decode rejects tampered token

- **WHEN** `CursorEncoder.decode(token)` is called with a token whose payload
  was modified
- **THEN** it returns `Result.Failure` with `CursorErrorCode.INVALID_CURSOR`

### Scenario: Decode rejects malformed token

- **WHEN** `CursorEncoder.decode(token)` is called with a non-Base64url or
  structurally invalid string
- **THEN** it returns `Result.Failure` with `CursorErrorCode.INVALID_CURSOR`

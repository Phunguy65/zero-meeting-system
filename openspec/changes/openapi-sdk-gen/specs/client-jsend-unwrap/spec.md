# ADDED Requirements

## Requirement: The Android app SHALL unwrap JSend envelopes via an OkHttp Interceptor

An OkHttp `Interceptor` SHALL parse every API response as a JSend envelope and
transform it before Retrofit deserialisation:

- `"success"` responses: the interceptor SHALL rebuild the response body with
  only the `data` field's value so Retrofit deserialises the payload type
  directly.
- `"fail"` responses: the interceptor SHALL throw an `ApiFailException`
  containing the machine-readable `code`, the `message`, and the list of
  `violations` from the `FailData` object.
- `"error"` responses: the interceptor SHALL throw an `ApiErrorException`
  containing the `message`.

### Scenario: Success response unwrapped

- **WHEN** the server returns `{"status":"success","data":{...}}`
- **THEN** the interceptor MUST replace the response body with the value of
  `data` so that Retrofit deserialises the payload type directly

### Scenario: Fail response throws typed exception

- **WHEN** the server returns
  `{"status":"fail","data":{"code":"EMAIL_EXISTS","message":"...","errors":[...]}}`
- **THEN** the interceptor MUST throw an `ApiFailException` whose `getCode()`
  returns `"EMAIL_EXISTS"`, `getMessage()` returns the message, and
  `getViolations()` returns the errors list

### Scenario: Error response throws server exception

- **WHEN** the server returns `{"status":"error","message":"Internal failure"}`
- **THEN** the interceptor MUST throw an `ApiErrorException` whose
  `getMessage()` returns `"Internal failure"`

## Requirement: The web app SHALL unwrap JSend envelopes via @hey-api middleware

A response transformer middleware SHALL be registered on the `@hey-api` client
that:

- `"success"` responses: returns the `data` field value directly.
- `"fail"` responses: throws an `ApiFailError` with `code`, `message`, and
  `errors`.
- `"error"` responses: throws an `ApiError` with `message`.

### Scenario: Success response unwrapped

- **WHEN** the server returns a JSend success response
- **THEN** the middleware MUST resolve the promise with the `data` field value

### Scenario: Fail response throws typed error

- **WHEN** the server returns a JSend fail response
- **THEN** the middleware MUST reject with an `ApiFailError` containing the
  `code`, `message`, and `errors` from the `FailData`

### Scenario: Error response throws server error

- **WHEN** the server returns a JSend error response
- **THEN** the middleware MUST reject with an `ApiError` containing the
  `message`

## Requirement: Both clients SHALL expose an ErrorTranslator extension point

The Android interceptor and web middleware SHALL accept an optional
`ErrorTranslator` that maps error codes and default messages to translated
strings. The default behaviour (when no translator is provided) SHALL return the
original message unchanged.

### Scenario: Android translator hook invoked

- **WHEN** a fail response is intercepted and an `ErrorTranslator` is registered
- **THEN** the interceptor MUST call
  `errorTranslator.translate(code, defaultMessage)` and use the returned string
  as the exception message

### Scenario: Web translator hook invoked

- **WHEN** a fail response is intercepted and an `errorTranslator` function is
  provided
- **THEN** the middleware MUST call `errorTranslator(code, defaultMessage)` and
  use the returned string as the error message

### Scenario: Default behaviour without translator

- **WHEN** no `ErrorTranslator` is registered
- **THEN** both clients MUST use the original server-provided `message`
  unchanged

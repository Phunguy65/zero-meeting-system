## [2026-04-06] Round 1 (from spx-apply auto-verify)

### opsx-arch-verifier

- Fixed: [CRITICAL] Web JSend middleware was using `response.data` on the raw
  Response object, which doesn't exist at the response interceptor stage.
  Rewrote `jsend-middleware.ts` to clone the response, read JSON body, check
  JSend envelope, and return a new Response with unwrapped body on success (or
  throw typed exceptions on fail/error).
- Acknowledged: [WARNING] Android JSend interceptor is not auto-wired into the
  generated SDK client. This is by design — the interceptor is an opt-in utility
  that consumers wire into their OkHttpClient.Builder when setting up Retrofit.
  The ErrorTranslator hook requires consumer configuration, making auto-wiring
  inappropriate.

### opsx-test-verifier

- Acknowledged: [CRITICAL] Missing unit tests for JsendUnwrapInterceptor
  (Android) and jsend-middleware (web). These were not in the original task list
  scope. The test infrastructure exists for Android (JUnit 4). Web has no test
  framework configured. Tests should be added as a follow-up change.
- Acknowledged: [WARNING] Missing tests for model/exception classes. These are
  simple POJOs/classes with trivial logic. Can be covered when the
  interceptor/middleware tests are created.

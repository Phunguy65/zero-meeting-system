# Tasks

## 1. Database Migration

- [x] 1.1 Create `V5__google_auth_and_preferences.sql` (PostgreSQL): alter
      `password_hash` to nullable, add `google_uid VARCHAR(128) UNIQUE`, add
      `auth_provider VARCHAR(20) NOT NULL DEFAULT 'EMAIL'`
- [x] 1.2 Create matching `V5__google_auth_and_preferences.sql` in
      `src/test/resources/db/h2-migration/` with H2-compatible syntax

## 2. Domain Model

- [x] 2.1 Update `User` aggregate: make `hashedPassword` field `@Nullable`, add
      `googleUid` (`String`, nullable) and `authProvider` (`String`) fields
- [x] 2.2 Add `User.registerWithGoogle(email, googleUid, fullName, avatarUrl)`
      static factory method that sets `authProvider = "GOOGLE"` and
      `hashedPassword = null`; fires `UserRegisteredEvent`
- [x] 2.3 Add `User.linkGoogle(googleUid)` method that sets `googleUid` and
      updates `authProvider` to `"BOTH"` when linking to an existing email
      account
- [x] 2.4 Add `User.hasPassword()` helper returning `hashedPassword != null`
- [x] 2.5 Add `AuthErrorCode.INVALID_FIREBASE_TOKEN` and
      `AuthErrorCode.FIREBASE_AUTH_ERROR` to `AuthErrorCode` enum

## 3. Persistence Layer

- [x] 3.1 Update `UserJpaEntity`: add `googleUid` and `authProvider` columns;
      make `passwordHash` nullable (`@Column(nullable = false)` → remove
      constraint)
- [x] 3.2 Update `UserRepositoryAdapter.toDomain()` and `toEntity()` mappers to
      include `googleUid` and `authProvider`
- [x] 3.3 Add
      `UserRepository.findActiveByGoogleUid(String googleUid): Optional<User>`
      port method and its JPA implementation

## 4. Firebase Infrastructure

- [x] 4.1 Add `com.google.firebase:firebase-admin:9.8.0` to
      `services/user-management/build.gradle.kts`
- [x] 4.2 Add `app.firebase.project-id` and `app.firebase.credentials-path`
      properties to `application.properties` using `${ENV_VAR:default}` pattern
- [x] 4.3 Create `infrastructure/config/FirebaseConfig.java`: `@Configuration`
      bean that initialises `FirebaseApp` from credentials path (file) or falls
      back to `GoogleCredentials.getApplicationDefault()`; exposes
      `FirebaseAuth` bean
- [x] 4.4 Create `infrastructure/security/FirebaseTokenVerifier.java`: thin
      wrapper around `FirebaseAuth.verifyIdToken()` that returns
      `Result<FirebaseTokenClaims, AuthErrorCode>` (maps `FirebaseAuthException`
      to `INVALID_FIREBASE_TOKEN`)
- [x] 4.5 Create `FirebaseTokenClaims` record: `uid`, `email`, `displayName`,
      `photoUrl`

## 5. Application Layer — DTOs

- [x] 5.1 Create `UserPreferencesDto` record: `theme`
      (`@Pattern(regexp = "dark|light|system")`), `defaultMic` (boolean),
      `defaultCamera` (boolean); add `defaults()` static factory
- [x] 5.2 Create `GoogleLoginRequest` record: `@NotBlank String idToken`
- [x] 5.3 Extend `LoginResponse` record to include
      `UserPreferencesDto preferences` (add as last field to avoid breaking
      positional constructors in tests)
- [x] 5.4 Update all existing `new LoginResponse(...)` call sites in
      `LoginUserUseCase` and `RefreshTokenUseCase` to pass `preferences`

## 6. Application Layer — Use Cases

- [x] 6.1 Create `LoginWithGoogleUseCase`: inject `FirebaseTokenVerifier`,
      `UserRepository`, `RefreshTokenRepository`, `JwtTokenProvider`,
      `ObjectMapper`; implement find-or-create logic per spec; return
      `Result<LoginResponse, AuthErrorCode>`
- [x] 6.2 Update `LoginUserUseCase`: null-check `user.hasPassword()` before
      calling `passwordHasher.verify()`; return `INVALID_CREDENTIALS` if no
      password set
- [x] 6.3 Create `GetUserPreferencesUseCase`: accepts `UUID userId`; loads user;
      deserialises `preferences` JSON via `ObjectMapper`; returns
      `Result<UserPreferencesDto, AuthErrorCode>`
- [x] 6.4 Create `UpdateUserPreferencesUseCase`: accepts `UUID userId` +
      `UserPreferencesDto`; serialises to JSON; saves via `UserRepository`;
      returns `Result<UserPreferencesDto, AuthErrorCode>`

## 7. Presentation Layer

- [x] 7.1 Add `POST /{version}/auth/google-login` endpoint to `AuthController`
      (version `1.0`); delegate to `LoginWithGoogleUseCase`; map errors to HTTP
      401
- [x] 7.2 Create `UserController` (`@RestController`, `@RequestMapping`):
      `GET /{version}/users/me/preferences` and
      `PUT /{version}/users/me/preferences` (version `1.0`); extract `userId`
      from `SecurityContextHolder`; delegate to respective use cases
- [x] 7.3 Update `SecurityConfig`: add `/api/v1/auth/google-login` to permitted
      paths; ensure `/api/v1/users/**` requires authentication
- [x] 7.4 Update `compose.yaml` for `user-management`: add `FIREBASE_PROJECT_ID`
      and `GOOGLE_APPLICATION_CREDENTIALS` environment variables and volume
      mount for service-account JSON

## 8. Tests

- [x] 8.1 Unit test `LoginWithGoogleUseCase`: cover first-time login (creates
      user), returning user, account linking, invalid token, deleted user
      scenarios
- [x] 8.2 Unit test `GetUserPreferencesUseCase` and
      `UpdateUserPreferencesUseCase`: cover happy path, null preferences
      (returns defaults), invalid theme validation
- [x] 8.3 Update `LoginUserUseCaseTest`: add test for user with `null` password
      (Google-only account) attempting email/password login →
      `INVALID_CREDENTIALS`
- [x] 8.4 Integration test (`@SpringBootTest`) for
      `POST /api/v1/auth/google-login`: mock `FirebaseTokenVerifier`; test 200
      (new user), 200 (existing user), 401 (invalid token), 401 (deleted user)
- [x] 8.5 Integration test for `GET` and `PUT /api/v1/users/me/preferences`:
      test 200 with stored prefs, 200 with defaults (null prefs), 400 invalid
      theme, 401 unauthenticated

## 9. Frontend — Web Client

- [x] 9.1 Add `firebase` to `frontends/web/package.json` dependencies
      (`pnpm add firebase`)
- [x] 9.2 Create `src/lib/firebase.ts`: initialise Firebase app with
      `NEXT_PUBLIC_FIREBASE_*` env vars; export `auth` instance (auth module
      only)
- [x] 9.3 Create `src/hooks/useGoogleAuth.ts`:
      `signInWithPopup(GoogleAuthProvider)`, get `idToken`,
      `POST /api/v1/auth/google-login`, store `accessToken` and `refreshToken`,
      return `{ login, logout, loading, error }`
- [x] 9.4 Create `src/components/GoogleLoginButton.tsx`: button that calls
      `useGoogleAuth().login()`; show loading state during sign-in
- [x] 9.5 Add `.env.local.example` with `NEXT_PUBLIC_FIREBASE_API_KEY`,
      `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`, `NEXT_PUBLIC_FIREBASE_PROJECT_ID`
      placeholders

# Android App — Codemap

Architecture: **MVVM + Clean Architecture** | Language: **Java** | DI: **Hilt**
| Network: **Retrofit + OkHttp + Gson**

## Package Root

`io.github.phunguy65.zms` — all source under `app/src/main/java/`

```
ZeroMeetingApp.java                         @HiltAndroidApp — Hilt entry point
```

## di/ — Dependency Injection

Hilt `@Module` classes wiring the app together.

```
di/
├── NetworkModule.java                      @Module — provides OkHttpClient, Retrofit, API interfaces
├── RepositoryModule.java                   @Module — @Binds repository interfaces → implementations
└── StorageModule.java                      @Module — provides EncryptedSharedPreferences
```

## data/ — Data Layer

Implementation details: API communication, DTO mapping, repository
implementations. Depends on: `domain/` (implements its interfaces). NEVER
depended on by `domain/`.

```
data/
├── local/
│   └── TokenManager.java                   @Singleton — EncryptedSharedPreferences token storage
├── remote/
│   ├── api/          [auto-generated]      Retrofit API interfaces (build/generated/openapi/)
│   ├── dto/          [auto-generated]      JSON model classes (build/generated/openapi/)
│   ├── client/       [auto-generated]      ApiClient configuration (build/generated/openapi/)
│   └── interceptor/                        OkHttp interceptors (handwritten)
│       ├── JsendUnwrapInterceptor.java     Unwraps JSend envelopes before Retrofit deserialization
│       ├── JsendEnvelope.java              JSend response envelope model (status/data/message)
│       ├── ErrorTranslator.java            i18n hook for error code → locale message
│       ├── AndroidErrorTranslator.java     @Singleton — translates error codes to locale strings via R.string
│       ├── ApiFailException.java           Thrown on JSend "fail" (HTTP 4xx) with violations
│       └── ApiErrorException.java          Thrown on JSend "error" (HTTP 5xx)
├── mapper/                                 DTO → Domain model converters
│   ├── UserMapper.java                     Maps User DTOs → domain User
│   ├── MeetingMapper.java                  Maps Meeting DTOs → domain Meeting
│   ├── ParticipantMapper.java              Maps Participant DTOs → domain Participant
│   └── ChatMessageMapper.java             Maps ChatMessage DTOs → domain ChatMessage
└── repository/                             Repository implementations
    ├── AuthRepositoryImpl.java             Implements AuthRepository (login, register, googleLogin)
    ├── MeetingRepositoryImpl.java          Implements MeetingRepository
    ├── ChatRepositoryImpl.java             Implements ChatRepository
    ├── CalendarRepositoryImpl.java         Implements CalendarRepository
    ├── ProfileRepositoryImpl.java          Implements ProfileRepository
    └── ScheduleRepositoryImpl.java         Implements ScheduleRepository
```

## domain/ — Domain Layer

Pure Java, zero Android dependencies. Business entities, repository contracts,
use cases. Depends on: **nothing** (innermost layer).

```
domain/
├── model/                                  Business entities (POJOs)
│   ├── User.java                           Authenticated user
│   ├── LoginResult.java                    Login/Google sign-in result (accessToken, refreshToken, expiresIn)
│   ├── RegisterResult.java                 Registration result (userId, email, fullName, username)
│   ├── Meeting.java                        Meeting room session
│   ├── Participant.java                    Meeting participant (name, role, mic/video state)
│   ├── ChatMessage.java                    In-meeting chat message
│   ├── Schedule.java                       Scheduled meeting
│   └── CalendarEvent.java                  Calendar event entry
├── repository/                             Repository interfaces (contracts)
│   ├── AuthRepository.java                 Authentication operations (login, register, googleLogin)
│   ├── MeetingRepository.java              Meeting room operations
│   ├── ChatRepository.java                 In-meeting chat operations
│   ├── CalendarRepository.java             Calendar event operations
│   ├── ProfileRepository.java              User profile operations
│   └── ScheduleRepository.java             Schedule operations
└── usecase/                                Business actions (one action per class)
    ├── auth/
    │   ├── LoginUseCase.java               User login via email/password
    │   ├── RegisterUseCase.java            New user registration
    │   └── GoogleLoginUseCase.java         Google Sign-In via Firebase ID token
    ├── meeting/
    │   ├── CreateMeetingUseCase.java        Create a new meeting room
    │   ├── JoinMeetingUseCase.java          Join an existing meeting
    │   └── LeaveMeetingUseCase.java         Leave meeting + cleanup
    ├── chat/
    │   └── SendMessageUseCase.java          Send chat message in meeting
    ├── calendar/
    │   └── GetCalendarEventsUseCase.java    Retrieve calendar events
    ├── profile/
    │   └── GetProfileUseCase.java           Retrieve current user profile
    └── schedule/
        └── GetScheduleUseCase.java          Retrieve scheduled meetings
```

## presentation/ — Presentation Layer

UI components: Activities, Fragments, ViewModels, Adapters. Grouped by feature.
Depends on: `domain/` (uses models, use cases). NEVER imports `data/`.

```
presentation/
├── common/
│   └── state/
│       ├── UiState.java                    Sealed interface: Idle | Loading | Success<T> | Error
│       ├── UiError.java                    Sealed interface: Fail | ServerError | NetworkError | Unknown
│       └── FieldError.java                 Field-level validation error (field, message, code); 2-arg constructor for client-side (message=null)
├── auth/
│   ├── AuthActivity.java                   @AndroidEntryPoint — NavHost for auth flow
│   ├── login/
│   │   ├── LoginFragment.java              @AndroidEntryPoint — login screen (email/pw + Google)
│   │   └── LoginViewModel.java             @HiltViewModel — login state + Google Sign-In
│   └── register/
│       ├── RegisterFragment.java           @AndroidEntryPoint — registration screen
│       └── RegisterViewModel.java          @HiltViewModel — registration state + validation
├── dashboard/
│   ├── DashboardActivity.java              @AndroidEntryPoint — main dashboard with bottom nav
│   └── DashboardViewModel.java             @HiltViewModel — dashboard state
├── meeting/
│   ├── create/
│   │   ├── CreateMeetingActivity.java      @AndroidEntryPoint — create meeting screen
│   │   └── CreateMeetingViewModel.java     @HiltViewModel
│   ├── room/
│   │   ├── MeetingRoomActivity.java        @AndroidEntryPoint — active meeting room (video/audio)
│   │   └── MeetingRoomViewModel.java       @HiltViewModel
│   ├── join/
│   │   ├── JoinMeetingActivity.java        @AndroidEntryPoint — join meeting by code
│   │   └── JoinMeetingViewModel.java       @HiltViewModel
│   ├── chat/
│   │   ├── MeetingChatActivity.java        @AndroidEntryPoint — in-meeting chat
│   │   └── MeetingChatViewModel.java       @HiltViewModel
│   └── participant/
│       ├── ParticipantsActivity.java       @AndroidEntryPoint — participant list
│       ├── ParticipantsViewModel.java      @HiltViewModel
│       └── ParticipantAdapter.java         RecyclerView adapter for participant items
├── calendar/
│   ├── CalendarActivity.java               @AndroidEntryPoint — calendar view
│   └── CalendarViewModel.java              @HiltViewModel
├── schedule/
│   ├── ScheduleActivity.java              @AndroidEntryPoint — schedule view
│   └── ScheduleViewModel.java             @HiltViewModel
├── profile/
│   ├── ProfileActivity.java               @AndroidEntryPoint — user profile
│   └── ProfileViewModel.java              @HiltViewModel
├── splash/
│   ├── SplashActivity.java                @AndroidEntryPoint — app launch screen (LAUNCHER)
│   └── SplashViewModel.java               @HiltViewModel
├── welcome/
│   └── WelcomeActivity.java               Welcome/onboarding screen → launches AuthActivity
└── guest/
    ├── JoinGuestActivity.java             @AndroidEntryPoint — guest join (no account)
    └── JoinGuestViewModel.java            @HiltViewModel
```

## res/ — Resources (key files)

```
res/
├── layout/
│   ├── activity_auth.xml                   NavHost container for auth flow
│   ├── fragment_login.xml                  Login screen (email/pw + Google, no Apple)
│   ├── fragment_register.xml               Register screen (fullName, username, email, pw, confirm)
│   └── ...                                 Other activity layouts
├── values/
│   ├── strings.xml                         English string resources (app + auth i18n)
│   ├── colors.xml                          Material 3 color palette (light + dark)
│   ├── dimens.xml                          Spacing tokens (xs/sm/md/lg/xl), touch targets, corner radii
│   └── themes.xml                          Light theme (Material 3 DayNight)
├── values-night/
│   └── themes.xml                          Dark theme overrides
├── values-vi/
│   └── strings.xml                         Vietnamese translations for auth flow
├── drawable/
│   ├── ic_google_logo.xml                  Google "G" multicolor vector drawable
│   ├── bg_login_header.xml                 Login header gradient (light)
│   └── bg_image_placeholder.xml            Circular image placeholder
├── drawable-night/
│   └── bg_login_header.xml                 Login header gradient (dark)
└── navigation/
    └── nav_graph_auth.xml                  Auth navigation graph (loginFragment ↔ registerFragment)
```

## util/ — Utilities

Shared utility classes and constants.

```
util/
└── (empty — add Constants.java, NetworkUtils.java as needed)
```

## Dependency Direction

```
presentation/ ──→ domain/ ←── data/
                    ↑
                    │
                  di/ (wires everything)
```

- `domain/` imports nothing — pure Java
- `presentation/` imports `domain/` only
- `data/` implements `domain/` interfaces
- `di/` knows all layers to wire them

## OpenAPI Generated Code

Generated into `build/generated/openapi/` (not committed to git). Packages:
`data.remote.api`, `data.remote.dto`, `data.remote.client`. Regenerated every
build via `openApiGenerate` Gradle task from `openapi/unified-openapi.yaml`.

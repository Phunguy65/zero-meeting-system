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

**Architecture**: Single-Activity pattern with Navigation Component for main app
flows.

- `MainActivity` hosts bottom navigation (Dashboard, Calendar, Profile) and
  nested destinations
- `VideoCallActivity` is a separate task stack for video calls (supports PiP)
- `AuthActivity` hosts login/register flow

```
presentation/
├── common/
│   └── state/
│       ├── UiState.java                    Sealed interface: Idle | Loading | Success<T> | Error
│       ├── UiError.java                    Sealed interface: Fail | ServerError | NetworkError | Unknown
│       └── FieldError.java                 Field-level validation error (field, message, code)
├── auth/
│   ├── AuthActivity.java                   @AndroidEntryPoint — NavHost for auth flow
│   ├── login/
│   │   ├── LoginFragment.java              @AndroidEntryPoint — login screen (email/pw + Google)
│   │   └── LoginViewModel.java             @HiltViewModel — login state + Google Sign-In
│   └── register/
│       ├── RegisterFragment.java           @AndroidEntryPoint — registration screen
│       └── RegisterViewModel.java          @HiltViewModel — registration state + validation
├── main/                                   **Main app flow (single-activity + fragments)**
│   ├── MainActivity.java                   @AndroidEntryPoint — NavHost + BottomNavigationView
│   ├── dashboard/
│   │   ├── DashboardFragment.java          @AndroidEntryPoint — quick actions + upcoming meetings
│   │   └── DashboardViewModel.java         @HiltViewModel — dashboard state
│   ├── calendar/
│   │   ├── CalendarFragment.java           @AndroidEntryPoint — calendar view + day events
│   │   └── CalendarViewModel.java          @HiltViewModel — calendar state
│   ├── profile/
│   │   ├── ProfileFragment.java            @AndroidEntryPoint — user profile + settings menu
│   │   └── ProfileViewModel.java           @HiltViewModel — profile state
│   ├── schedule/
│   │   ├── ScheduleFragment.java           @AndroidEntryPoint — schedule meeting form
│   │   └── ScheduleViewModel.java          @HiltViewModel — schedule state
│   ├── meeting/
│   │   ├── CreateMeetingFragment.java      @AndroidEntryPoint — create meeting with AV toggle
│   │   └── CreateMeetingViewModel.java     @HiltViewModel
│   └── settings/
│       ├── SettingsFragment.java           @AndroidEntryPoint — language + about settings
│       └── SettingsViewModel.java          @HiltViewModel
├── videocall/                              **Video call flow (separate activity stack)**
│   ├── VideoCallActivity.java              @AndroidEntryPoint — NavHost, singleInstance, PiP support
│   ├── CallViewModel.java                  @HiltViewModel — shared call state (activity-scoped)
│   ├── PreJoinFragment.java                @AndroidEntryPoint — meeting code + AV preview
│   ├── ActiveCallFragment.java             @AndroidEntryPoint — video grid + call controls
│   ├── ParticipantsBottomSheet.java        BottomSheetDialogFragment — participant list
│   └── MeetingChatBottomSheet.java         BottomSheetDialogFragment — in-call chat
├── splash/
│   ├── SplashActivity.java                 @AndroidEntryPoint — app launch screen (LAUNCHER)
│   └── SplashViewModel.java                @HiltViewModel
└── welcome/
    └── WelcomeActivity.java                Welcome/onboarding screen → launches AuthActivity or VideoCallActivity (guest)
```

## res/ — Resources (key files)

```
res/
├── layout/
│   ├── activity_main.xml                   MainActivity NavHost + BottomNavigationView
│   ├── activity_video_call.xml             VideoCallActivity NavHost
│   ├── activity_auth.xml                   NavHost container for auth flow
│   ├── fragment_dashboard.xml              Dashboard with quick actions + meetings
│   ├── fragment_calendar.xml               Calendar strip + day events
│   ├── fragment_profile.xml                Profile menu items
│   ├── fragment_schedule.xml               Schedule meeting form
│   ├── fragment_create_meeting.xml         Create meeting with AV preview
│   ├── fragment_settings.xml               Language + about settings
│   ├── fragment_prejoin.xml                Pre-join screen with AV toggle
│   ├── fragment_active_call.xml            Video grid + call controls
│   ├── layout_participants_sheet.xml       Participants bottom sheet
│   ├── layout_meeting_chat_sheet.xml       Chat bottom sheet
│   ├── layout_empty_dashboard.xml          Empty state for dashboard
│   ├── layout_empty_calendar.xml           Empty state for calendar
│   ├── fragment_login.xml                  Login screen (email/pw + Google)
│   └── fragment_register.xml               Register screen
├── navigation/
│   ├── nav_graph_main.xml                  Main app navigation (dashboard/calendar/profile + nested)
│   ├── nav_graph_call.xml                  Video call navigation (prejoin → active call)
│   └── nav_graph_auth.xml                  Auth navigation (login ↔ register)
├── menu/
│   └── bottom_nav_menu.xml                 Bottom navigation menu items
├── values/
│   ├── strings.xml                         English string resources
│   ├── colors.xml                          Material 3 color palette (light + dark)
│   ├── dimens.xml                          Spacing tokens, touch targets, corner radii
│   └── themes.xml                          Light theme (Material 3 DayNight)
├── values-night/
│   └── themes.xml                          Dark theme overrides
├── values-vi/
│   └── strings.xml                         Vietnamese translations
├── drawable/
│   ├── ic_*.xml                            Material Symbols icons (home, calendar, person, etc.)
│   ├── bg_circle_*.xml                     Circular button backgrounds (theme-aware via ?attr/)
│   ├── bg_rounded_gray.xml                 Rounded card backgrounds (theme-aware)
│   ├── bg_leave_button.xml                 Leave button background (error container)
│   └── bg_image_placeholder.xml            Avatar/image placeholder
└── drawable-night/
    └── bg_login_header.xml                 Login header gradient (dark theme)
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

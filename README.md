# News

News is a modular Android application for browsing the latest articles from
[NewsAPI](https://newsapi.org/). It provides searchable news feed, category
filtering, pagination, local Room caching, and resilient loading and error
states.

## Features

- Latest US headlines
- Debounced article search
- Filtering by NewsAPI categories
- Infinite-scroll pagination with retry support
- Local cache separated by feed, search query, and category
- Cached content displayed before a network refresh
- Skeleton, empty, offline, initial-error, and inline-error states
- Bottom navigation with independent Home, Categories, and Settings back stacks
- Persistent dark-theme and haptic-feedback preferences with an interactive pull-cord lamp
- Two-step demo sign-in, persisted user sessions, and logout that clears preferences and cached articles
- Support for `/v2/top-headlines`, `/v2/everything`, and
  `/v2/top-headlines/sources`

## Architecture

The project uses a modular, layered architecture. Presentation modules depend
on feature-domain contracts, while concrete network and database-backed
implementations live in the feature data module and are connected through
Hilt.

```mermaid
flowchart TD
    APP[app] --> HOME[screens:home]
    APP --> CATEGORIES[screens:categories]
    APP --> NAV[core:ui:navigation]

    HOME --> NEWS_DOMAIN[features:news:domain]
    CATEGORIES --> NEWS_DOMAIN
    APP --> NEWS_DATA[features:news:data]
    NEWS_DATA --> NEWS_DOMAIN

    NEWS_DATA --> NETWORK[core:network]
    NEWS_DATA --> DB[core:db]
    NEWS_DOMAIN --> DOMAIN[core:domain]

    HOME --> MVI[core:ui:mvi]
    CATEGORIES --> MVI
    HOME --> DESIGN[core:ui:designsystem]
    CATEGORIES --> DESIGN
```

### MVI

Every screen follows the same unidirectional state flow:

```text
UI intent -> MviViewModel -> partial state -> reducer -> StateFlow -> UI
                                |
                                +-> SharedFlow effect
```

- `Intent` represents a user or lifecycle action.
- `ViewModel` performs asynchronous work and emits a `Partial` result.
- `Reducer` is the single place where a `Partial` transforms immutable UI
  state.
- `StateFlow` exposes the current state to Compose.
- `Effect` is available for one-time events.
- `BaseScreen` provides shared loading, content, offline, and error-state
  handling.

### Data flow

For the initial page, a screen first requests articles stored for the current
`NewsRequest`. Cached content is rendered immediately when available, after
which the repository refreshes it from NewsAPI. Successful remote pages are
written to Room in a transaction.

The cache uses an independent context key for:

- the main feed;
- each normalized search query;
- each selected category.

Subsequent pages are requested as the list approaches its end and appended to
the current context. Network failures do not remove previously cached content.

### Navigation

`MainContainer` selects between `authFlow` and `regularFlow` from the persisted
user session. Only `regularFlow` contains the Home, Categories, and Settings
bottom-navigation destinations. Navigation is implemented with Navigation 3.
Each destination owns a separate `NavBackStack`, `ViewModelStore`, and saved
state, so switching tabs preserves its navigation and UI state. Leaving a flow
removes its entries and screen ViewModels; logout also resets all tab stacks.

### Demo authentication

Enter any nonblank username of up to 32 Unicode characters, the password
`news`, and then the random four-digit code delivered to the Android notification
panel. Android 13 and later request notification permission when Continue is
pressed. Notifications and the Sign-in codes channel must be enabled. Every login
and resend generates a new code; a code expires after five minutes and can be used
only once. Resending invalidates the previous code. The code verifies automatically
when all four digits are entered in the animated bottom sheet. Verification remains
local to this demo; no email or authentication server is used. The app stores only
the verified user's name in DataStore after the code
succeeds; passwords and codes stay in memory and are redacted from MVI logs.
Subsequent launches restore the user and enter the regular flow directly.

Settings displays the user's name and uppercase initial. Logout first opens a
Yes/No confirmation dialog. Confirming clears the
entire DataStore and every Room table, restores default preferences, and returns
to sign-in. A cache generation guard prevents pending news requests from
writing data from the previous session after cleanup.

### System notifications

`core:ui:interactions:domain` exposes a shared `NotificationManager`, bound as a
Hilt singleton by `core:ui:interactions:data`. Add the domain dependency to a
screen module and inject the manager into its ViewModel like the dialog or modal
manager. In a suspend intent handler, post a notification with localized strings
provided by the feature:

```kotlin
notificationManager.show(
    AppNotification(
        id = 100,
        channel = AppNotificationChannel(id = "news_updates", name = channelName),
        title = title,
        message = message,
    ),
)
```

Stable IDs identify notifications across the app: posting the same ID replaces
its notification. `dismiss(id)` removes one and `dismissAll()` clears all app
notifications. Blocking variants are available for synchronous callers. Channels
configure importance; each notification also supports visibility, timeout, and
auto-cancel on tap. Tapping a notification opens the app.

The manager dispatches directly to Android and works without an Activity or a
Compose collector. `show` returns `false` when notification permission, app
notifications, or the channel is disabled. The foreground screen owns the system
permission request; AuthScreen requests it when Continue is pressed. Verification
codes use the same manager through the feature's `AuthCodeNotifier` adapter.
Notification payloads are excluded from manager logs.

## Module structure

```text
app
├── Application, Hilt composition root, and AppLogger binding
├── MainActivity / MainContainer / MainViewModel
└── application configuration

core
├── domain
│   ├── shared models and DataResult/NetworkError
│   └── application-level abstractions
├── db
│   ├── Room database, DAO, entity, and database mapper
│   └── database Hilt module
├── logging
│   └── Timber initialization
├── network
│   ├── domain — API contract
│   └── data — Ktor client, logging, and network error mapping
├── storage
│   ├── domain — generic Storage contract and serializer extensions
│   └── data — DataStore, settings adapter, and Hilt bindings
└── ui
    ├── designsystem — shared light/dark theme, colors, typography, shapes, and components
    ├── mvi — MVI contracts, MviViewModel, and BaseScreen
    ├── interactions — haptic, dialog, modal and system notification managers, and themed Compose handlers
    └── navigation — destinations, manager, and Navigation 3 host

features
├── auth
│   ├── domain — user, challenge, authentication and session contracts
│   └── data — mock authentication, DataStore session, and logout cleanup
└── news
    ├── domain
    │   ├── NewsRepository
    │   └── local and remote data-source contracts
    └── data
        ├── NewsRepositoryImpl
        ├── NewsRemoteDataSourseImpl
        ├── NewsLocalDataSourseImpl
        ├── API DTOs and mappers
        └── Hilt bindings

screens
├── auth — sign-in and verification, following BaseScreen / MVI conventions
├── home — latest headlines, search, and pagination
├── categories — category selection and paginated articles
└── settings — user identity, logout, dark theme, haptic feedback, and animated pull-cord lamp
```

All three tabs use the same `NewsTheme` in `core:ui:designsystem`. Its light
ivory/green and dark navy/gold palettes also define lamp materials and lighting;
screens do not define their own colors. Settings follows the same `BaseScreen`,
`MviViewModel`, `model`, and `reducer` structure as Home and Categories.
`SettingsViewModel` writes preferences through the shared `AppSettingsStore`
domain contract. Its observable state keeps the app theme synchronized and is
persisted by `core:storage:data`. Storage supports JSON-serialized values with
blocking and suspend access. Serialization lets the generic API store primitive
values, collections, and custom objects. Existing SharedPreferences settings
migrate to DataStore on first use.

The launcher icon and native Android splash screen use the shared forest,
ivory, and gold palette. The splash fades into the app as soon as its first
frame is ready. The editable icon is `core/ui/designsystem/design/news-app-icon.svg`;
regenerate all legacy density variants on macOS with
`swift core/ui/designsystem/design/export-launcher-icons.swift`.
Adaptive, monochrome, and legacy launcher resources live in
`core/ui/designsystem/src/main/res`. The design system also owns every UI icon
through `NewsIcons` and the animated `NewsAnimatedCheckIcon` component.

## Main technology stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.4.21 |
| Build | Gradle 9.8.2, Android Gradle Plugin 9.4.1 |
| UI | Jetpack Compose BOM 2026.09.00, Material 3 |
| Architecture | MVI, repository pattern, modular layered architecture |
| Async/state | Kotlin Coroutines 1.11.0, Flow, StateFlow, SharedFlow |
| Navigation | AndroidX Navigation 3 1.2.0 |
| Dependency injection | Hilt 2.60.1 with KSP 2.3.12 |
| Networking | Ktor Client 3.6.0 with OkHttp engine |
| Serialization | Kotlinx Serialization 1.12.0 |
| Persistence | Room 2.8.5, DataStore 1.3.0 |
| Image loading | Coil 3.6.3 |
| Logging | Timber |
| Testing | JUnit 4, kotlinx-coroutines-test |

The application targets Android API 37, supports API 29 and newer, and uses
Java 11 bytecode compatibility.

## Configuration

1. Create an API key at [newsapi.org](https://newsapi.org/).
2. Set the local value in `gradle/libs.versions.toml`:

   ```toml
   [versions]
   baseUrl = "https://newsapi.org/v2"
   apiKey = "YOUR_NEWS_API_KEY"
   ```

The values are exposed to the app through generated `BuildConfig` fields. Do
not commit a production API key to a public repository.

## Build and verification

Open the project in Android Studio with support for Android Gradle Plugin 9.4.
The wrapper pins Gradle 9.8.2 and verifies its distribution checksum. Daemon JVM
criteria select JDK 25; JVM-only modules use a Java 11 toolchain. Foojay resolves
missing toolchains. The main Gradle commands are:

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Run JVM-module tests and Android debug unit tests
./gradlew test

# Run Android lint
./gradlew lintDebug

# Run the complete local verification used by the project
./gradlew test lintDebug :app:assembleDebug :app:assembleRelease

# Run instrumented tests on a connected emulator/device
./gradlew :app:connectedDebugAndroidTest :core:db:connectedDebugAndroidTest :core:storage:data:connectedDebugAndroidTest
```

The generated debug APK is available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Testing

The current unit-test suite covers:

- MVI reducers and ViewModel state transitions;
- pagination and retry scenarios;
- navigation-manager behavior;
- repository cache and remote-source coordination;
- Room entity mapping;
- API/domain mapping and network error classification;
- real Ktor/OkHttp requests against a local HTTP server, JSON decoding, headers,
  query encoding, and credential redaction.

Instrumented tests cover authentication, notifications, dialogs, verification
animations, settings, and navigation. Persistence upgrade tests read a frozen
Room 2.7.2 database schema and a DataStore 1.2.1 protobuf fixture, then verify
preservation, subsequent writes, reopening, and logout cleanup with the current
dependencies.

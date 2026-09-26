# Twoverse — project guide for Claude Code

Twoverse is a private Android app for long-distance couples: live distance,
a compass that points to the partner ("Your Star"), a reunion countdown
("Until We Meet"), and a private memory vault ("Ours").

- Product spec: `docs/SRS.md`
- UI spec: `docs/design/DESIGN.md` (read this before any UI work)
- Screen mockups: `docs/design/screens/*.dc.html` (visual reference, 1 px = 1 dp)

## Repo layout

```
twoverse/
├── android/     # Android Studio project (Kotlin, Jetpack Compose)
├── supabase/    # backend: migrations, edge functions (not started yet)
└── docs/        # SRS, design spec, mockups
```

## Tech stack

- Kotlin, Jetpack Compose, Material 3 (custom Twoverse theme, no dynamic colour)
- Architecture: MVVM + unidirectional data flow (UDF)
- DI: Hilt
- Navigation: Navigation Compose with type-safe routes (`@Serializable` route objects)
- Async: Kotlin Coroutines + StateFlow
- Images: Coil
- Widget (later): Jetpack Glance
- Backend (later): Supabase via supabase-kt
- Build: Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`)

## Package structure (`android/app/src/main/java/app/twoverse/`)

```
app/twoverse/
├── TwoverseApplication.kt        # @HiltAndroidApp
├── MainActivity.kt               # single activity, edge-to-edge
├── navigation/                   # routes + TwoverseNavHost + bottom bar wiring
├── core/
│   ├── designsystem/
│   │   ├── theme/                # Color.kt, Type.kt, Shape.kt, Spacing.kt, Theme.kt
│   │   └── component/            # reusable UI (buttons, cards, Planet, OrbitGraphic…)
│   ├── model/                    # plain data classes (Memory, Couple, PartnerLocation…)
│   ├── data/                     # repository interfaces + Fake implementations
│   └── common/                   # small utils (formatters, time)
└── feature/
    ├── splash/
    ├── onboarding/               # Welcome
    ├── auth/                     # SignIn
    ├── pairing/                  # Connect your worlds
    ├── home/                     # Our Universe
    ├── compass/                  # Your Star
    ├── countdown/                # Until We Meet
    ├── vault/                    # Ours list, Memory viewer, New memory
    ├── settings/                 # You & Her
    └── birthday/
```

Each feature folder contains:
- `XRoute.kt` – stateful entry: gets the ViewModel (`hiltViewModel()`), collects state with `collectAsStateWithLifecycle()`, passes state + lambdas down
- `XScreen.kt` – **stateless** composable: `XScreen(uiState, onAction…)`, fully previewable
- `XViewModel.kt` – `@HiltViewModel`, exposes `StateFlow<XUiState>`, handles events
- `XUiState.kt` – immutable UI state (sealed interface for Loading / Success / Error when needed)

Keep it one Gradle module for now, but respect these package boundaries so
features can move into separate modules later. Features never import other
features; shared things go in `core/`.

## Coding rules

- Never hardcode colours, font sizes or shapes in screens: use `TwoverseTheme` tokens.
- All user-visible text goes in `res/values/strings.xml`.
- Every screen and component gets `@Preview`s for **light and dark**.
- Composables take `modifier: Modifier = Modifier` as the first optional parameter.
- Hoist state; screens don't talk to repositories directly.
- Use `WindowInsets` for status/navigation bars (edge-to-edge), never hardcoded top padding.
- Touch targets at least 48dp; `contentDescription` on icon-only buttons.
- Until Supabase is added, all data comes from `Fake…Repository` classes bound in Hilt,
  so the UI is fully clickable with sample data.
- No secrets in code or Git. Config goes in `local.properties` / `BuildConfig`.
- Prefer small, focused files. No unused code or commented-out blocks.

## Commands (run from `android/`)

- Build: `./gradlew assembleDebug`
- Lint: `./gradlew lint`
- Unit tests: `./gradlew testDebugUnitTest`

Always make sure the project builds before saying a task is done.




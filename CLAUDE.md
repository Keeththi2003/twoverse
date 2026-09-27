# Twoverse — Guide for Claude Code

Twoverse is a private Android app for long-distance couples: live distance, a
compass that points to the partner ("Your Star"), a reunion countdown ("Until We
Meet"), and a private memory vault ("Ours").

## Source of truth

| Document | Read before |
|---|---|
| `docs/SRS.md` | Implementing or changing any product behaviour (requirement IDs like `FR-CMP-3`) |
| `docs/design/DESIGN.md` | Any UI or visual work |
| `docs/design/screens/*.dc.html` | Building a screen (visual reference only, 1 px = 1 dp; never copy HTML) |
| `docs/design/screens-png/` | Building a screen, if present |

If the code, SRS and design disagree, or a requirement is unclear, stop and ask.
Do not invent product behaviour.

---

## Agent rules

### Git: the user owns history
- NEVER run `git commit`, `git push`, `git tag`, create releases, stage files for committing, rewrite history, or run destructive commands (`reset --hard`, `clean -fd`, force operations).
- You MAY run read-only commands: `git status`, `git diff`, `git diff --check`, `git log`, and inspect branches.
- At the end of a task, review `git status` and `git diff` to confirm only intended files changed and no secrets or generated files were added, then suggest a commit message.

### Work style
1. Inspect the relevant code, docs and similar existing features first. Follow existing patterns.
2. For larger tasks, explain the plan before making extensive changes.
3. Make the smallest change that fully solves the task.
4. Do not refactor, rename, reformat, upgrade or delete anything unrelated to the task.
5. Do not add a dependency unless Android, Kotlin, Compose or an existing library can't do the job. Add it through `gradle/libs.versions.toml`.

### Verification (run from `android/`)
| Change | Check |
|---|---|
| Docs, strings, resources only | None needed |
| Any Kotlin change during work | `./gradlew :app:compileDebugKotlin` |
| New screen/feature, navigation, DI, Gradle, manifest, dependencies | `./gradlew assembleDebug` |
| ViewModel or data logic | `./gradlew testDebugUnitTest` |
| Before declaring a feature complete | `./gradlew assembleDebug lint` |

Never say something builds or passes unless you actually ran it and it succeeded.

---

## Project facts

- Package: `app.twoverse` · Min SDK 26 · Single Gradle module `:app` (for now)
- Primary test device: Samsung Galaxy phones
- Stack: Kotlin, Jetpack Compose, Material 3 with a custom Twoverse theme (no dynamic colour), MVVM + unidirectional data flow, Hilt, Navigation Compose with type-safe `@Serializable` routes, Coroutines + StateFlow, Coil
- Later: Jetpack Glance (widget), Supabase via supabase-kt, Firebase Cloud Messaging

## Repo layout

```
twoverse/
├── android/     # Android app
├── supabase/    # migrations, edge functions (not started)
└── docs/        # SRS, design spec, mockups
```

## Package structure

```
app/twoverse/
├── TwoverseApplication.kt      # @HiltAndroidApp
├── MainActivity.kt             # single activity, edge-to-edge
├── navigation/                 # routes, TwoverseNavHost, bottom bar wiring
├── core/
│   ├── designsystem/
│   │   ├── theme/              # Color, Type, Shape, Spacing, Theme
│   │   └── component/          # reusable UI (buttons, cards, Planet, OrbitGraphic…)
│   ├── model/                  # plain data classes (Memory, Couple, PartnerLocation…)
│   ├── data/                   # repository interfaces + Fake implementations + sample data
│   └── common/                 # formatters, time utilities
└── feature/
    ├── splash/  onboarding/  auth/  pairing/  home/
    ├── compass/  countdown/  vault/  settings/  birthday/
```

**Boundaries:** features never import other features. Shared code goes in `core/`.
Only `navigation/` may reference feature entry points (`XRoute` composables).

## Feature pattern

Each feature has:

- `XRoute.kt` — gets the ViewModel with `hiltViewModel()`, collects state with `collectAsStateWithLifecycle()`, passes state and callbacks to the screen.
- `XScreen.kt` — **stateless**: `XScreen(uiState, onAction…, modifier)`. Renders only; no repositories, no business logic. Fully previewable.
- `XViewModel.kt` — `@HiltViewModel`, exposes `StateFlow<XUiState>`, handles events, talks to repositories.
- `XUiState.kt` — immutable state. Use a sealed interface (Loading / Success / Error) only when the screen really has those states.

---

## Code rules

**UI**
- Use only Twoverse theme tokens and `core/designsystem/component/` components. No hardcoded colours, font sizes, shapes or dimensions in screens. Check for an existing component before creating a new one.
- Every screen and component has `@Preview`s for light and dark, using sample data (never real repositories).
- `modifier: Modifier = Modifier` is the first optional parameter.
- Edge-to-edge: handle system bars with `WindowInsets`, never hardcoded padding.
- Accessibility: touch targets ≥ 48dp, `contentDescription` on icon-only buttons, don't convey meaning by colour alone.

**Text**
- All user-visible text in `res/values/strings.xml`, named by feature: `home_distance_title`, `vault_empty_message`.

**Data**
- Until Supabase is added, all data comes from `Fake…Repository` classes bound in Hilt. No hardcoded data inside screens.
- Keep repository interfaces backend-agnostic so Supabase can replace the fakes without UI changes.

**Kotlin**
- Immutable state, small focused files and functions, clear names.
- No `!!`, no `GlobalScope`, no blocking calls on the main thread.
- No unused code, commented-out code, leftover debug logs or unintentional TODOs.
- New ViewModel logic gets unit tests.


## Backend (Supabase)

- All schema changes are SQL migrations in `supabase/migrations/`, created with
  `supabase migration new <name>`. Never edit a migration that has already been applied.
- Every table has Row Level Security enabled, with policies so users can only access
  their own couple's data (NFR-SEC-1). No table without RLS.
- Multi-step rules (pairing, disconnect, deletion) are Postgres functions called via RPC,
  so they run atomically on the server (NFR-SEC-3).
- Test migrations locally with `supabase start` / `supabase db reset` when Docker is available.
- NEVER run `supabase db push` or change the remote project without asking me first.
- The app uses only the publishable key (SUPABASE_PUBLISHABLE_KEY). The service-role key is never used in the Android app.
- Android talks to Supabase only through repository implementations in `core/data/`;
  keep the Fake repositories for previews and tests.

**Security**
- No secrets in code, resources or Git. Config goes in `local.properties` → `BuildConfig`.
- Never commit `local.properties`, keystores or `google-services.json`.

---

## Before finishing a task

Check that the work follows the SRS, DESIGN.md and the feature pattern, uses
tokens and string resources, has light/dark previews, meets accessibility rules,
touches only intended files, and was verified at the right level.

Reply in this format:

```
Implemented:
- …
Changed files:
- …
Verification:
- … (commands actually run and results)
Notes / open issues:
- …
Suggested commit message:
- …
Git: no commit created.
```

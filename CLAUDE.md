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

### Remote services: the user owns production
- NEVER run `supabase db push`, `supabase functions deploy`, `supabase secrets set`, or change the remote Supabase or Firebase project in any way. Tell me the exact command and I will run it.
- Local Supabase (`supabase start`, `supabase db reset`, `supabase test db`) is fine.

### Work style
1. Inspect the relevant code, docs and similar existing features first. Follow existing patterns.
2. For larger tasks, explain the plan before making extensive changes, unless I say to implement directly.
3. Make the smallest change that fully solves the task.
4. Do not refactor, rename, reformat, upgrade or delete anything unrelated to the task.
5. Do not add a dependency unless Android, Kotlin, Compose or an existing library can't do the job. Add it through `gradle/libs.versions.toml`.

### Verification
| Change | Check |
|---|---|
| Docs, strings, resources only | None needed |
| Any Kotlin change during work | `./gradlew :app:compileDebugKotlin` (from `android/`) |
| New screen/feature, navigation, DI, Gradle, manifest, dependencies | `./gradlew assembleDebug` |
| ViewModel, repository or data logic | `./gradlew testDebugUnitTest` |
| New or changed migration | `supabase db reset` (from repo root, local stack) |
| RLS policies or database functions | `supabase test db` |
| Before declaring a feature complete | `./gradlew assembleDebug lint` (+ database checks if backend changed) |

If Docker or the local Supabase stack isn't running, say so instead of skipping silently.
Never say something builds or passes unless you actually ran it and it succeeded.

---

## Project facts

- Package: `app.twoverse` · Min SDK 26 · Single Gradle module `:app` (for now)
- Primary test device: Samsung Galaxy phones
- App stack: Kotlin, Jetpack Compose, Material 3 with a custom Twoverse theme (no dynamic colour), MVVM + unidirectional data flow, Hilt, Navigation Compose with type-safe `@Serializable` routes, Coroutines + StateFlow, Coil, DataStore
- Backend: Supabase (Auth, Postgres with RLS, Storage, Realtime, Edge Functions, pg_cron) via supabase-kt
- Push notifications: Firebase Cloud Messaging only (no other Firebase features; no Firebase Auth)
- Sign-in: Google (Credential Manager → ID token → Supabase Auth) and email/password
- Later: Jetpack Glance widget with real data
- Supabase region: Singapore

## Repo layout

```
twoverse/
├── android/                 # Android app
├── supabase/
│   ├── migrations/          # SQL migrations (schema, RLS, functions)
│   ├── functions/           # Edge Functions (TypeScript/Deno)
│   ├── tests/               # pgTAP database tests
│   └── config.toml
└── docs/                    # SRS, design spec, mockups
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
│   │   └── component/          # reusable UI
│   ├── model/                  # domain data classes used by the UI (Memory, Couple…)
│   ├── data/
│   │   ├── repository/         # repository interfaces
│   │   ├── supabase/           # Supabase implementations + DTOs + mappers
│   │   ├── fake/               # Fake implementations + sample data
│   │   └── di/                 # Hilt modules binding interfaces to implementations
│   └── common/                 # formatters, time utilities, Result/error types
└── feature/
    ├── splash/  onboarding/  auth/  pairing/  home/
    ├── compass/  countdown/  vault/  settings/  star/  orbit/
```

**Boundaries:** features never import other features. Shared code goes in `core/`.
Only `navigation/` may reference feature entry points (`XRoute` composables).
Features and ViewModels depend only on repository interfaces, never on Supabase classes.

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
- Backend errors are mapped to the user messages in SRS section 7; never show raw error text.

**Data layer**
- The app uses Supabase implementations bound in Hilt. Fake implementations stay for previews, unit tests, and any feature not yet connected to the backend.
- Supabase DTOs (`@Serializable`, `@SerialName` for snake_case columns) live in `core/data/supabase/` and are mapped to `core/model/` classes. DTOs never reach the UI.
- Repositories return domain models and a typed result/error, not Supabase exceptions.
- Realtime subscriptions are exposed as `Flow` and cancelled with the collector's scope.
- Times are stored and sent in UTC; convert to local time only for display.

**Kotlin**
- Immutable state, small focused files and functions, clear names.
- No `!!`, no `GlobalScope`, no blocking calls on the main thread.
- No unused code, commented-out code, leftover debug logs or unintentional TODOs.
- New ViewModel and repository logic gets unit tests (using fakes).

## Backend (Supabase)

- All schema changes are SQL migrations in `supabase/migrations/`, created with `supabase migration new <name>`. Never edit a migration that has already been applied to the remote project; add a new one.
- Naming: snake_case, plural table names, `timestamptz` for all times, UUID primary keys.
- Every table has Row Level Security enabled, with policies so users can only access their own couple's data (NFR-SEC-1). No table without RLS. Write policies for the `authenticated` role.
- Multi-step rules (pairing, disconnect, deletion) are Postgres functions called via RPC, so they run atomically on the server (NFR-SEC-3). `security definer` functions must check `auth.uid()` and set `search_path`.
- Every RLS policy and RPC function gets pgTAP tests in `supabase/tests/`, including a test that another couple's user cannot access the data.
- Storage: private buckets only; access through storage policies and signed URLs.
- Edge Functions live in `supabase/functions/` and read secrets from environment variables only.

## Security

- No secrets in code, resources or Git.
- Android config goes in `android/local.properties` → `BuildConfig`: `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `GOOGLE_WEB_CLIENT_ID`.
- The app uses only the publishable key. The secret key (`sb_secret_…`), legacy service-role key, and Firebase service-account key never appear in the app or the repo; they live only in Supabase Edge Function secrets.
- Never commit `local.properties`, `.env` files, keystores or `google-services.json`.
- Never log tokens, keys, locations or photo URLs.

---

## Before finishing a task

Check that the work follows the SRS, DESIGN.md and the feature pattern, uses
tokens and string resources, has light/dark previews, meets accessibility rules,
keeps RLS on every table, touches only intended files, and was verified at the
right level.

Reply in this format:

```
Implemented:
- …
Changed files:
- …
Verification:
- … (commands actually run and results)
Commands for me to run (remote changes):
- … (e.g. supabase db push), or "none"
Notes / open issues:
- …
Suggested commit message:
- …
Git: no commit created.
```
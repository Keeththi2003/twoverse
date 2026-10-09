<div align="center">

<img src="docs/assets/logo.png" alt="Twoverse logo" width="96">

<h1>Twoverse</h1>

A private Android app for long-distance couples.

<!-- version:start -->
<img src="https://img.shields.io/badge/version-1.0.0-informational" alt="Version 1.0.0">
<!-- version:end -->
<img src="https://img.shields.io/badge/platform-Android-3DDC84" alt="Platform: Android">
<img src="https://img.shields.io/badge/Android-9.0%2B-blue" alt="Android 9.0+ (min SDK 28)">
<img src="https://img.shields.io/badge/Kotlin-2.4.20-7F52FF" alt="Kotlin 2.4.20">
<img src="https://img.shields.io/badge/license-Proprietary-lightgrey" alt="License: Proprietary">

<a href="https://github.com/Keeththi2003/twoverse/releases/latest/download/twoverse.apk"><img src="https://img.shields.io/badge/Download-latest%20APK-3DDC84" alt="Download latest APK"></a>
&nbsp;
<a href="https://github.com/Keeththi2003/twoverse/releases">All releases</a>

<p>
  <img src="docs/screenshots/home.png" alt="Our Universe (home)" width="200">
  <img src="docs/screenshots/your-star.png" alt="Your Star" width="200">
  <img src="docs/screenshots/our-orbit.png" alt="Our Orbit" width="200">
  <img src="docs/screenshots/widget.png" alt="Widget" width="200">
</p>

</div>

## Features

- **Live distance**: the distance to your partner, with how recent their location is.
- **Your Star**: a compass that points toward your partner.
- **Until We Meet**: a shared countdown to the next reunion.
- **Ours**: a private photo vault with optional expiry, screenshot blocking and a biometric lock.
- **Shooting Stars**: surprise messages your partner sees once, now or at a chosen time.
- **Our Orbit**: days together, milestones and the history of your meetups.
- **Widget**: a home-screen widget in small, medium and large sizes.

## Download and install

Download the [latest APK](https://github.com/Keeththi2003/twoverse/releases/latest/download/twoverse.apk) on an Android 9.0+ phone, open it and allow installs from that source when asked. The repository is private, so the download requires access to it.

New versions install over the existing app and keep your data.

## Tech stack

| Area | Technology |
|---|---|
| App | Kotlin 2.4.20, Jetpack Compose (BOM 2026.02.01), Material 3, Hilt 2.60.1, Navigation Compose 2.10.2, Coroutines and StateFlow, Coil 3.6.3, DataStore 1.2.1, WorkManager 2.12.0, Glance 1.2.0 |
| Build | Android Gradle Plugin 9.3.1, Gradle 9.5.0, KSP 2.3.12, compile SDK 37, target SDK 36, min SDK 28 |
| Backend | Supabase (Auth, Postgres 17 with Row Level Security, Storage, Realtime, Edge Functions, pg_cron) via supabase-kt 3.8.0 |
| Sign-in | Google through Credential Manager 1.6.0, and email and password |
| Notifications | Firebase Cloud Messaging (Firebase BoM 34.19.0), sent by the `send-push` Edge Function |

## Architecture

- MVVM with unidirectional data flow. Each screen has a stateless `XScreen`, an `XRoute` that connects it to a Hilt `XViewModel`, and an immutable `XUiState`.
- Features live in their own packages under `feature/` and never import each other. Shared code is in `core/`.
- ViewModels depend on repository interfaces. Supabase implementations are bound with Hilt; fake implementations back previews and unit tests.
- Every table has Row Level Security so users can only read their own couple's data. Multi-step rules such as pairing, disconnecting and deleting an account run as Postgres functions.

Conventions and coding rules are in [CLAUDE.md](CLAUDE.md).

## Getting started

### Prerequisites

- Android Studio with support for Android Gradle Plugin 9.3.1
- JDK 21
- [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started) and Docker

### Setup

```sh
git clone https://github.com/Keeththi2003/twoverse.git
cd twoverse
```

Create `android/local.properties` with these keys:

| Key | Description |
|---|---|
| `SUPABASE_URL` | URL of the Supabase project |
| `SUPABASE_PUBLISHABLE_KEY` | The project's publishable key |
| `GOOGLE_WEB_CLIENT_ID` | OAuth web client ID used for Google sign-in |

Place the Firebase config file at `android/app/google-services.json`.

Start the local backend from the repository root:

```sh
supabase start      # requires Docker
supabase db reset   # apply all migrations
```

## Build and test

From `android/`:

```sh
./gradlew assembleDebug        # debug build
./gradlew testDebugUnitTest    # unit tests
./gradlew lint                 # Android lint
```

From the repository root, with the local Supabase stack running:

```sh
supabase test db               # database, RLS and function tests
```

## Releasing

Releases follow [Semantic Versioning](https://semver.org/) and are recorded in [CHANGELOG.md](CHANGELOG.md).

```sh
scripts/release.sh X.Y.Z       # updates version, README badge and changelog
git commit -am "Release X.Y.Z"
git tag -a vX.Y.Z -m "Twoverse X.Y.Z"
git push origin main vX.Y.Z
```

Pushing the tag runs `.github/workflows/release.yml`, which tests, builds and signs the APK and publishes it as a GitHub release. The workflow reads its configuration and signing keys from secrets in the `production` environment.

## Security and privacy

- Secrets, `local.properties`, `google-services.json` and keystores are never committed.
- The app uses only the Supabase publishable key.
- Row Level Security limits each user to their own couple's data.
- Only the latest location is stored, with no history. Location sharing is off by default.
- Photos are kept in a private bucket and are never saved to the device gallery.
- No analytics or advertising SDKs.

## Documentation

- [Software requirements](docs/SRS.md)
- [Design spec](docs/design/DESIGN.md)
- [Changelog](CHANGELOG.md)

## License

Proprietary. All rights reserved. See [LICENSE](LICENSE).

## Author

K.Keeththigan

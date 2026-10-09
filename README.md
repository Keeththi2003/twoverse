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

The repository is private, so downloads require access to the repository.

<p>
  <img src="docs/screenshots/home.png" alt="Our Universe (home)" width="200">
  <img src="docs/screenshots/your-star.png" alt="Your Star" width="200">
  <img src="docs/screenshots/our-orbit.png" alt="Our Orbit" width="200">
  <img src="docs/screenshots/widget.png" alt="Widget" width="200">
</p>

</div>

## Contents

- [Features](#features)
- [Download and install](#download-and-install)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Getting started](#getting-started)
- [Build and run](#build-and-run)
- [Testing](#testing)
- [Releasing](#releasing)
- [Security](#security)
- [Privacy](#privacy)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Live distance**: the distance to your partner, with how recent their location is.
- **Your Star**: a compass that points toward your partner.
- **Until We Meet**: a shared countdown to the next reunion.
- **Ours**: a private photo vault with optional expiry, screenshot blocking and a biometric lock.
- **Shooting Stars**: surprise messages your partner sees once, now or at a chosen time.
- **Our Orbit**: days together, milestones and the history of your meetups.
- **Widget**: a home-screen widget in small, medium and large sizes.

## Download and install

1. Open the [latest APK](https://github.com/Keeththi2003/twoverse/releases/latest/download/twoverse.apk) on your phone while signed in to a GitHub account with access to the repository. Older versions are on the [releases page](https://github.com/Keeththi2003/twoverse/releases).
2. Open the downloaded file. Android asks you to allow installs from the app you opened it with (for example the browser or Files). Allow it for that app, then go back and tap Install.
3. Requires Android 9.0 or later (min SDK 28).

**Updating.** Install the new APK over the existing app. Your data stays. Android accepts the update only if it is signed with the same key and has a higher version code, which every release does. A debug build, or a build signed with another key, must be uninstalled first.

**Verifying the download.** Each release includes a `.sha256` file next to the APK. Put both files in the same folder and run:

```sh
shasum -a 256 -c twoverse.apk.sha256    # macOS
sha256sum -c twoverse.apk.sha256        # Linux
```

On Windows, run `Get-FileHash twoverse.apk -Algorithm SHA256` in PowerShell and compare the result with the contents of the `.sha256` file. The versioned files (`twoverse-X.Y.Z.apk` and its `.sha256`) are checked the same way.

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
- JDK 21 for the Gradle daemon (the app compiles to Java 17)
- [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started)
- Docker, for the local Supabase stack

### Clone

```sh
git clone https://github.com/Keeththi2003/twoverse.git
cd twoverse
```

### Configure the app

Create `android/local.properties` with these keys:

| Key | Description |
|---|---|
| `SUPABASE_URL` | URL of the Supabase project |
| `SUPABASE_PUBLISHABLE_KEY` | The project's publishable key (never the secret or service-role key) |
| `GOOGLE_WEB_CLIENT_ID` | OAuth web client ID used for Google sign-in |

Place the Firebase config file at `android/app/google-services.json`.

### Local Supabase

From the repository root:

```sh
supabase start      # start the local stack (requires Docker)
supabase db reset   # apply all migrations to the local database
supabase stop       # stop the stack
```

The `send-push` Edge Function reads `FIREBASE_SERVICE_ACCOUNT` and `PUSH_INTERNAL_SECRET` from its environment. Scheduled jobs read `project_url`, `push_internal_secret` and `storage_service_key` from Supabase Vault.

## Build and run

From `android/`:

```sh
./gradlew assembleDebug
./gradlew installDebug
```

Or open `android/` in Android Studio and run the `app` configuration.

## Testing

From `android/`:

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew lint                # Android lint
```

From the repository root, with the local Supabase stack running:

```sh
supabase test db   # pgTAP tests for schema, RLS policies and functions
```

## Releasing

Twoverse follows [Semantic Versioning](https://semver.org/). Releases are tagged `vMAJOR.MINOR.PATCH`, `versionName` in `android/app/build.gradle.kts` matches the tag, and `versionCode` increases with every release. Changes are recorded in [CHANGELOG.md](CHANGELOG.md). Releases are published on GitHub only.

### Steps

1. Add the changes to the `[Unreleased]` section of `CHANGELOG.md`.
2. From the repository root, run `scripts/release.sh X.Y.Z`. It checks that the version is greater than the current one and that `[Unreleased]` is not empty. It then sets `versionName`, increments `versionCode`, updates the version badge in this README, moves the `[Unreleased]` items under `## [X.Y.Z] - date` and updates the compare links in the changelog. It does not commit, tag or push.
3. Review the result with `git diff`.
4. Commit, tag and push. The script prints the exact commands. If `main` is protected, merge the branch into `main` first and create the tag on `main`:

   ```sh
   git tag -a vX.Y.Z -m "Twoverse X.Y.Z"
   git push origin vX.Y.Z
   ```

### What the release workflow does

Pushing a tag such as `v1.0.0` starts `.github/workflows/release.yml`, which runs in the `production` environment:

1. Checks that the tag, `versionName` and the `CHANGELOG.md` section for that version agree, and takes the release notes from the changelog.
2. Writes `local.properties` and `google-services.json` from secrets and restores the release keystore.
3. Runs the unit tests and lint (`testDebugUnitTest`, `lintDebug`).
4. Builds the signed release APK (`assembleRelease`).
5. Verifies the APK signature with `apksigner` and creates the SHA-256 checksums.
6. Uploads the R8 `mapping.txt` as a private workflow artifact. It expires after 90 days, so download it from the workflow run and store it safely. It is needed to read obfuscated crash stack traces.
7. Creates the GitHub release with `twoverse-X.Y.Z.apk`, `twoverse.apk` and their `.sha256` files. `twoverse.apk` always points to the newest version through `/releases/latest/download/twoverse.apk`.

### Required secrets

Add these as secrets of the `production` environment (Settings, Environments). Never commit them.

| Secret | Content |
|---|---|
| `SUPABASE_URL` | Same as in `local.properties` |
| `SUPABASE_PUBLISHABLE_KEY` | Same as in `local.properties` |
| `GOOGLE_WEB_CLIENT_ID` | Same as in `local.properties` |
| `GOOGLE_SERVICES_JSON` | The full text of `google-services.json` |
| `RELEASE_KEYSTORE_BASE64` | The release keystore, base64 encoded |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Create the base64 value of the keystore with one of these:

```sh
base64 -i release.jks | tr -d '\n'    # macOS
base64 -w0 release.jks                # Linux
```

Keep a separate backup of the keystore and its passwords. Without them, no update can be installed over an existing install.

### Local release build

Release signing values are read from `android/local.properties`:

| Key | Description |
|---|---|
| `RELEASE_STORE_FILE` | Path to the keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

```sh
./gradlew assembleRelease
```

Without all four keys the release APK is built unsigned. Release builds are optimized with the rules in `android/app/proguard-rules.pro`.

## Security

Secrets are never committed. These files are ignored by Git:

- `android/local.properties`
- `android/app/google-services.json`
- Keystores under `android/`: `*.jks`, `*.keystore`, `*.p12`
- Environment files under `android/`: `.env`, `.env.*`
- Supabase secrets: `supabase/.env`, `supabase/functions/.env`, `.env.keys`, `.env.local`, `.env.*.local`

The app uses only the Supabase publishable key. The secret key and the Firebase service account exist only as Edge Function secrets.

## Privacy

- Only each user's latest location is stored. There is no location history. Sharing is off until the user turns it on and can be turned off at any time in Settings. The default precision is approximate (rounded to about 1 km).
- Photos are stored in a private bucket and shown only through short-lived signed URLs. Received photos are cached in app-internal storage and are not written to the device gallery.
- The app has no analytics or advertising SDKs. Firebase is used only for push notifications.
- Row Level Security on every table limits each user to their own couple's data.

The requirements behind these points are in [docs/SRS.md](docs/SRS.md) (FR-LOC-7, NFR-PRV-1 to NFR-PRV-3, NFR-SEC-1, NFR-SEC-2).

## Troubleshooting

**Google sign-in fails.** Add the SHA-1 fingerprints of both the debug keystore and the release keystore to the Android app in the Firebase project, then download the updated `google-services.json`. Print the debug fingerprint with `./gradlew :app:signingReport` from `android/`, and the release fingerprint with `keytool -list -v -keystore <release.jks>`. Check also that `GOOGLE_WEB_CLIENT_ID` is the OAuth web client ID.

**Background location stops on Samsung phones.** Samsung can pause the app in the background. Set Settings, Apps, Twoverse, Battery to Unrestricted, and choose "Allow all the time" for location. Background updates run at most every 15 minutes or after about 1 km of movement.

**Notifications do not arrive.** Check that notifications are allowed for Twoverse in the system settings (Android 13 and later ask for this permission) and that the phone is online. The device's push token is registered with the backend while a user is signed in, so open the app once while signed in and online.

**The widget does not update.** Android limits how often widgets refresh, and battery restrictions make it worse, so set the battery use to Unrestricted as above. The widget shows a placeholder state when the phone is not paired, location sharing is off, the phone is offline, or no reunion date is set. If it still shows old data, remove the widget and add it again.

## Documentation

- [Software requirements (SRS)](docs/SRS.md): product behaviour and requirement IDs.
- [Design spec](docs/design/DESIGN.md): colours, typography, components and screens.
- [CHANGELOG.md](CHANGELOG.md): what changed in each release.
- [CLAUDE.md](CLAUDE.md): coding conventions and rules for the AI coding agent.

## Contributing

This is a private project and external contributions are not accepted.

## License

Proprietary. All rights reserved. See [LICENSE](LICENSE).

## Author

K.Keeththigan

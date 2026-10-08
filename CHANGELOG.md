# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-10-09

### Added

- Sign-in with Google and with email and password, password reset by email, and persistent sessions.
- Pairing with a one-time couple code, disconnecting, and reconnecting within 7 days of a disconnect.
- Location sharing with approximate or precise precision, background updates, and a silent wake-up push that asks the partner's phone for a fresh location.
- Our Universe (home) with the live distance, freshness, the partner's direction and the reunion countdown.
- Your Star: a compass that points toward the partner, with a calibration hint.
- Until We Meet: a shared reunion countdown with date, time, place and note.
- Ours: a private photo vault with captions, filters, temporary memories that expire, hiding received memories, screenshot blocking and an optional biometric lock.
- Shooting Stars: surprise messages with templates, layouts and an optional photo, shown once on the partner's next app open or at a chosen time, with a list of sent and received stars.
- Our Orbit: the "together since" date, days together, day milestones, and meetups that can be added, edited and recorded from a passed reunion.
- Profiles with full and short names, pronouns, an optional phone number, sharing choices for email and phone, and a private nickname for the partner. An "About you" step after sign-in asks for the short name and pronouns.
- Partner texts written per pronoun from string resources, using the partner's nickname or short name.
- Push notifications through Firebase Cloud Messaging for new memories, the partner joining, the reunion day, Shooting Stars, anniversaries and day milestones.
- Home-screen widget in small, medium and large sizes with the distance, the reunion and Our Orbit, following the app's Appearance setting.
- Settings for location sharing and precision, Lock Ours, distance unit, appearance (System, Light, Dark), log out, disconnect and account deletion, with an About section showing the app version.
- Offline cache that shows the latest saved data with an offline banner.
- Light and dark themes, adaptive launcher icons and an animated splash screen.
- Supabase backend: SQL migrations, Postgres functions for pairing, disconnecting, reconnecting and account deletion, the `send-push` Edge Function, scheduled purge and notification jobs, and pgTAP tests.

### Fixed

- Distances of 100 and above use the locale's thousands separator.

### Security

- Row Level Security on every table, limiting users to their own couple's data, with pgTAP tests for each policy and function.
- Photos stored in a private bucket and served through signed URLs.
- Phone numbers and emails visible to the partner only when the user shares them; nicknames visible only to the user who set them.
- Saved data and cached photos cleared on sign-out.
- Release signing and app configuration read from `local.properties`, which is not committed.


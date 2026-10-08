# Software Requirements Specification — Twoverse

| | |
|---|---|
| Version | 2.3 |
| Status | MVP scope agreed |
| Platform | Android (min SDK 26), primary devices Samsung Galaxy |
| Package | `app.twoverse` |

---

## 1. Introduction

### 1.1 Purpose
This document defines what Twoverse must do for its first release (the
"birthday MVP") and the quality it must meet. It is the reference for design,
development and testing. Requirement IDs (e.g. `FR-CMP-3`) are stable and are
used in issues, commits and code reviews.

### 1.2 Product vision
> A private universe for two.

Twoverse is a private space shared by exactly two people in a long-distance
relationship. It helps them feel close through three things: knowing how far
apart they are and in which direction, counting down to their next meeting,
and keeping private memories that only they can open.

It is not a social network or a messaging app.

### 1.3 Definitions

| Term | Meaning |
|---|---|
| Couple | A private connection between exactly two users |
| Partner | The other user in the couple |
| Our Universe | The home screen |
| Your Star | The compass that points toward the partner |
| Until We Meet | The shared reunion countdown |
| Ours | The private memory vault |
| Memory | A photo with optional caption shared inside Ours |
| Couple code | One-time code used to connect two accounts |
| Wake-up ping | A silent push that asks the partner's phone for a fresh location |
| Shooting Star | A surprise message one partner prepares for the other, shown once on a later app open |
| Together since | The date the relationship began. Not the date the couple paired in Twoverse |
| Meetup | A time the couple met in person: a start date, an optional end date, place and note |
| Our Orbit | The screen showing how long the couple has been together and their meetup history |
| Freshness | How recent the partner's last location update is |

### 1.4 References
- `docs/design/DESIGN.md` — UI specification
- `docs/design/screens/` — screen mockups
- `CLAUDE.md` — engineering rules

---

## 2. Overall description

### 2.1 Product perspective
A native Android app backed by Supabase (auth, Postgres database, storage,
realtime, edge functions, scheduled jobs) and Firebase Cloud Messaging for push
notifications. No custom server.

### 2.2 Users
Two users with equal permissions. There is no admin role. The user who creates
the couple code has no extra rights.

### 2.3 Operating environment
- Android 8.0+ phones; tested on current Samsung Galaxy devices
- Internet required for sync; the app remains usable offline with cached data
- Location, notification and (optionally) biometric permissions

### 2.4 Constraints
- Free-tier infrastructure for the MVP (Supabase Free, FCM)
- Android limits background location to periodic updates; true continuous
  tracking is not a goal
- Distribution for the MVP by direct APK install (no Play Store required)

### 2.5 Assumptions
- Both partners install the app and grant location permission voluntarily.
- Screenshots and screen recording can be blocked inside the app, but a photo
  of the screen taken with another device cannot be prevented.

---

## 3. Functional requirements

Priority: **M** = must have for MVP, **S** = should have, **C** = could have.

### 3.1 Splash and onboarding (`FR-ONB`)

| ID | Requirement | P |
|---|---|---|
| FR-ONB-1 | Show a branded splash screen (orbit animation, "Connecting your worlds…") while the app loads session state. | M |
| FR-ONB-2 | On first launch, show a Welcome screen with a short description and actions "Get started" and "I have a couple code". | M |
| FR-ONB-3 | Logged-in, paired users skip onboarding and open Our Universe. | M |

### 3.2 Authentication (`FR-AUTH`)

| ID | Requirement | P |
|---|---|---|
| FR-AUTH-1 | Users can sign up and sign in with Google. | M |
| FR-AUTH-2 | Users can sign up and sign in with email and password. | S |
| FR-AUTH-3 | Users can reset a forgotten password by email. | S |
| FR-AUTH-4 | Sessions persist until the user logs out. | M |
| FR-AUTH-5 | Users provide a display name at sign-up. | M |

### 3.3 Pairing (`FR-PAIR`)

| ID | Requirement | P |
|---|---|---|
| FR-PAIR-1 | An unpaired user can generate a couple code (format `XXXX-XXXX`), valid for 24 hours and usable once. | M |
| FR-PAIR-2 | The user can share the code through the Android share sheet or copy it. | M |
| FR-PAIR-3 | The other user can enter the code to join. Joining is atomic: the code is validated, both users are linked, and the code is consumed in one operation. | M |
| FR-PAIR-4 | A user can belong to only one active couple. | M |
| FR-PAIR-5 | Invalid, expired or used codes show a clear error. | M |
| FR-PAIR-6 | Both users see a "You're connected" confirmation when pairing completes. | M |
| FR-PAIR-7 | Either user can disconnect after a confirmation that explains the consequences (see BR-9). | M |

### 3.4 Location and distance (`FR-LOC`)

| ID | Requirement | P |
|---|---|---|
| FR-LOC-1 | Before requesting permission, explain that location is used only for distance and Your Star. | M |
| FR-LOC-2 | Location sharing is off until the user turns it on, and can be turned off at any time in Settings. | M |
| FR-LOC-3 | While the app is open, the user's location updates every few seconds and is pushed to the backend. | M |
| FR-LOC-4 | In the background, location updates at most every 15 minutes (WorkManager) or on significant movement. | M |
| FR-LOC-5 | When a user opens the app, send a wake-up ping so the partner's phone uploads a fresh location. | M |
| FR-LOC-6 | Location precision setting: **Approximate** (rounded to about 1 km, default) or **Precise**. | S |
| FR-LOC-7 | Only each user's latest location is stored. No location history. | M |
| FR-LOC-8 | Calculate distance from both latest locations (great-circle distance). | M |
| FR-LOC-9 | Show distance in km or miles based on the user's setting (one decimal under 100, whole numbers above). | M |
| FR-LOC-10 | Show freshness: **Live** (≤ 2 min), **Recent** (≤ 30 min, "Updated X min ago"), **Outdated** (> 30 min, "Last seen …"), **Unavailable** (sharing off or no data). Thresholds are configurable constants. | M |
| FR-LOC-11 | If either location is unavailable, show "Distance unavailable" instead of a number. | M |
| FR-LOC-12 | The home screen shows each partner's city name when available. | C |

### 3.5 Your Star — compass (`FR-CMP`)

| ID | Requirement | P |
|---|---|---|
| FR-CMP-1 | Compute the initial bearing from the user's location to the partner's location. | M |
| FR-CMP-2 | Read device orientation from the rotation vector sensor and correct magnetic to true north (`GeomagneticField`). | M |
| FR-CMP-3 | Rotate the needle so it points toward the partner in real time as the phone turns. | M |
| FR-CMP-4 | Show distance, compass direction (e.g. "North-east") and bearing in degrees. | M |
| FR-CMP-5 | When sensor accuracy is low, show a calibration hint. | M |
| FR-CMP-6 | When the partner's location is Outdated, label the needle as "last known direction". When Unavailable, hide the needle and explain why. | M |
| FR-CMP-7 | Short haptic feedback when the phone points within ±5° of the partner. | C |
| FR-CMP-8 | Devices without a compass sensor show the direction as text only. | S |

### 3.6 Until We Meet — countdown (`FR-CNT`)

| ID | Requirement | P |
|---|---|---|
| FR-CNT-1 | Either partner can set, edit or clear the next reunion date, with optional time, place and note. | M |
| FR-CNT-2 | Both partners see the same countdown (synced through the backend). | M |
| FR-CNT-3 | Show days prominently, plus hours, minutes and seconds. | M |
| FR-CNT-4 | Show the countdown summary on the home screen. | M |
| FR-CNT-5 | When the countdown reaches zero, show a celebration state; afterwards prompt "When's the next time?". | S |
| FR-CNT-6 | Times are stored in UTC and displayed in each user's local time zone. | M |
| FR-CNT-7 | With no date set, show an empty state inviting the user to set one. | M |

### 3.7 Ours — memory vault (`FR-VLT`)

| ID | Requirement | P |
|---|---|---|
| FR-VLT-1 | Show all accessible memories in a grid, newest first, with a count. | M |
| FR-VLT-2 | Filters: All, From you, From her/him, Expiring. | S |
| FR-VLT-3 | Unviewed memories show a "new" indicator. | M |
| FR-VLT-4 | Temporary memories show a remaining-time badge. | M |
| FR-VLT-5 | Opening Ours requires device authentication (biometric or device PIN) when "Lock Ours" is on (default on). | M |
| FR-VLT-6 | Vault, memory viewer and new memory screens set `FLAG_SECURE` to block screenshots and screen recording. | M |
| FR-VLT-7 | Received photos are cached only in app-internal storage and never written to the device gallery. | M |
| FR-VLT-8 | The memory viewer shows the photo, sender, date, caption and expiry. | M |
| FR-VLT-9 | Empty state when there are no memories. | M |

### 3.8 Creating memories (`FR-MEM`)

| ID | Requirement | P |
|---|---|---|
| FR-MEM-1 | The user picks a photo with the Android photo picker. | M |
| FR-MEM-2 | Photos are resized (long edge ≤ 1080 px) and compressed before upload. | M |
| FR-MEM-3 | Optional caption, up to 500 characters. | M |
| FR-MEM-4 | Expiry options: Never, 24 hours, 7 days, 30 days. Default: Never. | M |
| FR-MEM-5 | For temporary memories, the sender chooses whether the partner may keep it forever. | S |
| FR-MEM-6 | Upload shows progress; failures show a retry option without losing the caption. | M |
| FR-MEM-7 | The partner receives a notification (see FR-NOT). | M |

### 3.9 Expiry and deletion (`FR-DEL`)

| ID | Requirement | P |
|---|---|---|
| FR-DEL-1 | Expired memories become inaccessible immediately (enforced by the backend), and their files are deleted by a scheduled job. | M |
| FR-DEL-2 | "Keep forever" (when allowed) removes the expiry for that memory. | S |
| FR-DEL-3 | Either partner can delete a memory after confirmation; it is removed for both, including the stored file. | M |
| FR-DEL-4 | Deleted or expired memories are also removed from the local cache. | M |

### 3.10 Notifications (`FR-NOT`)

| ID | Requirement | P |
|---|---|---|
| FR-NOT-1 | Notify on new memory: "You received a new memory" with no photo or caption preview. | M |
| FR-NOT-2 | Notify when the partner joins the couple. | M |
| FR-NOT-3 | Notify one day before a temporary memory expires. | C |
| FR-NOT-4 | Notify on the reunion day. | S |
| FR-NOT-5 | The wake-up ping is a silent data message with no visible notification. | M |
| FR-NOT-6 | Notify both partners on their anniversary ("Happy anniversary ✨ 2 years in orbit") and on day milestones (FR-ORB-6), at 08:00 in each partner's time zone. At most one such notification per person per day; the anniversary wins when both fall on the same day. Only the type and the number are sent. | S |

### 3.11 Home-screen widget (`FR-WGT`)

| ID | Requirement | P |
|---|---|---|
| FR-WGT-1 | Widget shows distance, freshness and days until the next reunion. It adapts to its size: **small (2×2)** planets with the dashed arc, distance and freshness; **medium (4×2)** distance, "apart" and freshness beside the planets, with the days until the reunion below; **large (4×3 and bigger)** the orbit with the distance inside it, the partner's direction, both local times when the couple is in different time zones, a reunion card with the date and a "getting closer" progress line, and as space allows a line for new memories or a waiting Shooting Star. No size leaves large empty areas. | M |
| FR-WGT-2 | Widget updates when location, reunion, memory, Shooting Star or connectivity data arrives, within Android widget limits. | M |
| FR-WGT-3 | Widget never shows photos or captions; memories and Shooting Stars appear only as text ("2 new memories", "✨ Something is waiting for you"). | M |
| FR-WGT-4 | Tapping the distance or planets opens Our Universe, the reunion opens Until We Meet, and the memories line opens Ours. | M |
| FR-WGT-5 | Widget supports light and dark themes, using the app's colour tokens, and on Android 12+ the system widget corner radius. | S |
| FR-WGT-6 | Not paired, location sharing off, distance unavailable, no reunion date and offline each have a designed state (planets faded with a short explanation); old data is never shown as live (BR-8). | M |
| FR-WGT-7 | The widget picker shows a preview with sample data and the description "See how far apart you are and when you'll meet." | S |
| FR-WGT-8 | When "together since" is set, the widget adds it: **small** "845 days together" under the distance (or in its place when the distance is unavailable); **medium** "16 days until we meet · 845 days together"; **large** an Our Orbit row with the together duration (2y 3m 5d), times met, and the next anniversary when it is within 30 days, which opens Our Orbit. When it isn't set, this part is left out with no empty space and no prompt. | S |

### 3.12 Shooting Star — surprise messages (`FR-STAR`)

A Shooting Star is a full-screen surprise one partner prepares for the other: a
birthday wish, an anniversary note, a good-luck message or anything else. It
replaces the single birthday welcome of version 2.0; existing birthday welcomes
become Shooting Stars.

**Composing ("Send a Shooting Star", from You & Her and from Our Universe)**

| ID | Requirement | P |
|---|---|---|
| FR-STAR-1 | A paired user can send any number of Shooting Stars to their partner. Each is stored in the backend, not in app code. | M |
| FR-STAR-2 | The sender picks one of three layouts, each shown as a small preview: **Photo and message** (photo card, eyebrow, title, message, signature), **Message only** (no photo; larger, vertically centred text with the star and planet decoration) and **Full photo** (the photo fills the screen, with an optional short title and message on top). | M |
| FR-STAR-3 | Templates fill in the eyebrow and title, which stay editable: **Birthday** ("For you" / "Happy Birthday"), **Anniversary** ("For us" / "Happy Anniversary"), **Good luck** ("For you" / "Good luck"), **Thinking of you** ("Right now" / "Thinking of you") and **Blank** (clears both). Templates never change the message, signature or photo. | M |
| FR-STAR-4 | The composer shows only the fields the layout uses. Photo and message: eyebrow, title, message, signature, photo. Message only: eyebrow, title, message, signature. Full photo: photo, photo fit, title, message. Fields a layout doesn't use are not saved. | M |
| FR-STAR-5 | Every text field is optional. Limits: eyebrow 40, title 80, message 1,000 (multi-line), signature 50 characters. The signature starts as the sender's display name and can be edited or cleared. A star must show something: Full photo needs a photo; Message only needs a title or message; Photo and message needs a photo, title or message. | M |
| FR-STAR-6 | The photo is picked with the Android photo picker, then resized (long edge ≤ 1,920 px) and compressed like memories (FR-MEM-2). Full photo offers **Fill screen** (cropped to fill) or **Fit whole image**. | M |
| FR-STAR-7 | When to show: **the next time the partner opens Twoverse**, or a chosen **date and time** in the sender's time zone, which must be in the future. | M |
| FR-STAR-8 | Before sending, a live full-screen preview shows the star exactly as the partner will see it, switchable between light and dark. | M |
| FR-STAR-9 | The sender sees a list of their sent stars with their status (**Scheduled**, **Waiting** to be opened, **Seen**) and can edit or delete a star until the partner has seen it. | M |

**Viewing**

| ID | Requirement | P |
|---|---|---|
| FR-STAR-10 | Empty fields are hidden completely and the layout closes the gap. | M |
| FR-STAR-11 | Long messages scroll; "Enter Twoverse" stays pinned at the bottom. On Full photo, the photo fills the screen edge to edge behind the system bars, "Enter Twoverse" sits on a gradient scrim so it is always readable, the text overlay appears only if a title or message is set, and the photo can be pinched to zoom. | M |
| FR-STAR-12 | A star is shown once, on the first app open after it became visible (sent for the next open, or its time reached), and is then marked seen. Several waiting stars are shown one after another, oldest first. | M |
| FR-STAR-13 | Received stars can be viewed again from a "Shooting Stars" list in You & Her. | S |
| FR-STAR-14 | The composer, preview and viewer set `FLAG_SECURE`, like Ours (FR-VLT-6). | M |

**Backend**

| ID | Requirement | P |
|---|---|---|
| FR-STAR-15 | Only the couple can read a star. The recipient can read it only once it is visible, so a scheduled surprise stays hidden. Only the sender creates, edits or deletes it, and only while it is unseen. The recipient can only mark it seen. | M |
| FR-STAR-16 | Photos are stored in the private memories bucket under `{couple_id}/stars/` and shown only through signed URLs (NFR-SEC-2). Replaced or deleted photos are removed. | M |
| FR-STAR-17 | When a star becomes visible (sent for the next open, or its time reached), the partner receives a push: "Something special is waiting for you ✨", with no preview of the text or photo (FR-NOT-1). A scheduled job sends it for future times. At most one such push per recipient per minute. Rescheduling a star to a later time notifies again when it arrives. | M |

### 3.13 Our Orbit — relationship and meetups (`FR-ORB`)

Day counts use the couple's local date. The start date is day 1: on the day the
relationship began the couple has been together 1 day, and "day 100" is 99 days
after it.

| ID | Requirement | P |
|---|---|---|
| FR-ORB-1 | Either partner can set or change "together since", the date the relationship began. It can't be in the future, is never derived from the pairing date, and the backend records who changed it and when. | M |
| FR-ORB-2 | After pairing (unless the partner already set it), and from You & Her, the app asks "When did your story begin?" with a date picker. Asking after pairing can be skipped; Our Universe then shows a gentle prompt to set it later. | M |
| FR-ORB-3 | Either partner can add, edit and delete meetups: start date (today or earlier), optional end date (on or after the start, for visits lasting several days), optional place (up to 100 characters) and note (up to 300 characters). | M |
| FR-ORB-4 | Our Orbit, a bottom-bar tab also opened from the Together tile on Our Universe, shows: "In orbit since 14 February 2024", the duration in years, months and days ("2 years, 3 months, 5 days") and the total days, with the planets-and-orbit visual. | M |
| FR-ORB-5 | Our Orbit shows the next anniversary with a countdown. An anniversary on 29 February falls on 28 February in years without one. | M |
| FR-ORB-6 | Milestones are days 100, 365, 500 and 1000, then every 1000 days, plus every yearly anniversary. Our Orbit shows the next one ("1000 days in 23 days"). | M |
| FR-ORB-7 | Meetup statistics: times met (number of meetups); days together in person (the days covered by meetups so far, counting both the start and end day, with overlapping meetups counted once); and days since you last met ("12 days ago"). | M |
| FR-ORB-8 | A meetup timeline, newest first, with add, edit and delete. | M |
| FR-ORB-9 | Our Universe shows a Together tile beside Until we meet: "475 days", "Together" and "Met 7 times"; before the date is set it shows "Set your date". | M |
| FR-ORB-10 | When a reunion from Until We Meet has passed and isn't recorded as a meetup yet, Our Universe asks once on this device: "Did you meet on 10 October?". Yes opens a new meetup prefilled with the date and place, with an editable end date; No dismisses the question. A reunion is recorded as a meetup at most once per couple. | S |
| FR-ORB-11 | On an anniversary or milestone day, Our Orbit shows a small celebration. | C |
| FR-ORB-12 | Only the couple can read or change their "together since" date and meetups (NFR-SEC-1); meetups update live for both partners. | M |

### 3.14 Settings — You & Her (`FR-SET`)

| ID | Requirement | P |
|---|---|---|
| FR-SET-1 | Show couple status and "connected since" date. | M |
| FR-SET-2 | Toggles: Share my location, Lock Ours. | M |
| FR-SET-3 | Location precision, distance unit, appearance (System / Light / Dark). | M |
| FR-SET-4 | Log out. | M |
| FR-SET-5 | Disconnect from partner (FR-PAIR-7). | M |
| FR-SET-6 | Request account deletion, which removes the user's account and data. | M |
| FR-SET-7 | Show and change "together since" (FR-ORB-2). | M |

---

## 4. Non-functional requirements

### 4.1 Privacy (`NFR-PRV`)
- NFR-PRV-1 No location history is stored; only the latest location per user.
- NFR-PRV-2 Location sharing always shows its status and can be turned off in one tap.
- NFR-PRV-3 No analytics or advertising SDKs in the MVP.
- NFR-PRV-4 The app clearly states that screenshots are blocked in Ours but photos of the screen cannot be prevented.
- NFR-PRV-5 Location consent is explicit, even when the app is installed as a gift.

### 4.2 Security (`NFR-SEC`)
- NFR-SEC-1 Every table uses Supabase Row Level Security so users can only access their own couple's data.
- NFR-SEC-2 Photos are stored in a private bucket and served only through short-lived signed URLs.
- NFR-SEC-3 Pairing, expiry and deletion rules are enforced in the backend, not only in the app.
- NFR-SEC-4 No secrets in the app or Git; the service-role key exists only in edge-function secrets.
- NFR-SEC-5 All traffic uses HTTPS.

### 4.3 Performance (`NFR-PRF`)
- NFR-PRF-1 Cold start to a usable home screen in under 2 seconds on a recent Samsung device, using cached data.
- NFR-PRF-2 Compass needle updates smoothly (target 30+ fps).
- NFR-PRF-3 Each photo is downloaded once and then served from cache.

### 4.4 Battery (`NFR-BAT`)
- NFR-BAT-1 No continuous background GPS or permanent foreground service in the MVP.
- NFR-BAT-2 Background location work respects Android and Samsung power limits.

### 4.5 Reliability (`NFR-REL`)
- NFR-REL-1 Offline: show cached data with an offline banner; never present old data as live.
- NFR-REL-2 Failed uploads can be retried without data loss.

### 4.6 Usability and accessibility (`NFR-UX`)
- NFR-UX-1 Follows `docs/design/DESIGN.md` in light and dark themes.
- NFR-UX-2 Touch targets ≥ 48dp; text meets WCAG AA contrast; supports font scaling.
- NFR-UX-3 Icon-only controls have content descriptions.

### 4.7 Maintainability (`NFR-MNT`)
- NFR-MNT-1 Follows the architecture in `CLAUDE.md` (MVVM, UDF, Hilt, feature packages).
- NFR-MNT-2 Data access goes through repository interfaces so fake and Supabase implementations are interchangeable.

---

## 5. Data model (logical)

| Entity | Key fields |
|---|---|
| Profile | id, display_name, distance_unit, appearance, created_at |
| Couple | id, user_a, user_b, status (pending / active / ended), connected_at, together_since (date, nullable), together_since_set_by, together_since_set_at |
| CoupleCode | code, couple_id, created_by, expires_at, used_at |
| Location | user_id (unique), lat, lng, accuracy_m, precision, sharing_enabled, updated_at |
| Reunion | couple_id (unique), meet_at (UTC), place, note, updated_by, updated_at |
| Memory | id, couple_id, sender_id, storage_path, caption, expires_at (nullable), allow_keep, viewed_at, created_at |
| ShootingStar | id, couple_id, sender_id, recipient_id, layout (photo_message / message_only / full_photo), eyebrow, title, message, signature, photo_path, photo_fit (fill / fit), show_at (UTC, nullable = next open), seen_at, created_at |
| Meetup | id, couple_id, start_date, end_date (nullable, ≥ start_date), place, note (≤ 300), created_by, from_reunion_at (the reunion it records, nullable, once per couple), created_at |
| DeviceToken | user_id, fcm_token, updated_at |

---

## 6. Business rules

| ID | Rule |
|---|---|
| BR-1 | A couple has exactly two users; a user has at most one active couple. |
| BR-2 | Users can never read or change another couple's data. |
| BR-3 | Location sharing is voluntary and can be stopped at any time. |
| BR-4 | Only the latest location per user is kept. |
| BR-5 | Received photos never appear in the device gallery automatically. |
| BR-6 | Expired memories are inaccessible, even before their files are deleted. |
| BR-7 | Deleted memories are removed for both partners. |
| BR-8 | Old location data is never shown as live (see FR-LOC-10). |
| BR-9 | Disconnecting ends location sharing immediately. Shared memories, reunion, Shooting Stars, meetups and the "together since" date are deleted for both after confirmation. |
| BR-10 | Couple codes expire after 24 hours and work only once. |

---

## 7. Error and empty states

| Situation | Message |
|---|---|
| Offline | "You're offline. Showing the latest saved information." |
| My location sharing off | "Location sharing is off." |
| Partner location unavailable | "Your partner's location isn't available right now." |
| Invalid or expired code | "This couple code is invalid or has expired." |
| Memory expired | "This memory has expired." |
| Upload failed | "Couldn't send your memory. Try again." |
| No memories | "Nothing here yet. Send your first memory." |
| No reunion date | "When will you see each other next?" |
| Compass needs calibration | "Move your phone in a figure-8 to calibrate." |
| Shooting Star unavailable (deleted, already seen, or not yet visible) | "This Shooting Star is no longer available." |
| "Together since" not set | "When did your story begin?" |
| No meetups | "No meetups yet. Add the times you've met." |
| Date in the future | "Choose a date that isn't in the future." |

---

## 8. Screens

| Screen | Requirements |
|---|---|
| Splash | FR-ONB-1 |
| Welcome | FR-ONB-2 |
| Sign in | FR-AUTH |
| Connect your worlds | FR-PAIR |
| Our Universe (home) | FR-LOC, FR-CNT-4, FR-VLT (entry), FR-ORB-2, FR-ORB-9, FR-ORB-10 |
| Your Star | FR-CMP |
| Until We Meet | FR-CNT |
| Ours | FR-VLT |
| Memory viewer | FR-VLT-8, FR-DEL |
| New memory | FR-MEM |
| You & Her (settings) | FR-SET |
| Shooting Star (viewer) | FR-STAR-10 to FR-STAR-14 |
| Send a Shooting Star (composer and preview) | FR-STAR-1 to FR-STAR-8, FR-STAR-14 |
| Shooting Stars (sent and received list) | FR-STAR-9, FR-STAR-13 |
| Our Orbit | FR-ORB-4 to FR-ORB-8, FR-ORB-11 |
| When did your story begin? | FR-ORB-1, FR-ORB-2 |
| Add / edit meetup | FR-ORB-3, FR-ORB-10 |
| Widget | FR-WGT |

---

## 9. MVP release plan

1. **UI with fake data:** all screens, navigation, light/dark themes
2. **Backend:** Supabase schema, RLS, auth, pairing
3. **Location:** sharing, distance, freshness, wake-up ping
4. **Compass and countdown** with real data
5. **Ours:** upload, view, expiry, deletion, notifications
6. **Widget and birthday welcome**
7. **Testing on both phones**, then install for the birthday

### Success criteria
Both partners can install, sign in, pair, share location, see an accurate
distance with correct freshness, follow the compass, see the same countdown,
exchange private photos that stay out of the gallery and expire correctly, use
the widget, see the birthday welcome, and disconnect with their data removed.

---

## 10. Out of scope for MVP
Public profiles, social features, group couples, chat, voice/video calls,
location history or maps, ads, payments, iOS.

## 11. Future ideas
Notes on the partner's widget, "thinking of you" tap, time-locked memories and
"open when…" letters, shared timeline and journal, voice memories, anniversary
reminders, end-to-end encrypted memories, iOS app.

## 12. Open questions
- Should disconnect keep a short grace period (e.g. 7 days) before deleting shared memories?Yes, to allow accidental disconnects to be reversed.
- Should the recipient be allowed to delete memories the partner sent, or only hide them?Only hide them; the sender controls the memory's lifetime.
- Approximate location as the default: confirm after testing compass accuracy on real devices.su

# Twoverse Design Spec

Source of truth for the UI. The screen files in `screens/` are HTML design
mockups (390 × 844). Use them as a **visual and measurement reference only**.
Do not copy HTML into the app; rebuild each screen in Jetpack Compose.

Unit conversion: **1 px in the mockups = 1 dp** (and font px = sp).
The mockups include 56 px top padding for the status bar; in the app use
`WindowInsets.safeDrawing` / `statusBarsPadding()` instead of hardcoding it.

Sample values in mockups (94.6 km, Kandy, 12 days, 17 memories, AB72-KP91,
dates, names) are placeholder data. Feed them from fake repositories.

---

## 1. Brand concept

Twoverse = "a private universe for two". Visual language is **planets**:

| Element | Meaning | Colour |
|---|---|---|
| Rose planet | Her (the partner) | rose gradient |
| Lavender planet | You (the user) | lavender gradient |
| Gold star | What you share (centre of the orbit) | gold gradient |

No 3D illustrations. Depth comes from radial gradients and soft shadows,
drawn in Compose (`Brush.radialGradient`, `Modifier.shadow`, `Canvas`).

## 2. Colour tokens

| Token | Light | Dark | Use |
|---|---|---|---|
| background | #F6ECF1 | #120E24 | Screen background |
| surface | #FFFFFF | #1C1733 | Cards, nav bar, inputs |
| surfaceVariant | #FBF4F7 | #231D3E | Tip cards |
| onSurface (ink) | #2B1633 | #F6ECF1 | Primary text |
| onSurfaceVariant (muted) | #6B5572 | #B8AECF | Secondary text |
| outline (line) | #EAD9E2 | #2E2650 | Borders, dividers, rings |
| primary | #C4466A | #F07C98 | Buttons, active nav, links |
| onPrimary | #FFFFFF | #2A0F1C | Text on primary |
| accent (her) | #E0607E | #F07C98 | Compass needle, new-dot |
| lavender (you) | #9D86D0 | #A48FDA | Your planet |
| gold | #E8B04A | #F2C46D | Star, live dot |
| goldText | #8A5A0B | #F2C46D | Gold text (contrast-safe) |
| chip | #F3E1EA | #2A2247 | Chips, icon tiles, secondary buttons |
| error (danger) | #B3261E | #FF9494 | Delete, disconnect |

Planet gradients (radial, centre at 32% x / 30% y):
- Rose: #FFE0E8 → #E0607E (55%) → #9E3552
- Lavender: #F1E9FF → #9D86D0 (55%) → #5E4A94
- Gold star (centre 35%/35%): #FFF3D1 → #E8B04A (60%) → #C98A22, with a gold glow

Vault tile tints — light: #F3E1EA, #ECE5F7, #FBEFD9, #FFFFFF; dark: #2A2247, #2B2350, #33263A, #1C1733.

Card depth: **light** = soft shadow (y 8dp, blur 24dp, plum #2B1633 at 7%);
**dark** = no shadow, 1dp `outline` border instead.

Theme follows the system setting by default (Settings › Appearance: System / Light / Dark).
Do not use Material dynamic colour; the brand palette is fixed.

## 3. Typography

Fonts (Google Fonts, SIL Open Font License, free for commercial use):
- **Fraunces** (serif) 500/600 — display and headings
- **Manrope** (sans) 400/500/600/700 — everything else

| Style | Font | Size / weight | Used for |
|---|---|---|---|
| displayLarge | Fraunces | 104sp / 600 | Countdown days |
| displayMedium | Fraunces | 60sp / 600 | Home distance number |
| displaySmall | Fraunces | 46sp / 600 | Splash wordmark, Birthday title |
| headlineLarge | Fraunces | 34–36sp / 600 | Onboarding and auth titles |
| headlineMedium | Fraunces | 30sp / 600 | Tab screen titles |
| headlineSmall | Fraunces | 24sp / 500–600 | Sub-screen titles, captions |
| titleMedium | Manrope | 16sp / 700 | Card titles, buttons |
| bodyLarge | Manrope | 16sp / 400–500 | Body text |
| bodyMedium | Manrope | 15sp / 500 | Secondary body |
| labelMedium | Manrope | 13sp / 600 | Field labels, small labels |
| labelSmall | Manrope | 12sp / 600–700 | Chips, nav labels, section headers (letter-spacing 1.2) |

## 4. Shapes and spacing

- Primary / secondary buttons: height 56dp, fully rounded (28dp)
- Small buttons: height 52dp, radius 26dp
- Cards: radius 24dp (distance card 28dp)
- Inputs: height 54dp, radius 16dp, 1dp outline border
- Chips: height 36dp, radius 18dp
- Icon tiles: 48dp, radius 16dp
- Vault tiles: 112dp tall, radius 16dp, 3-column grid, 8dp gap
- Screen horizontal padding: 20–28dp (tab screens 20dp)
- Spacing scale: 4, 8, 12, 14, 16, 20, 24, 32
- Minimum touch target: 48dp

## 5. Icons

Line icons, 1.8dp stroke, rounded caps/joins, 24dp.
Use Material Symbols Rounded (outlined) equivalents, or vector drawables:
orbit (Universe tab), explore (compass), lock (Ours), person (You & Her),
calendar, pin, clock, image, heart, trash, send, copy, close, back chevron, eye, info.

## 6. Reusable components (build these first)

- `TwoversePrimaryButton`, `TwoverseSecondaryButton` (chip bg), `TwoverseOutlineButton`, `TwoverseTextButton`
- `TwoverseCard` (handles shadow in light, border in dark)
- `TwoverseTextField` (label above, 54dp, optional trailing icon)
- `TwoverseSwitch` (52 × 32, primary track, white thumb)
- `TwoverseChip` (selectable, primary when selected)
- `SegmentedOptions` (4 equal options, used for memory expiry)
- `Planet(size, kind = Her | You)` and `GoldStar(size, glow)`
- `OrbitGraphic` (rings + planets + star; animated variant for Splash)
- `StarField` (twinkling background dots)
- `TwoverseBottomBar` (4 tabs: Universe, Your Star, Ours, You & Her)
- `SettingsRow` (title, optional subtitle, trailing switch or value + chevron)

## 7. Navigation

```
Splash → Welcome → SignIn → Pair → Home (tabs)
Welcome → "I have a couple code" → Pair
Birthday (first launch for the partner only) → Home

Tabs (bottom bar): Home (Universe) · Compass (Your Star) · Vault (Ours) · Settings (You & Her)
Home → Compass, Countdown, Vault, AddMemory
Vault → Memory, AddMemory
Memory → back to Vault
AddMemory → close/send returns to Vault
Countdown → back to Home
```

## 8. Screens

| File | Screen | Notes |
|---|---|---|
| Splash.dc.html / SplashDark.dc.html | Splash | Two planets orbit the gold star (28 s per turn, planets counter-rotate), twinkling stars, 3 pulsing loading dots, "Connecting your worlds…" |
| Welcome.dc.html | Welcome | Static orbit with 3 feature chips (lock, compass, calendar) |
| SignIn.dc.html | Sign in | Google button, email, password with show/hide, forgot link |
| Pair.dc.html | Connect your worlds | Couple code card with Share/Copy, waiting state, partner code input, Connect |
| Home.dc.html | Our Universe | Distance card (live dot, number, dashed arc between planets, cities, updated time), Your Star + countdown tiles, Ours row, Send a memory button |
| Compass.dc.html | Your Star | Dial with 72 ticks, N/E/S/W, needle rotated to partner bearing (42° in mockup), distance + direction, status chips, tip card |
| Countdown.dc.html | Until We Meet | Days, H/M/S tiles, "Getting closer" track, plan rows (date, place, time), Change date |
| Vault.dc.html | Ours | Filter chips, 3-col grid, expiry badges, new dots, FAB add |
| Memory.dc.html | Memory viewer | Photo, back/more, expiry chip, sender, caption, Keep forever / Delete, screenshot note (use FLAG_SECURE) |
| AddMemory.dc.html | New memory | Photo picker, caption, expiry segmented (Never/24 h/7 days/30 days), keep-forever switch, Send to her |
| Settings.dc.html | You & Her | Couple card, Privacy rows, Preferences rows, Log out, Disconnect |
| Birthday.dc.html | Birthday welcome | Big glowing star, planets, stars, "Happy Birthday", personal message, Enter Twoverse |

Dark mode: every screen uses the same layout with the dark tokens.

## 9. Accessibility

- Text contrast meets WCAG AA in both themes with the tokens above; do not lighten muted text.
- Icon-only buttons need `contentDescription`.
- Switches use `Modifier.toggleable` / Material semantics with a label.
- Support font scaling; avoid fixed heights on text containers.

# Nothing-style UI Mockup — Analysis

Source: `Melodia Nothing-style UI Mockup.html` (single-file bundled artifact, 9 nested screens).
Extracted standalone HTML in `docs/mockup/html/`, rendered screenshots in `docs/mockup/screens/`
(contact sheet: `contact_sheet.png`). Fonts are embedded and OFL-licensed (Doto, Space Mono).

## What it is

A **redesign proposal** for Melodia: 9 phone-frame screens (390×844) in a dot-matrix
Nothing-style language, organized as a **3-tab app + 5-step creation wizard**.

## Design system (extracted tokens)

| Token | Value |
|---|---|
| Screen background | `#000` + dot-matrix texture (`radial-gradient` white 10% dots, 20px grid) |
| Card surface | `#0D0D0D`, border `#2A2A2A`, radius 22px |
| Text | white body (15px system), grey `#9A9A9A` labels |
| Red accents | primary `#D71921`, bright `#FF4B52`, hot gradient `#D71921→#FF7DB9` |
| Display font | **Doto 900** (dot-matrix) — wordmark 22px, H1 34px, cover-art glyph 64px |
| Label font | **Space Mono 700** 11px, letter-spacing .08em, ALL-CAPS |
| Buttons | pill radius 999px, min-height 48px, active scale .96; primary = red fill |
| Chips | 999px pills, active = white fill/black text |
| Progress | 8px bars, animated red diagonal stripes; indeterminate variant; skeleton shimmer |
| Nav | 3 tabs, dot-matrix SVG icons, active = white + 2px red inset top line |
| Motion | staggered entry, pulsing dots, equalizer bars, blinking cursor; `prefers-reduced-motion` respected |

## Screen-by-screen

1. **Studio** — `STEP 1 / 5 · STUDIO`, segment progress, H1 "Write the lyrics", prompt card
   with chips (Pop / Melancholic / Arabic + English), GENERATING state (striped indeterminate
   bar), live lyrics card with `[VERSE 1]`, **mixed English/Arabic lines with `dir="auto"`**,
   skeleton lines.
2. **Lyrics** — `STEP 2 / 5`, section tabs (VERSE 1 / CHORUS / BRIDGE), editor, rhyme panel
   (`RHYMES FOR "PAST"`: last/fast/glass), AUTO-SAVED badge, `NEXT: VISUALS`.
3. **Visuals** — `STEP 3 / 5`, cover-art styles (NEO / MONO DOTS / RED PULSE / GRID),
   "Rendering cover 48%", GENERATE COVER.
4. **Sync** — `STEP 4 / 5`, file chip (`night_drive.mp3 · 3:12`), 4-step checklist
   (Decode audio → Find vocal regions → Estimate word timing → Review timing) with pulsing
   current step, LIVE PREVIEW (ESTIMATED) word chips, CANCEL.
5. **Export** — `STEP 5 / 5`, frame-accurate progress (`Frame 1,260 / 3,600`, `42 s of 120 s · 1080p`),
   checklist (Render frames → Add audio → Save to gallery), "Keeps running if you leave the app.",
   CANCEL / SHARE.
6. **Library** — search, song rows with status (`NEO · 3:12 · Synced` / `Needs sync` /
   `Lyrics only`).
7. **Settings** — **Language EN / عربي toggle**, cloud transcription, haptics, video quality
   (1080P/4K), storage used 38%.
8. **App icon** — "M in 7×7 dots + the red signal dot", round/square/themed variants.
9. **Dot-matrix icon set** — 13 dot-matrix glyphs (studio, lyrics, visuals, sync, export,
   library, settings, play, pause, check, close, add).

## Comparison with the current app

**Already aligned (current baseline shares the language):** black surfaces, red accent,
ALL-CAPS technical labels, dot/led motifs, Nothing-style components. The mockup does not
contradict the visual identity — it refines it.

**Structural deltas (this is a redesign, not a reskin):**
- Navigation: current 5-tab bar (Studio/Lyrics/Library/Sync/More) → mockup 3-tab
  (PROJECT/LIBRARY/SETTINGS) with the creation flow as a **5-step wizard** (segments
  `STEP n / 5`). Visuals/Export become wizard steps instead of More-menu destinations.
- New UI states not present today: striped animated progress, skeleton shimmer, step
  checklists with pulsing current item, rhyme panel, live word-chip preview, frame-accurate
  export progress, language toggle in Settings.
- Typography: Doto as display font (current app uses its own technical typography);
  Space Mono for labels.

**Notable alignments with recent work:** the mockup assumes bilingual lyrics
(`dir="auto"` Arabic lines) and an EN/عربي settings toggle — exactly what the i18n
slices 1–8 and `values-ar` locale now support. Status-pill vocabulary (READY,
TRANSCRIBING, PROCESSING) matches the badge strings inventoried in the audit.

## Adoption options (not started — needs direction)

- **A. Full adoption (v4):** restructure nav to 3 tabs + wizard, integrate Doto/Space Mono,
  rebuild components. Large effort; replaces the current active interface.
- **B. Component-level adoption:** keep current 5-tab structure; port individual pieces
  (striped progress, step checklists, rhyme panel, frame-accurate export status, dot-matrix
  icons, Doto for wordmark/cover glyph). Medium effort; preserves baseline.
- **C. Reference only:** keep as design north-star, no code changes.

Licensing: Doto and Space Mono are SIL OFL 1.1 — bundling in the app is permitted
(license text ships with the mockup file).

# Nothing-style UI: spec for the implementation agent

Source of truth for the visual design. Reference code lives in `app/src/main/java/com/example/ui/nothing/` (prefix `Nt`). It is stateless and additive: nothing existing was changed except the launcher icon resources. Wire it to the ViewModel one screen at a time (see the plan, tasks U6 to U8).

## Tokens (`Nt`)
Black `#000000`, surface `#0D0D0D`, border `#2A2A2A`, track `#262626`, grey `#9A9A9A`, white, red `#D71921` (fills), red text `#FF4B52` (small text), pink `#FF7DB9`. Card radius 22dp, chips and buttons pill, screen padding 20dp. Titles: existing dot-matrix font 34sp. Labels: Space Mono Bold 11sp, caps, letter spacing 0.08em.

## Shell
`NtAppShell(selectedDestination, onSelectDestination, topLabel, stepIndex?, bottomExtra?) { screen content }`
Top bar: wordmark (dot-matrix 22sp) + pulsing red LED + label. Project steps show `NtStepIndicator` (5 segments) and label `ntStepLabel(step)`. Bottom: `NtBottomNav` with 3 destinations (Project, Library, Settings) and animated dot icons.

## Screens (each is a `ColumnScope` extension, entrance order set with `staggeredEnter(n)`)
| Screen | Composable | Waiting or motion elements |
|---|---|---|
| Studio | `NtStudioContent` | indeterminate `NtProgressBar`, `NtSkeleton` lines, `NtCaret`, LED in the button while generating |
| Lyrics | `NtLyricsContent` | caret, rhyme skeleton while loading, LED + AUTO-SAVED |
| Visuals | `NtVisualsContent` + `NtCoverArt` | drifting dot grid, pulsing glow, determinate bar |
| Sync | `NtSyncContent` | `NtEqualizer`, determinate bar, `NtStepList` (current step pulses), `NtWordChip` |
| Export | `NtExportContent` | big dot-matrix percentage, determinate bar, step list, share disabled until done |
| Library | `NtLibraryContent` + `NtMiniPlayer` | equalizer on the playing row |
| Settings | `NtSettingsContent` | `NtSwitch` knob slide, storage bar |

## Waiting animations
Determinate: striped red bar (`NtProgressBar(progress)`). Unknown length: `NtProgressBar(null)`. Text loading: `NtSkeleton`. Writing: `NtCaret`. Listening or analysing: `NtEqualizer`. Current step: pulsing ring in `NtStepList`. All loops stop when animator scale is 0 (`rememberReducedMotion`).

## Icons
`DotIcon` (12 glyphs, 7x7) drawn by `DotMatrixIcon`; `animated = true` runs a diagonal pulse wave.

## App icon
Adaptive: black background, white dot-matrix M (cell 6, origin 33,33, radius 2.2) and a red dot at (73,35), all inside the 66dp safe zone. A monochrome layer is provided for themed icons.

## Not covered yet
Splash, waiting-animation catalog, manual timing editor, voice dictation, genre picker, drafts, lyrics export sheet, empty/error/offline states.

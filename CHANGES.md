# Changes

## Iteration 2: UI refresh
- **Design system** (`MelodiaDesignSystem.kt`): spacing and radius tokens, a new `MelodiaActionButton`
  (Primary / Secondary / Quiet, 48 dp minimum height, whole-button press feedback, rounded ripple), and a
  `Modifier.dotGrid()` Nothing-style dot backdrop that fades out down the screen.
- **Bug fix:** `MelodiaPrimaryButton` text was pinned to the top-left corner of its box; now centred.
- **Cards:** larger radius and a top-lit gradient border (used by `MelodiaCard`).
- **Bottom bar:** 7 tabs at an 8 sp label were hard to read. Now icons are 24 dp and only the selected tab
  shows its name (10 sp) with a red marker; layout never shifts. Tab names are sentence case
  ("Settings" instead of "CONFIG"), which also matches the existing `MainActivityTest` selectors.
- **Studio header:** large dot-matrix "Melodia" title, a mode chip, a quiet save line (dot pulses only while
  saving), and three real 48 dp actions instead of ~26 dp pills crammed into one box. Copy is plain sentence case.
- Tab screens no longer paint their own opaque black, so the dot grid shows through.
- Not compiled here (no Android SDK): build and look at it on a device before shipping.

---

# Iteration 1

## 1. Lyric sync: audio decoding fixed (`LocalWhisperEngine.kt`)
- **Bug:** the decoder treated raw decoder output as 16 kHz mono. For a normal 44.1 kHz stereo file the
  computed duration was ~5.5x too long, so every word timing was wrong.
- Now reads the decoder's real output format (sample rate, channels, 16-bit or float PCM), down-mixes to
  mono and resamples to 16 kHz chunk by chunk (new `downmixToMono`, `StreamingResampler`).
- Uses a primitive float buffer instead of `MutableList<Float>` (big memory/GC saving on long songs).
- Decoder is always released (`finally`), supports coroutine cancellation, and cannot hang if the codec
  never signals end-of-stream.

## 2. Lyric sync: smarter alignment
- Finds where the audio is actually active (adaptive RMS threshold), so leading/trailing silence and long
  quiet gaps are skipped, and no word stays highlighted across an instrumental gap.
- Adds pauses after line ends and before `[Section]` tags.
- NOTE: still a heuristic, not speech recognition. Real Whisper needs the NDK engine
  (`externalNativeBuild` is commented out in `app/build.gradle.kts`, and the C++ files are stubs).

## 3. Security
- Gemini API key no longer travels in the URL (it was visible in logs at `Level.BASIC`). An interceptor
  moves it into the `x-goog-api-key` header; call sites are unchanged. HTTP logging is off in release builds.
- Backup rules now exclude `secure_prefs` (EncryptedSharedPreferences keys are not restored on a new
  device, so restoring that file would crash the app).

## 4. Tests
- New `AudioAlignmentTest.kt` (JVM-only): downmix, resampling (incl. chunked == one-shot), activity
  detection, word alignment, lyric parsing, Arabic lyrics.

## Not changed, worth deciding on
- `AppDatabase` uses `fallbackToDestructiveMigration(true)`: any schema bump deletes users' lyrics/drafts.
  Add real Room migrations (and `exportSchema = true`) before shipping.
- `usesCleartextTraffic="true"` in the manifest. Kept because `FreeLlmProvider` explicitly accepts `http://`
  base URLs; set it to false (or use a network-security-config) if you only use HTTPS.
- API keys are baked into the APK through `BuildConfig`; anyone can extract them. For a public release,
  proxy the calls through your own backend.

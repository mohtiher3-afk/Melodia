# Changes

## Iteration 7: lyric video audio/video sync
- **Fix:** MP4 export drew frames with `Surface.lockCanvas()`, which stamps each frame with the wall-clock time it was drawn. The video therefore lasted as long as rendering took, not as long as the song, and lyrics drifted from the audio. Frames are now drawn on a Bitmap canvas and presented through EGL (`EncoderInputSurface.kt`) with an explicit timestamp from `framePresentationTimeNs(frameIndex, fps)` (unit-tested in `DomainLogicTest`).
- **Fix:** the encoder drain loop gave up after one 10 ms timeout and could drop the last frames; it now waits for the real end-of-stream flag (up to ~5 s without output).
- **Fix:** Room no longer wipes the library on any schema change; destructive fallback is limited to versions 1-6. Future schema bumps need an explicit `Migration`.
- Default player artist is now "Melodia Studio".
- Not compiled or run here (no Android SDK). Test an export on a device and check the last lyric line against the audio.

## Iteration 6: local models (offline-first)
- Replaced cloud LLM dependency with multiple free on-device models downloadable directly from the app via a single button press per model.
- Added **Gemma 3 1B** text generation via LiteRT-LM (`com.google.ai.edge.litertlm:litertlm:1.1.0`) — fully offline, no API key, ~500 MB download.
- Added **Whisper.cpp** speech-to-text foundation via existing `LocalWhisperEngine.kt` — local transcription ready for integration.
- Added **Stable Diffusion v1-5** image generation via MediaPipe Image Generator (`com.google.mediapipe:mediapipe-image-generator:0.10.14`) — ~2 GB download, size warning in UI.
- Created `LocalModelManager` — centralized model download, storage (app-private files), status tracking (Idle/Downloading/Completed/Failed), and per-model deletion.
- Added "LOCAL MODEL DOWNLOADS" section to Settings with one-tap download/remove buttons, progress indicator, and retry on failure.
- Extended `LlmProviderFactory` with `PROVIDER_GEMMA3_LOCAL` and new `localProviders()` list for offline-first provider selection.
- Implemented `Gemma3LocalLlmProvider` using LiteRT-LM for on-device text generation; falls back to cloud providers if model not downloaded.
- Added `downloadModel(modelId)` and `deleteModel(modelId)` to `MelodiaViewModel` using `viewModelScope` and `getApplication()`.
- All models are free, open-license, and require zero cloud credentials.

## Iteration 5: visual polish
- Added a low-intensity red ambient glow behind the existing dot-matrix backdrop.
- Refined shared cards with subtle dark gradients, soft elevation, and a consistent red title marker.
- Kept the Nothing-inspired monochrome palette and preserved existing navigation and actions.

---

## Iteration 4: local AI foundation
- Added Gemini Nano through ML Kit Prompt API as the default provider for new installs; the app checks AICore/device availability at use and downloads the managed model when offered.
- Routed lyric drafting, prompt polishing, rhyme suggestions, next-line generation, and sentiment analysis through the on-device provider or its existing local fallback instead of silently using the Gemini cloud API.
- Disabled Gemini cloud image generation in the cover-art path; covers now use the existing offline procedural renderer until a compatible on-device diffusion model is integrated.
- Added an explicit, off-by-default setting for Gemini cloud audio transcription. Local audio import no longer uploads automatically; the domain method also requires an explicit upload-consent argument.
- Local Whisper and on-device image diffusion are not integrated yet. Timing remains an energy-aware heuristic, not true forced alignment.
- Repaired the Unix Gradle wrapper (it previously dropped all Java arguments); Gradle 9.3.1 now launches. Android compilation/tests remain unverified because this environment has no Android SDK/NDK.

---

## Iteration 3: lyric workflow and video effects
- Added a lyric-generation action inside the lyrics editor, with a replacement confirmation when text already exists.
- Audio import can use Gemini vocal transcription only when the user explicitly enables cloud transcription; Sync also has a manual retry/extract action and confirms before replacing existing lyrics. Gemini inline transcription supports files up to 14 MB.
- Added energy-aware automatic karaoke timing using decoded PCM activity, active audio regions, lyric lengths, and section/line pauses. This remains a heuristic, not speech recognition or true Whisper forced alignment; users should review it.
- Added a pulsing red karaoke word, animated red/pink/white word gradient, and word-progress-reactive waveform in MP4 exports. Exported MP4s now include imported audio without re-encoding.
- Integrated the supplied audio-alignment tests, UI refresh, Gemini key handling, and Android backup-rule updates.
- Android build/tests were not run because the Android SDK is unavailable in this environment.

---

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

# Melodia — Phase 1 Feature Audit (evidence-based)

Date: 2026-11-12 · Baseline: `git` working tree at `4af5995` + uncommitted local edits (reviewed, not reverted).

## Baseline verification (run before any change)

| Command | Result |
|---|---|
| `:app:assembleDebug` | ✅ BUILD SUCCESSFUL (54 s) — NDK 26.1 + CMake 3.22.1 present, native `melodia_whisper` compiles |
| `:app:testDebugUnitTest --rerun-tasks` | ✅ BUILD SUCCESSFUL (2 m 17 s) — **30 tests, 0 failures** (8 DomainLogic, 18 AudioAlignment, 3 Robolectric, 1 Example) |
| `:app:connectedDebugAndroidTest` (Pixel 9 AVD, API 17 image, emulator-5554) | ⚠️ **20 tests, 1 failure** — app installs and launches; `floatingPlayerBarContainerRenders` fails (pre-existing test bug, see F-13) |
| First run of instrumented tests | ❌ `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — stale differently-signed build on emulator; resolved by uninstalling the dev package (test env only) |

Environment: Android SDK 36 present, JDK 21 (Temurin), emulator available. No blockers.

## How each finding was verified

Static trace of every screen → ViewModel → domain/network call, cross-checked with the emulator
run (app launches, 19/20 instrumented assertions pass) and the unit-test suite. No feature was
marked "working" from code inspection alone where a runtime check was possible.

## Findings table

| # | Feature (screen → path) | Status | Evidence / reason | Repro | Priority |
|---|---|---|---|---|---|
| F-1 | **Lyric generation** (Studio GENERATE / Lyrics GENERATE → `MelodiaViewModel.generateLyricsOnly` → `MusicEngine.generateSongDraft` → `LlmProviderFactory`) | **broken (default config)** | Default provider is `gemini_nano`; `GeminiNanoLlmProvider.isAvailable()` is hardcoded `false` and `generate()` throws "not yet integrated". The ML Kit GenAI dependency (`com.google.mlkit:genai-prompt:1.0.0-beta4`) is in `app/build.gradle.kts` but **never imported anywhere** (verified by grep). Settings labels it "DEVICE CHECK" (configured=true), contradicting the runtime failure. Cloud Gemini works only with a real key; Gemma 3 1B local works but is **not offered in Settings**. | Settings → Nano Local selected (default) → Studio → CREATE LYRICS → always "not configured / Choose another provider" | **P0** |
| F-2 | **Gemma 3 1B local provider chain** (Settings → provider → `Gemma3LocalLlmProvider` → LiteRT-LM) | **partial → broken by wiring** | Provider code is real (LiteRT-LM `Engine`, model download from HF). But (a) Settings never lists `PROVIDER_GEMMA3_LOCAL`; (b) if it were selected, `MusicEngine.generateSongDraft/autoPolishPrompt/…` call `LlmProviderFactory.create(providerId)` **without a Context**, which throws `IllegalStateException("Context required…")` for this provider. | Not reachable from UI; code-trace confirms crash path | **P0** |
| F-3 | **Local model downloads UI** (Settings → LOCAL MODEL DOWNLOADS → `LocalModelDownloadSection` → `viewModel.downloadModel`) | **broken (progress invisible)** | `SettingsScreen` creates its **own** `LocalModelManager(context)` to observe state, while `MelodiaViewModel.downloadModel()` creates a **different** instance. Download runs, UI stays `Idle` forever. Also `cancelDownload()` does not cancel the coroutine/HTTP stream — it posts `Paused` while bytes keep writing; "Resume" restarts from 0 and overwrites the partial file. | Settings → DOWNLOAD on Gemma 3 1B → progress never appears; PAUSE does not stop the transfer | **P0** |
| F-4 | **Local Whisper transcription** (Settings model list + `WhisperNativeBridge` → `app/src/main/cpp/melodia_whisper_bridge.cpp`) | **broken (fake success)** | `nativeInit` really loads whisper.cpp/ggml (compiles into `libmelodia_whisper.so`), but `nativeTranscribe` returns the hardcoded string `"Transcription API ready"` — no audio is processed. `WhisperNativeBridge` has **0 references** outside its own file; downloaded `ggml-tiny/base.bin` models are never used. `LocalWhisperEngine.alignLyricsWithAudio` is the energy-heuristic only (honestly labelled in Sync UI). | Sync screen shows "local Whisper is not integrated yet" while Settings implies the model enables it | **P0** |
| F-5 | **Image/artwork generation** (Visuals → `generateCoverArtOnly` → `CoverArtGenerator`) | **partial (procedural only, honestly labelled)** | Real, working offline procedural PNG generator (1024×1024, saved under `filesDir/covers`, preview + gallery save work). **No** diffusion integration: `stable_diffusion_v15` download entry fetches a raw `.ckpt` that **nothing consumes** (MediaPipe Image Generator is in deps but never imported) — a fake capability. | Visuals → GENERATE COVER ART → works; Settings SD entry → downloads 2.1 GB that does nothing | **P1** |
| F-6 | **Export button double-tap** (Library ▸ VIDEO / More ▸ Export → `VideoExporterScreen`) | **partial** | Export button lacks `enabled = !exportProgress.isExporting` → two concurrent `MediaCodec`+`MediaMuxer` encodes possible. Also `bitrateMbps` is fixed at 12 (no slider despite the parameter) and the finished file has **no open/share action** on this screen. | Tap EXPORT MP4 twice quickly → two encodes | **P1** |
| F-7 | **Audio focus & interruptions** (`AudioPlayerManager`) | **partial** | No `AudioManager.requestAudioFocus`, no noisy-receiver, no focus-loss pause. Playback overlaps other apps and calls. Play/pause/seek/complete/error handling otherwise solid (error listener stops cleanly). | Start Melodia playback, play another app's audio → both play | **P1** |
| F-8 | **Island overlay** (Settings switch → `PlaybackIslandService`) | **complete (with caveats)** | Off by default ✓, `canDrawOverlays` re-checked before every show ✓, safe remove on revoke/stop ✓, notification fallback with MediaSession ✓, cutout-safe placement ✓. Caveats: `MediaSession.Callback.onPlay` routes through the `pauseAudio()` toggle (works but fragile); service starts on every play even when Island is disabled (foreground notification appears — arguably a feature, but it runs whenever any audio plays). | Settings → Island → play track → overlay appears; revoke permission → overlay removed | **P2** |
| F-9 | **Cloud transcription consent flow** (import → `MusicEngine.transcribeLyricsFromAudio`) | **complete** | Off by default, explicit `allowCloudUpload=true` argument required, 14 MB limit enforced, status text explains state in all four branches. Requires `GEMINI_API_KEY`; failure surfaces an error (not fake success). | Import audio with cloud off → "kept on device" message | — |
| F-10 | **Sync / word timing** (Sync → `LocalWhisperEngine.alignLyricsWithAudio`) | **complete (heuristic, honestly labelled)** | Real PCM decode → activity detection → word timing; UI says "heuristic, not speech recognition; review before export". LRC/JSON share intents work via FileProvider. | Sync → AUTO-CREATE WORD TIMINGS with imported audio + lyrics | — |
| F-11 | **MP4 export** (`LyricVideoExporter`, MediaCodec/MediaMuxer) | **partial** | Real hardware encode path with resolution/aspect handling; outputs to `cacheDir/exported_videos` (cache — can be evicted by the OS; should go to `filesDir` or MediaStore). correctness on-device **unverified** (needs a long-running emulator encode; planned in fix stage). | Export tab → EXPORT MP4 | **P2** |
| F-12 | **Studio/Library/Sync/Vault/Queue navigation & controls** | **complete** | All traced controls have real handlers (no empty callbacks found — grep for `onClick = { }` is empty). Bottom bar = 5 primary tabs; Visuals/Export/Settings reachable via More ✓. Back-handler returns to Studio ✓. | Instrumented nav tests pass (19/20) | — |
| F-13 | **Instrumented test bug** (`MainActivityTest.floatingPlayerBarContainerRenders`) | **broken (test only)** | Bar is `AnimatedVisibility(visible = currentUrl != null)` — correctly absent until audio plays; the test asserts it exists on a fresh launch. Test expectation is wrong, not the app. | Run connected tests → 1 failure | **P2** |
| F-14 | **Room schema safety** (`AppDatabase`) | **risk** | `fallbackToDestructiveMigration(true)` — any future schema bump silently deletes all user lyrics/drafts/songs. No `exportSchema`, no migrations. | Code inspection | **P1** |
| F-15 | **i18n / RTL** | **partial** | `supportsRtl=true`, Arabic lyric handling is real (normalizer, tests), but **all UI chrome is hardcoded English** — `strings.xml` has 1 entry, 0 `R.string` usages in Kotlin. Full extraction is a large refactor; staged approach proposed. | Any screen | **P2** |
| F-16 | **Security posture** | **partial** | `.env` is gitignored ✓ and not tracked ✓; key travels in `x-goog-api-key` header (not URL) ✓; release logging off ✓. Remaining: `usesCleartextTraffic="true"` (needed only for custom `http://` LLM base URLs), API key baked into APK via BuildConfig (documented AI-Studio pattern), keystore passwords in `build.gradle.kts` (template-mandated). | Code inspection | **P2** |
| F-17 | **Dead/legacy code** (user priority: clean up safely) | **unavailable (unused)** | 0-reference files verified by grep: `GenreSelectionScreen`, `WavPlaybackController`, `AudioAlignmentService`, `SecurityManager`, `BpmAnalyzer`, `AudioCacheManager`, `FreeLlmProvider`, dialogs `LyricsDialog`/`ExportLyricsDialog`/`EditTagsDialog`/`ManualSyncDialog`, `TrebloApiService` (only referenced by `ApiClient`, no call sites — audio-synthesis pivot leftover), and `app/src/main/cpp/whisper.cpp` (top-level stub, **not** in the CMake target, mismatched JNI names). Active UI does not reference any of them. | grep reference counts (see audit notes) | **P2** |
| F-18 | **Local provider engine lifecycle** (`Gemma3LocalLlmProvider`) | **partial** | New LiteRT-LM `Engine` created per `generate()` call (multi-second model load each time); unused `estimateTokenCount`. Should hold a cached engine. | Code inspection | **P3** |
| F-19 | **Floating player copy** | **cosmetic** | Expanded sheet says "MELODIA v3 AUDIO SYNTHESIS" — app does not synthesize audio (pivot removed it). | Open expanded player | **P3** |

## Prioritized fix plan (proposed stages — approval requested before starting)

**Stage 1 — make lyric generation actually work (P0: F-1, F-2, F-18)**
1. Implement `GeminiNanoLlmProvider` for real with ML Kit GenAI Prompt API (`GenerativeModel`, availability check, download-then-retry flow) — the dependency is already declared.
2. Add `PROVIDER_GEMMA3_LOCAL` to the Settings provider row; pass `Application` context through `MusicEngine` (change `LlmProviderFactory.create` call sites to accept context); cache the LiteRT-LM engine per provider instance.
3. Make the Settings badge reflect the *runtime* check, not a constant.
Files: `GeminiNanoLlmProvider.kt`, `LlmProviderFactory.kt`, `MusicEngine.kt`, `Gemma3LocalLlmProvider.kt`, `SettingsScreen.kt`.
Acceptance: with no key and no downloaded model, Nano path reports a specific actionable state; with Gemma 3 1B downloaded, lyrics generate end-to-end on device; unit tests for provider selection and fallback.

**Stage 2 — model-download truthfulness (P0: F-3, F-4 partial)**
1. Single shared `LocalModelManager` (ViewModel-owned) exposed to Settings; LiveData → StateFlow.
2. Real cancellable downloads (cancel the coroutine + close stream; resume = re-download, delete partial file on cancel).
3. Remove the `stable_diffusion_v15` entry (nothing consumes it — usage check documented) and either wire Whisper models to a real transcription path or label them "reserved — not yet used" honestly.
Files: `LocalModelManager.kt`, `MelodiaViewModel.kt`, `SettingsScreen.kt`.
Acceptance: progress bar moves during download; PAUSE actually stops bytes; no model entry implies a capability the app lacks.

**Stage 3 — playback correctness (P1: F-7, F-6, F-13) — DONE 2026-11-12**
- F-7: `AudioPlayerManager` now requests `AudioFocusRequest` before play (blocks playback if denied), pauses on transient/total loss, ducks to 0.2 on CAN_DUCK, restores on gain. (Emulator has no competing audio app; focus paths are code-reviewed + compile-verified, not live-audited on hardware.)
- F-6: export button disabled while `isExporting` (double-tap guard); bitrate selector chips (8/12/20/35 Mbps) wired to `bitrateMbps`; SHARE VIDEO via FileProvider (`${applicationId}.fileprovider`, `video/mp4`) after a completed export; output moved from `cacheDir` to `getExternalFilesDir(MOVIES)/exported_videos` (survives cache clears).
- F-13: instrumented test renamed `floatingPlayerBarHiddenUntilAudioLoaded`, asserts absence when idle.
- E2E on emulator: draft → Export screen shows chips + button; export ran ~90 s producing `Melodia_*.mp4` (5.28 MB) in `files/Movies/exported_videos/`; SHARE VIDEO opened the system share sheet (Drive/Quick Share visible).
- Note: completion state has no visible "complete" banner — the progress card only renders while exporting; the Share button appearing is the only completion cue (minor UX, candidate for Stage 5).
- Note: a stale incremental build (OneDrive + Gradle incremental mix) produced an APK with old+new classes; `clean` rebuild fixed it. If UI ever looks "old", rebuild clean before debugging.
1. Audio focus (gain/pause/duck/loss) + noisy receiver in `AudioPlayerManager`.
2. Disable export button while exporting; add bitrate slider; add open/share for the finished MP4; move output out of `cacheDir`.
3. Fix the floating-player test to assert after loading a track (or assert absence when idle).
Acceptance: focus-loss pauses playback; double-tap cannot start a second encode; instrumented suite green.

**Stage 4 — data safety & cleanup (P1–P2: F-14, F-17) — DONE 2026-11-12**
- F-14: `fallbackToDestructiveMigration(true)` removed; `exportSchema = true` + KSP `room.schemaLocation` (schemas in `app/schemas/`, 7.json + 8.json); explicit no-op `MIGRATION_7_8` registered via `addMigrations` (schema verified byte-identical between 7.json and 8.json). Future schema bumps must add a real migration — a missing one now throws instead of wiping user data.
- Migration test `AppDatabaseMigrationTest` (Robolectric + `MigrationTestHelper`): seeds all 5 tables at v7, migrates to v8, asserts every row survives and Room validates the schema. 2 tests, green.
- Build note: AGP 9 no longer packages unit-test assets; schemas are attached to the **debug** sourceSet assets so Robolectric can read them (release APKs stay clean).
- F-17: deleted 12 dead files (0 external references re-verified before deletion): `GenreSelectionScreen`, `WavPlaybackController`, `AudioAlignmentService`, `SecurityManager`, `BpmAnalyzer`, `AudioCacheManager`, `FreeLlmProvider`, dialogs `LyricsDialog`/`ExportLyricsDialog`/`EditTagsDialog`/`ManualSyncDialog`, `TrebloApiService` (+ removed the dead `trebloService` property from `ApiClient`), and `cpp/whisper.cpp` (stub, outside the CMake target — only `third_party/whisper.cpp` is compiled).
- Lesson: file-name grep missed that `OpenAiChatService`/`OpenAiChatRequest`/`OpenAiMessage`/`OpenAiChatResponse`/`OpenAiChoice` were defined *inside* `FreeLlmProvider.kt` and used by live `HuggingFaceLlmProvider`/`OpenRouterLlmProvider`. Restored them verbatim into `network/llm/OpenAiChatModels.kt`. Deletion checks must cover symbols defined inside deleted files, not just file names.
- Verification: clean `assembleDebug` + `testDebugUnitTest` (47 tests, 0 failures) + `connectedDebugAndroidTest` (20 tests, 0 failures) after deletions.

**Stage 5 — i18n/RTL & polish (P2–P3: F-15, F-16, F-8 caveats, F-19) — DONE 2026-11-12**
- F-16: `usesCleartextTraffic="true"` removed from the manifest; `OpenRouterLlmProvider.isAvailable()` now requires HTTPS (API key travels in a header — plain HTTP would leak it); HuggingFace provider already https-only; all model download URLs are https. 3 unit tests cover the https rule.
- F-8: `PlaybackIslandService.start()` now returns immediately unless the Island preference is enabled — plain in-app playback no longer spawns a foreground notification. 2 Robolectric tests prove gating both ways.
- F-19: expanded player copy corrected "MELODIA v3 AUDIO SYNTHESIS" → "MELODIA v3 PLAYBACK" (app does not synthesize audio).
- F-15 (first slice): bottom-bar tab labels + floating-player strings extracted to `strings.xml` (`tab_*`, `player_*`); `MelodiaTab` now carries `titleRes`; values are byte-identical to the old literals so instrumented text matchers stay green. Remaining screens (Studio/Lyrics/Library/Settings/…) still hardcoded — extraction continues screen-by-screen in follow-ups; Arabic lyric handling was already real (normalizer + tests).
- Emulator checks: all 5 tab labels render from resources; forced RTL (`debug.force_rtl 1`) renders with no crash; `font_scale 1.3` renders with no crash. Both settings reset afterwards.
- Verification: `assembleDebug` + `testDebugUnitTest` (52 tests, 0 failures) + `connectedDebugAndroidTest` (20 tests, 0 failures).

## Not testable in this environment (documented honestly)

- Real Gemini Nano generation requires a physical supported device (AICore) — emulator reports unavailable. **Physical-device checklist when available:** (1) Settings → Nano Local tile must show READY after AICore downloads its model; (2) Studio → CREATE LYRICS must produce a draft with provider toast "Creating lyrics with Nano Local…"; (3) airplane-mode test: generation must still work with no network; (4) `adb logcat` must show no LlmException from the Nano path.
- Real Gemini cloud calls are not made from tests (no paid requests); provider logic tested via fakes.
- 4K encode profile behaviour depends on device encoder caps; verified at 1080p on the emulator.

## On-device verification — Gemma 3 1B (2026-11-12, emulator-5554)

The Gemma 3 local inference path was verified END-TO-END on the x86_64 emulator (LiteRT-LM 0.17.1 ships x86_64 JNI):
1. Settings → Gemma 3 tile selected; 584,417,280-byte `gemma-3-1b-it-int4.litertlm` downloaded in ~5 min with live progress; UI shows READY.
2. Studio → CREATE LYRICS with the Gemma 3 provider: `liblitertlm_jni.so` loaded, engine initialized, inference completed in ~2 min (emulator CPU).
3. Result: draft "Crimson Echoes" — 177 words, 7 sections ([Intro]/[Verse 1]/[Chorus]/[Verse 2]/[Bridge]/[Outro]), coherent on-theme lyrics verified in the drafts database. Drafts count incremented.
4. **Bug found and fixed during verification:** `Gemma3LocalLlmProvider` appended the raw `Message` object from the `sendMessageAsync` Flow instead of extracting `Content.Text` — output would have been garbage. Fixed to `message.contents.contents.filterIsInstance<Content.Text>()`.
5. Side note: `SentimentAnalyzer` logs a Nano-unavailable LlmException then falls back to heuristics — graceful, but the error-level log is noisy (cosmetic follow-up).

Gemini Nano remains unverified until a physical device is available (checklist above).

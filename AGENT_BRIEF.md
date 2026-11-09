# Melodia: brief for the Android Studio agent

Read this whole file first. Do ONE task at a time, on its own branch, and report after each.

## 0. What was prepared for you

The folder `overlay/` mirrors the project tree. It contains finished first-draft code that was written WITHOUT a compiler:

1. Video timing fix: `domain/EncoderInputSurface.kt` (new), `domain/LyricVideoExporter.kt`, `DomainLogicTest.kt` (9 new tests for `framePresentationTimeNs`), `AppDatabase.kt`, `AudioPlayerManager.kt`, `CHANGES.md`.
2. Nothing-style UI: new package `ui/nothing/` (prefix `Nt`), `res/values/nt_strings.xml`, `res/values-ar/nt_strings.xml`, adaptive launcher icon resources, `docs/NOTHING_UI_SPEC.md`.

`melodia-improvements-v2.patch` and `melodia-nothing-ui.patch` hold the same changes as git patches (apply v2 first). Prefer copying `overlay/` over the project (the user does this step). If the project already contains a different `LyricVideoExporter.kt` (for example an earlier attempt that patched `bufferInfo.presentationTimeUs` in the muxer loop), REPLACE it with the overlay version. That muxer-timestamp approach is wrong: encoder output order and count do not match the render loop, and the encoder still receives wall-clock timestamps.

## 1. Standing rules

- One task per branch: `task/<id>-<short-name>`. Commit message `<id>: <what changed>`.
- The app never generates or synthesizes song audio.
- Reuse existing code first (`MelodiaDesignSystem.kt`, `ui/theme/*`, existing dot-matrix and Space Mono fonts). Add no new libraries or fonts without saying why and which version.
- No hardcoded user-visible strings: use `stringResource` and add both `values/` and `values-ar/`.
- Everything must work in RTL, with mixed Arabic/English text, and at 200% font scale.
- Every animation must stop when animator scale is 0 (`rememberReducedMotion`).
- Do not change a Room entity without bumping the version AND writing a `Migration`.
- After every task run `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`, then test on a device.
- Finish each task with a report: files changed, what you verified and how, and what you could NOT verify. Never claim a test or visual check passed if you did not do it.
- If something here does not match the real code, say so and stop for confirmation.

Environment notes seen in earlier sessions: the shell is PowerShell (`&&` and `2>&1` do not work, run commands one by one); `write_file` can fail with "edited by someone else" (use `replace_file_content`, or re-read the file first); the Gradle tool takes only the task name.

## 2. Tasks, in order

### B0. Baseline and overlay check
Confirm `domain/EncoderInputSurface.kt` exists and `LyricVideoExporter.kt` has no `lockCanvas`. Run Gradle sync, `assembleDebug`, `testDebugUnitTest`, `lintDebug`. Fix compile errors ONLY in files from the overlay. List every pre-existing failure separately and do not fix them yet.

### B1. Verify video sync on a real device
Export a 60 s audio track at 1080p. Read the MP4 duration with `MediaMetadataRetriever` and compare with the audio. Pass: difference under 0.3 s and the last lyric line ends with the audio. Check Logcat tag `LyricVideoExporter` for errors. If EGL fails on a device, report the exact error and device model.

### B2. Audio formats
Before rendering, read the audio MIME type and fail early with a localized message if it cannot be muxed into MP4 (treat AAC as supported; check `MediaMuxer` docs for the others). Then optionally transcode to AAC with `MediaCodec`.

### B3. Remove dead Treblo code
Delete unused Treblo network classes and UI text. Do NOT drop Room columns in this task; mark them deprecated.

### B4. Room safety
`exportSchema = true` with `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, commit the schema JSON, add a `MigrationTestHelper` test.

### B5. Network and privacy
Remove `usesCleartextTraffic="true"` unless a real HTTP use exists. Request `RECORD_AUDIO` only at the moment of use. Cloud transcription stays opt-in.

### B6. UI previews
Open each `@Preview` in `ui/nothing/NtPreviews.kt` (Studio, Lyrics, Visuals, Sync, Export, Library, Settings, Icons, and the Arabic and 200% variants). Compare with `docs/NOTHING_UI_SPEC.md`. Fix rendering problems in `ui/nothing/` only.

### B7. Launcher icon
Check the adaptive icon under circle, squircle and square masks and in themed icons (Android 13+). Everything must be inside the 66dp safe zone.

### B8. Navigation shell
Replace the 7-tab bar with 3 destinations (Project, Library, Settings) using `NtAppShell`; Studio, Lyrics, Visuals, Sync and Export become the 5 project steps (`ProjectStep` enum, `NtStepIndicator`). Keep state saving and back behavior. Update `MainActivityTest` tags.

### B9. Screens, one per task
Connect each `Nt*Content` composable to the real ViewModel state, replace the old screen, and delete unused old code: Studio, Lyrics, Visuals, Sync, Export, then Library and Settings. Do not invent state: find the real fields and name them in the report.

### B10. Quality
Test at 100% and 200% font, Arabic (RTL) and English, animator scale 1 and 0. Check contrast (use `Nt.RedText` for small red text). Profile one animated screen with the Profiler and report frame times. Update `CHANGES.md`.

## 3. Not covered yet
Splash, waiting-animation catalog, manual timing editor, voice dictation, genre picker, drafts, lyrics export sheet, empty/error/offline states, real Whisper transcription.

## 4. Prompt for each task
Read AGENT_BRIEF.md. Do task **B__** only. First list the files you will change and any assumption that does not match the code. Then implement, run `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`, check on a device, and finish with the report.

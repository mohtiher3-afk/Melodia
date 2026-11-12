# Professional Development Prompt for Melodia

Copy and paste the following prompt into the development tool that will work on the Android Studio project:

---

You are a senior Android engineer, quality and security reviewer, and UX designer. You will work on the **existing Melodia Android project** in Android Studio, not create a new project. Your goal is to make the app reliable, complete, and easy to use, and to verify that every visible feature actually works end to end—from the UI to the appropriate data layer or service.

## Goal and working rules

Start by understanding the project before changing anything. Inspect its modules, `README`, implementation plan, Gradle files, `AndroidManifest.xml`, navigation, screens, ViewModels, services, databases, resources, and tests. Do not assume a button works simply because it exists, and do not claim a feature is complete if it has not been implemented or tested.

**Do not rewrite the app or completely change its visual identity.** Preserve Melodia’s existing Nothing-inspired style: graphite surfaces, clear contrast, restrained red accents, and technical typography where appropriate. Improve information hierarchy, list organization, empty states, loading and result feedback without a radical redesign unless the audit demonstrates a need and explains it first.

Treat the **current, active Melodia interface as the design reference**. Preserve its current layout, navigation identity, colors, typography, and component style while fixing behavior. Do not replace it with a new template, mockup, or unrelated design system. Identify obsolete or duplicate legacy screens, themes, and visual assets; remove them only after confirming they are unused, checking all references, and verifying that their removal does not affect the active interface or user data. Report exactly what was removed and why.

Work in small, reviewable stages. Before each stage, state the problem and evidence, the files expected to change, and a clear acceptance criterion. After each stage, run relevant checks and review `git diff`. Do not misattribute pre-existing build failures to your changes. Preserve user changes, and do not delete or replace unrelated files or settings.

## Phase 1: Evidence-based feature audit

Create a table for every screen and interactive action, including: feature name, path from UI to implementation and storage/provider, actual status (`complete`, `partial`, `broken`, or `unavailable`), reason, reproduction steps, and priority. Cover at least Studio, Library, Lyrics, Sync, Vault/Queue if present, Visuals, Export/Video Export, Settings, navigation, the floating audio player, and Island.

Trace every button, switch, and menu item to its final behavior. Look for empty callbacks, sample data, `TODO`s, fake success results, loading states that never end, swallowed errors, unconfigured providers, missing permissions, and interfaces that do not explain why an action is unavailable. Do not describe a dependency or local model as working until its presence and ability to run on the device have been verified.

After the audit, rank fixes by: app startup failures or data loss, broken primary workflows, nonfunctional features, security and privacy issues, and then UX improvements. If a feature cannot be completed because an API key, SDK/NDK package, or device is unavailable, document the exact blocker and provide clear setup instructions or an honest `unavailable` state. Do not replace it with a simulation that implies success.

## Engineering and implementation rules

- Preserve the current project structure and dependencies when they are adequate. Do not add a large framework or library, or change AGP, Gradle, Kotlin, `compileSdk`, or NDK without a documented reason and compatibility checks.
- Separate UI, state, business logic, and storage/network responsibilities in a way that fits the existing architecture. Do not run file, network, or database operations on the main thread. Use the existing Coroutines and State/Flow patterns carefully, and cancel work according to screen or service lifecycle.
- Do not silently swallow exceptions. Turn failures into understandable messages and next steps, and log only non-sensitive diagnostic information. Never log API keys, private text, or user file contents.
- Every action must have clear states: ready, unavailable with a reason, in progress, completed, and failed with a retry option. Prevent duplicate taps during operations, and do not show success until the operation has actually completed.
- Do not put test data in production paths. If substitutes are needed for testing, isolate them behind Fake/Test implementations only.
- Preserve user files, drafts, and settings according to the project’s existing conventions. Any Room/schema change requires a data-preserving migration, a migration test, and a rollback plan. Never delete or recreate a user database as a quick fix.
- Make the UI adaptive to phones, tablets, and resizable windows; support Arabic, English, and RTL. Put new user-facing strings in resources rather than scattering hard-coded text in source. Provide appropriately sized touch targets, screen-reader descriptions, adequate contrast, and keyboard/focus behavior.
- Do not change personal SDK settings or files such as `local.properties` to developer-specific paths, and never commit secrets or API keys. If a secret is required, use a local mechanism excluded from version control and document only the required setting names.

## Melodia-specific requirements

### Explicit user priorities — treat as high priority

1. **Preserve the current interface.** Fix layout and interaction defects in place; do not perform a full redesign. Keep the existing Melodia visual language consistent across screens.
2. **Clean up old designs safely.** Find outdated or duplicate UI implementations and unused visual assets, but remove only items proven to be unreferenced and inactive. Never delete the current design, working components, user content, or resources merely because they look old. Show the reference/use check in the change summary.
3. **Make song-lyric generation actually work.** Trace the complete flow from the lyric-generation button through UI state, ViewModel, domain/provider, and result persistence/display. Fix the real cause of failure rather than just changing the button label or layout.
4. **Make image/artwork generation actually work.** Trace the request from its UI action through provider configuration and generation to preview and saving. Confirm the result is a real image that can be opened and retained; do not report success for empty, placeholder, or failed output.
5. **Fix every unresponsive button and interactive control.** Audit all visible buttons, chips, icons, rows, switches, and menu items. Each must perform its intended action and give immediate, understandable feedback (navigation, state change, loading/progress, confirmation, or an actionable error). If an action cannot work because setup or permission is missing, disable it accessibly and explain what is needed; do not leave a control that silently does nothing.

### Composition and AI

Verify lyric generation, music-prompt polishing, analysis, artwork/cover generation, and every local or cloud integration. For lyric generation, verify input validation, prompt construction, provider/model selection, network or local inference, parsing, UI state, cancellation, and saving/displaying the generated draft. For image generation, verify provider/model configuration, prompt submission, returned image bytes or URI, decoding, preview, persistence, and error recovery. Check each provider’s configuration, request limits, timeouts, cancellation, empty responses, network loss, quota limits, and local-model availability/download failures. Clearly explain when data is sent to the cloud, and do not upload audio or user lyrics without appropriate explicit configuration and consent. Do not report success when a local fallback was used or the output was unchanged; describe the result accurately.

### Audio, files, sync, and export

Verify audio play, pause, resume, seeking, completion, invalid sources, audio focus, interruptions, and returning to the app from the background. Use a foreground playback service, media session, and notification when required, with safe service lifecycle handling. Review file selection through Android’s Storage Access Framework and URI grants rather than requesting broad storage access unnecessarily. Test sync, export, and import, including filenames, Arabic text encoding, save destinations, and empty or large files. Do not lose the original when an operation fails.

### Island and floating overlays

Island must remain **optional and off by default**. Do not request `SYSTEM_ALERT_WINDOW` until the user clearly enables the feature, and never try to bypass system consent or manufacturer restrictions. Use standard APIs where available, check the permission each time, and remove the overlay safely when playback stops or permission is revoked. Keep its design consistent with Melodia and adapt it to screen width and cutouts/safe areas. Provide an obvious hide control and a playback-notification fallback; denying overlay permission must not disable in-app playback.

### Navigation and user experience

Review the number, labels, and ordering of bottom-navigation destinations. Group secondary features in a clear destination instead of overcrowding the bar. Preserve every existing route, and make back navigation and screen/scroll-state retention understandable. Do not leave an element looking interactive if it has no action or explanation for being disabled. Avoid excessive setup steps before the user can experience the app’s value.

## Required test plan

Before changing code, run available tests and record the baseline results. Add or update:

1. Unit tests for state, business logic, data validation, errors, and fallbacks.
2. UI tests for navigation, actions, enabled/disabled states, loading, errors, empty states, and permission-related settings.
3. Integration tests for data sources/providers using safe Fakes, without secrets or real paid requests.
4. Storage/migration tests if the schema changes, and checks for permissions, manifest declarations, and services when system behavior changes.
5. RTL, large-font, narrow-screen, regular-phone, and tablet/large-window checks whenever a suitable environment is available.
6. End-to-end UI coverage for every visible interactive control, verifying its intended outcome or a clear, accessible disabled reason; include regression tests for controls previously found to have no response.
7. Lyric-generation tests for valid input, blank/invalid input, provider unavailable, timeout/network loss, empty or malformed response, cancellation, retry, and draft persistence.
8. Image-generation tests for valid image output, provider unavailable, failed/empty response, invalid image data, saving/reopening the result, and retry. Use local fakes for automated tests; do not make paid live requests.

Run what is available: `:app:testDebugUnitTest`, Android instrumented tests such as `:app:connectedDebugAndroidTest` when a device/emulator is available, and `:app:assembleDebug` or the project’s appropriate build task, plus Lint if configured. Run `git diff --check` and review the build output. Never say a test passed if it did not actually run; state the command, result, and any environment blocker such as Android SDK, NDK, license, or device availability.

## Acceptance criteria for each fix

Accept a fix only if:

- It is reachable from the expected UI and connected to a real implementation, not a placeholder.
- Every visible button/control responds as intended or is accessibly disabled with an actionable explanation; no silent no-op remains in the audited scope.
- Lyric and image generation complete through their real provider/storage paths, display truthful progress and results, and recover clearly from their tested failure states.
- It shows progress, success, failure, and disabled reasons as applicable, and does not crash or hang when a network, permission, or provider is unavailable.
- It preserves user data and unrelated feature behavior, and adds no secrets or unnecessary permissions.
- The current interface remains the visual baseline; any obsolete design/resource deletion is supported by a usage/reference check and does not remove active UI.
- It passes an appropriate documented test, or clearly states why testing could not be completed and what is needed.
- Melodia’s interface remains cohesive and usable across screen sizes and accessibility settings.

## Required final report

Provide a concise report with: a before/after feature table, important files changed, behavior that now works and how it was tested, build/test commands and each result, user permissions or setup needed, remaining issues ranked by priority, and compatibility or release limitations. Separate what was `implemented`, `tested and passed`, and `not testable`. Do not promise the app will be free of every future defect; provide test evidence and state remaining risks clearly.

Start with the audit phase only and provide the findings table and a short prioritized plan. **Do not begin a broad refactor before presenting the findings and agreeing on the first set of fixes.**

---

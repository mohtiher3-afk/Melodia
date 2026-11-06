# Melodia

Nothing OS-inspired Android studio for writing, editing, transcribing, syncing, and exporting song lyrics.

## Overview

Melodia is an Android lyric-writing and local-audio app. The current default lyric backend is Gemini Nano through Android AICore, which runs on-device when the phone supports it. Local playback and cover-art generation do not need an AI API. Melodia does not synthesize song audio.

## Features

- **On-device lyric drafting** — Gemini Nano via ML Kit Prompt API on supported phones; AICore may need to provision its model first. No per-request API bill, but availability depends on device and system rollout.
- **Lyric Studio** — Edit and auto-save lyrics, add song sections, and request on-device rhyme suggestions.
- **Sentiment and prompt tools** — Use Gemini Nano locally where available; heuristic fallbacks remain available when the model is not.
- **Local audio library** — Import and play local audio files without uploading them.
- **Optional cloud transcription** — Off by default. If the user explicitly enables “Allow Gemini cloud transcription” in Settings, imported audio under 14 MB may be uploaded to Gemini; this can use quotas or incur charges depending on the key/account.
- **Automatic word timing** — Estimate karaoke timing from audio-active regions, word lengths, and lyric sections; review before export. This is not forced alignment.
- **Local cover art** — Procedural monochrome/red artwork is generated offline. A genuine on-device image-diffusion model is not yet integrated; Gemini image generation is not called by the current cover-art path.
- **Text-only music prompts**, **BPM detection**, **Karaoke preview**, and **LRC/JSON/TXT/DOC/MP4 lyric exports**.

## Privacy and AI limitations

The local Gemini Nano option requires a supported phone and Android AICore; the model may need to be downloaded by the system. On unsupported phones, text generation reports that Nano is unavailable rather than silently switching to a cloud model. Cloud audio transcription requires a separate, explicit opt-in and is disabled by default. The current C++ Whisper bridge is still a stub; local Whisper transcription is not implemented yet. Automatic timing uses an energy-aware heuristic and can mistake instruments for vocals.

For the implementation plan and official references on local LLM, image, and audio options, see [`docs/free_ai_architecture.md`](docs/free_ai_architecture.md).

## Tech stack

- **Language/UI:** Kotlin, Jetpack Compose
- **Architecture:** Room, Repository pattern, ViewModel
- **On-device text AI:** ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt:1.0.0-beta4`) backed by Android AICore/Gemini Nano
- **Optional cloud AI:** Gemini API for audio transcription only when the user enables it; optional cloud LLM providers are shown separately
- **Audio:** Android media APIs, local PCM analysis, and an energy-aware timing heuristic
- **Build:** Gradle, KSP, Google Services

## Run locally

**Prerequisites:** Android Studio, Android SDK 36, JDK 21.

1. Open this directory in Android Studio and allow Gradle sync.
2. Run on a physical Android device. To use local lyric drafting, the device must support Gemini Nano/AICore; the app checks this when generating.
3. No `.env` key is needed for the local lyric path or procedural cover art. Set a Gemini API key only if you intend to enable cloud transcription; review the usage and billing terms for that key.

## Project structure

```text
app/
├── src/main/java/com/example/
│   ├── data/          # Room entities, DAOs, repositories
│   ├── domain/        # Business logic, local audio and lyric tools
│   ├── network/       # Optional cloud clients and LLM providers
│   ├── ui/            # Compose screens, components, dialogs, theme
│   └── MainActivity.kt
├── src/main/cpp/      # Whisper bridge stubs (not an integrated Whisper engine yet)
├── src/main/res/      # Strings, colors, icons, fonts
└── src/test/          # Unit and Robolectric tests
```

## License

This project is for personal use.

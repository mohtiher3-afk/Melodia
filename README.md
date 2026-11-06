# Melodia

Nothing OS-inspired AI Music & Lyrics Studio powered by Treblo API and Gemini.

## Overview

Melodia is a full-featured Android application for music creation, lyric writing, and audio production. It combines a clean Nothing OS aesthetic with on-device and cloud AI capabilities.

### Features

- **AI Music Generation** — Create melodies and chord progressions powered by Gemini
- **Lyric Studio** — Write, auto-save, and rhyme lyrics with AI assistance
- **Voice Dictation** — Convert speech to lyrics using on-device Whisper
- **Rhyme Analyzer** — Find rhyming words and suggestions
- **BPM Detection** — Analyze audio tempo from local files
- **Cover Art Generation** — Generate album art via AI
- **Karaoke Lyric Sync** — Sync lyrics to audio playback
- **Audio Importer** — Import local audio files into the library
- **Free AI APIs** — Multiple free LLM providers (HuggingFace, Together, OpenRouter, FreeLlm)
- **Nothing OS Design** — Clean, minimalist UI with monospace typography

## Tech Stack

- **Language:** Kotlin
- **Framework:** Jetpack Compose
- **Architecture:** Room database, Repository pattern, ViewModel
- **Networking:** Retrofit, Treblo API
- **AI:** Gemini API, Whisper (on-device), Free LLM providers
- **Build:** Gradle 9.3.1, KSP, Google Services

## Run Locally

**Prerequisites:** Android Studio (latest), Android SDK 36, JDK 21

1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in it (see `.env.example` for an example)
5. Run the app on an emulator or physical device

## Project Structure

```
app/
├── src/main/java/com/example/
│   ├── data/          # Room entities, DAOs, repositories
│   ├── domain/        # Business logic, audio engines, AI analyzers
│   ├── network/       # API clients, LLM providers
│   ├── ui/            # Compose screens, components, dialogs, theme
│   ├── security/      # Security manager
│   └── MainActivity.kt
├── src/main/cpp/      # Whisper native bridge
├── src/main/res/      # Strings, colors, icons, fonts
└── src/test/          # Unit and Robolectric tests
```

## License

This project is for personal use.
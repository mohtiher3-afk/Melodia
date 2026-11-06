# Implementation Plan: Melodia - Text-First Lyric Studio, Local Whisper Sync, and MP4 Lyric Video Exporter

This document outlines the architectural transformation of **Melodia** from an AI audio generator into a **Text-First Song Lyric Studio, Local Audio Alignment Engine (Whisper.cpp NDK), and 4K MP4 Lyric Video Exporter**.

---

## 1. Product Pivot & Elimination of AI Audio Synthesis

### Objectives:
- Completely remove and permanently disable all AI audio synthesis, TTS singing, MusicGen, Treblo audio rendering, and procedural audio file generation.
- Ensure the app **never** generates a new song, music, vocal track, or audio file.
- Permit **only**:
  1. Lyric writing, AI lyric expansion, and section structuring.
  2. Text-based Music Prompt generation (copyable/editable text only).
  3. Cover Art image generation.
  4. User-supplied local audio file import.
  5. Local Whisper / NDK audio transcription and word alignment for imported audio.
  6. LRC / JSON word-timing export.
  7. High-fidelity 1080p/4K MP4 Lyric Video rendering using native `MediaCodec` and `MediaMuxer` (with imported audio or silent track).

### Files to Modify / Delete:
- **`com.example.domain.ProceduralAudioSynthesizer`**: Remove audio generation logic; keep purely silent or placeholder time calculation utilities if needed for timing.
- **`com.example.domain.MusicEngine`**: Remove `synthesizeAudio()`, `sendToTreblo()`, `sendToHuggingFaceMusicGen()`, and `fallbackOfflineAudio()`. Keep `generateSongDraft()` (lyrics & text prompt only), `autoPolishPrompt()`, `suggestTagsFromPrompt()`, `generateNextLine()`, and `transcribeLyricsFromAudio()`.
- **`com.example.ui.components.RawAudioStreamPlayer`**: Remove / disable.
- **`com.example.ui.components.WavPlaybackController`**: Adapt into `AudioPlaybackController` strictly for user-imported local audio files.
- **`com.example.network.TrebloApiService` & `HuggingFaceApiService`**: Disable audio synthesis network endpoints.

---

## 2. Text-First Studio Architecture & Reorganized UI Flow

### Sections & Navigation:
Reorganize the top-level app navigation into 5 core tabs:
1. **`Studio`**: Project Overview, Cover Art status, Lyrics status, Imported Audio status, Sync status, `CREATE LYRICS` and `IMPORT AUDIO` quick actions.
2. **`Lyrics`**: Interactive Lyric Editor with section management (`Intro`, `Verse`, `Pre-Chorus`, `Chorus`, `Bridge`, `Outro`), word/character counters, long line warnings, auto-save to Room database, and full Arabic/RTL support.
3. **`Visuals`**: Cover Art Generator (`generateCoverArtOnly()`) with 9:16, 16:9, and 1:1 aspect ratio previews, status badges (`GENERATING`, `READY`, `FAILED`, `OFFLINE FALLBACK`).
4. **`Sync`**: Local Whisper C++ / JNI Alignment Engine for imported audio tracks, confidence color coding (Green >= 0.85, Yellow 0.60-0.84, Red < 0.60), manual timing editor, and LRC/JSON export.
5. **`Export`**: Pre-Export Review Flow (`01 CONTENT` → `02 VISUAL` → `03 REVIEW` → `04 EXPORT`) and 1080p/4K MP4 Lyric Video Exporter (`MediaCodec` + `MediaMuxer`).

---

## 3. Local Whisper C++ NDK Engine (`melodia_whisper`)

### Architecture:
- **C++ Source**: `app/src/main/cpp/whisper.cpp` and `app/src/main/cpp/melodia_whisper_bridge.cpp`.
- **CMake Configuration**: `app/src/main/cpp/CMakeLists.txt` producing `libmelodia_whisper.so`.
- **Kotlin Bridge**: `com.example.domain.WhisperNativeBridge` and `com.example.domain.LocalWhisperEngine`.
- **GGML Model Management**: Downloads `ggml-base.bin` on demand to app private storage.
- **Audio Extraction**: Decodes imported user audio using `MediaExtractor` and `MediaCodec` into 16kHz Mono 32-bit Float PCM.
- **Arabic Text Normalization**: Normalizes diacritics, Alef/Ya variants, Tatweel, and punctuation for token matching, maintaining original text for display.
- **Fallback**: Weighted temporal alignment fallback if NDK or model is unavailable.

---

## 4. Real MP4 Lyric Video Exporter (`LyricVideoExporter`)

### Architecture:
- Uses `MediaCodec` (H.264/AVC) and `MediaMuxer` to generate true hardware-encoded `.mp4` video files.
- **Profiles Supported**:
  - `Vertical 9:16` (1080x1920)
  - `Landscape 16:9` (1920x1080)
  - `Square 1:1` (1080x1080)
  - `4K Vertical` (2160x3840)
  - `4K Landscape` (3840x2160)
  - `4K Square` (2160x2160)
- **Audio Handling**: Merges the imported user audio track as-is without re-encoding, or creates a silent video track if no audio is imported. Never generates new audio.
- **Bitrate Control**: Adjustable slider from 4 Mbps to 50 Mbps (clamped 2-80 Mbps) passed to `MediaFormat.KEY_BIT_RATE`.
- **Safe Area**: On-screen overlay guides and text positioning bounds.
- **Hardware Fallback**: Graceful fallback notification to 1080p if the device hardware encoder does not support 4K resolution.
- **Sharing**: Integrated `FileProvider` for Android share sheet.

---

## 5. Design System, RTL & Accessibility

- Charcoal, black, and dark grey base palette with Nothing Red and White accents.
- Unified Material 3 typography (`Space Mono` and system fonts).
- Touch targets >= 48dp across all interactive controls.
- Full RTL Support (Arabic text alignment, mixed text handling, layout direction).

---

## 6. Execution & Verification Steps

1. **Purge Audio Generation**: Disable/remove audio synthesis logic in `MelodiaViewModel`, `MusicEngine`, and UI components.
2. **Setup C++ / CMake for Whisper**: Add CMake configuration in `build.gradle.kts` and C++ JNI bridge files.
3. **Build Text-First Studio**: Update `StudioScreen`, `MelodiaViewModel`, `StudioUiState`, and lyric editor.
4. **Implement Local Audio Alignment & Whisper**: Build `LocalWhisperEngine`, PCM extractor, Arabic text normalizer, and `SyncScreen`.
5. **Build Real MP4 Video Exporter**: Implement `LyricVideoExporter` with `MediaCodec`, `MediaMuxer`, 4K/1080p profiles, and pre-export readiness check.
6. **Unified Styling & RTL**: Ensure consistent dark theme and complete RTL support.
7. **Verification**: Compile with `compile_applet`, run unit tests, and generate `QA_REPORT.md` & updated `README.md`.

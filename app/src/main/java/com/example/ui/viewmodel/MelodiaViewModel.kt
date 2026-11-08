package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.LyricDraft
import com.example.data.model.Preset
import com.example.data.model.RecentSubGenre
import com.example.data.model.Song
import com.example.data.repository.DraftsRepository
import com.example.data.repository.PresetRepository
import com.example.data.repository.RecentSubGenreRepository
import com.example.data.repository.SongRepository
import com.example.domain.AudioPlayerManager
import com.example.domain.CoverArtGenerator
import com.example.domain.ExportProgressState
import com.example.domain.ImportedAudioMetadata
import com.example.domain.LocalAudioImporter
import com.example.domain.LocalWhisperEngine
import com.example.domain.LyricVideoExporter
import com.example.domain.MusicEngine
import com.example.domain.RhymeResult
import com.example.domain.SentimentAnalyzer
import com.example.domain.SentimentResult
import com.example.domain.VideoAspectRatio
import com.example.domain.VideoResolution
import com.example.network.GeneratedSongDraft
import com.example.network.KaraokeLyrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class GenerationVersion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: String,
    val title: String,
    val genre: String,
    val mood: String,
    val language: String,
    val vocalStyle: String,
    val bpm: String,
    val promptText: String,
    val lyrics: String,
    val tags: List<String>,
    val coverImagePath: String? = null
)

data class StudioUiState(
    val activeTab: String = "STUDIO",
    val selectedGenre: String = "Arabic Pop",
    val selectedMood: String = "Energetic",
    val selectedLanguage: String = "Bilingual",
    val userTopic: String = "",
    val moodKeywords: String = "",
    val vocalStyle: String = "Auto-Tuned Male",
    val selectedBpm: String = "120 BPM",
    val vocalReverb: String = "Studio Clean",
    val isInstrumental: Boolean = false,
    val isGenerating: Boolean = false,
    val isGeneratingCover: Boolean = false,
    val isPolishingPrompt: Boolean = false,
    val currentDraft: GeneratedSongDraft? = null,
    val karaokeLyrics: KaraokeLyrics? = null,
    val statusMessage: String? = null,
    val isPlayingPreview: Boolean = false,
    val isAutoSaving: Boolean = false,
    val lastAutoSavedTime: String? = null,
    val activeDraftId: Long? = null,
    val sentimentResult: SentimentResult? = null,
    val isAnalyzingSentiment: Boolean = false,
    val highlightSentimentColors: Boolean = false,
    val rhymeResult: RhymeResult? = null,
    val isFindingRhymes: Boolean = false,
    val bpmResult: Int? = null,
    val isAnalyzingBpm: Boolean = false,
    val versionHistory: List<GenerationVersion> = emptyList(),
    val isSavingPreset: Boolean = false,
    val isSuggestingTags: Boolean = false,
    val suggestedTags: List<String> = emptyList(),
    val isImportingAudio: Boolean = false,
    val isTranscribingAudio: Boolean = false,
    val isAligningAudio: Boolean = false,
    val isLocalAudioActive: Boolean = false,
    val localAudioMetadata: ImportedAudioMetadata? = null,
    val isAutoRhymeActive: Boolean = true,
    val exportProgress: ExportProgressState = ExportProgressState()
)

class MelodiaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private val draftsRepository: DraftsRepository
    private val presetRepository: PresetRepository
    private val recentSubGenreRepository: RecentSubGenreRepository
    private val prefs = application.getSharedPreferences("melodia_prefs", Context.MODE_PRIVATE)
    val audioPlayerManager = AudioPlayerManager(application)
    private var autoSaveJob: Job? = null

    private val _studioState = MutableStateFlow(StudioUiState())
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    private val _cacheSizeFormatted = MutableStateFlow("0 B")
    val cacheSizeFormatted: StateFlow<String> = _cacheSizeFormatted.asStateFlow()

    private val _defaultExportFormat = MutableStateFlow(
        prefs.getString("default_export_format", "LRC") ?: "LRC"
    )
    val defaultExportFormat: StateFlow<String> = _defaultExportFormat.asStateFlow()

    fun updateDefaultExportFormat(format: String) {
        _defaultExportFormat.value = format
        prefs.edit().putString("default_export_format", format).apply()
    }

    private val _llmProviderId = MutableStateFlow(
        prefs.getString("llm_provider_id", com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO)
            ?: com.example.network.llm.LlmProviderFactory.PROVIDER_GEMINI_NANO
    )
    val llmProviderId: StateFlow<String> = _llmProviderId.asStateFlow()

    fun updateLlmProvider(id: String) {
        _llmProviderId.value = id
        prefs.edit().putString("llm_provider_id", id).apply()
    }

    private val _allowCloudTranscription = MutableStateFlow(
        prefs.getBoolean("allow_cloud_audio_transcription", false)
    )
    val allowCloudTranscription: StateFlow<Boolean> = _allowCloudTranscription.asStateFlow()

    fun setAllowCloudTranscription(allowed: Boolean) {
        _allowCloudTranscription.value = allowed
        prefs.edit().putBoolean("allow_cloud_audio_transcription", allowed).apply()
    }

    fun setActiveTab(tab: String) {
        _studioState.update { it.copy(activeTab = tab) }
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = SongRepository(db.songDao())
        draftsRepository = DraftsRepository(db.lyricDraftDao())
        presetRepository = PresetRepository(db.presetDao())
        recentSubGenreRepository = RecentSubGenreRepository(db.recentSubGenreDao())

        viewModelScope.launch {
            audioPlayerManager.isPlaying.collect { playing ->
                _studioState.update { it.copy(isPlayingPreview = playing) }
            }
        }
    }


    val savedSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedDrafts: StateFlow<List<LyricDraft>> = draftsRepository.allDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedPresets: StateFlow<List<Preset>> = presetRepository.allPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentSubGenres: StateFlow<List<RecentSubGenre>> = recentSubGenreRepository.recentSubGenres
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setGenre(genre: String) {
        _studioState.update { it.copy(selectedGenre = genre) }
        triggerDebouncedAutoSave()
        viewModelScope.launch {
            val resolvedCategory = when {
                genre.contains("R&B", ignoreCase = true) || genre.contains("Soul", ignoreCase = true) -> "R&B & Soul"
                genre.contains("Hip", ignoreCase = true) || genre.contains("Rap", ignoreCase = true) || genre.contains("Boom", ignoreCase = true) -> "Hip-Hop & Rap"
                genre.contains("Trap", ignoreCase = true) || genre.contains("Drill", ignoreCase = true) || genre.contains("Phonk", ignoreCase = true) -> "Trap & Modern Urban"
                else -> "Urban & Pop"
            }
            recentSubGenreRepository.recordSubGenreSelected(genre, resolvedCategory)
        }
    }

    fun recordSubGenreSelection(name: String, category: String = "", tempoRange: String = "") {
        _studioState.update { it.copy(selectedGenre = name) }
        triggerDebouncedAutoSave()
        viewModelScope.launch {
            val resolvedCategory = if (category.isNotBlank()) category else "Urban & Pop"
            recentSubGenreRepository.recordSubGenreSelected(name, resolvedCategory, tempoRange)
        }
    }

    fun clearRecentSubGenres() {
        viewModelScope.launch {
            recentSubGenreRepository.clearRecentSubGenres()
        }
    }

    fun setMood(mood: String) {
        _studioState.update { it.copy(selectedMood = mood) }
        triggerDebouncedAutoSave()
    }

    fun setLanguage(language: String) {
        _studioState.update { it.copy(selectedLanguage = language) }
        triggerDebouncedAutoSave()
    }

    fun setUserTopic(topic: String) {
        _studioState.update { it.copy(userTopic = topic) }
        triggerDebouncedAutoSave()
    }

    fun setMoodKeywords(keywords: String) {
        _studioState.update { it.copy(moodKeywords = keywords) }
        triggerDebouncedAutoSave()
    }

    fun setVocalStyle(style: String) {
        _studioState.update { it.copy(vocalStyle = style) }
        triggerDebouncedAutoSave()
    }

    fun setSelectedBpm(bpm: String) {
        _studioState.update { it.copy(selectedBpm = bpm) }
        triggerDebouncedAutoSave()
    }

    fun setVocalReverb(reverb: String) {
        _studioState.update { it.copy(vocalReverb = reverb) }
        triggerDebouncedAutoSave()
    }

    fun generateLyricsOnly() {
        viewModelScope.launch {
            val state = _studioState.value
            val provider = com.example.network.llm.LlmProviderFactory.create(_llmProviderId.value, getApplication())
            if (!provider.isAvailable()) {
                _studioState.update {
                    it.copy(
                        isGenerating = false,
                        statusMessage = "${provider.displayName} is not configured. Open CONFIG to select a provider."
                    )
                }
                return@launch
            }
            _studioState.update {
                it.copy(
                    isGenerating = true,
                    statusMessage = "Drafting structured lyrics with ${provider.displayName}..."
                )
            }
            try {
                val draft = MusicEngine.generateSongDraft(
                    genre = state.selectedGenre,
                    mood = state.selectedMood,
                    language = state.selectedLanguage,
                    userTopic = state.userTopic,
                    moodKeywords = state.moodKeywords,
                    providerId = _llmProviderId.value
                ) ?: throw Exception("Failed to generate draft")

                val coverPath = CoverArtGenerator.generateCoverArt(
                    context = getApplication(),
                    title = draft.title,
                    lyrics = draft.lyrics,
                    genre = draft.genre,
                    mood = draft.mood
                )

                val completedDraft = draft.copy(coverImagePath = coverPath)

                _studioState.update {
                    it.copy(
                        isGenerating = false,
                        currentDraft = completedDraft,
                        statusMessage = "Lyrics Drafted // ${completedDraft.title}"
                    )
                }
                recordVersionHistory(completedDraft)
                triggerDebouncedAutoSave()
                analyzeSentiment()
            } catch (e: Exception) {
                _studioState.update {
                    it.copy(
                        isGenerating = false,
                        statusMessage = "Lyrics drafting failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun generateMusicPromptOnly() {
        val draft = _studioState.value.currentDraft ?: return
        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    isPolishingPrompt = true,
                    statusMessage = "Creating text production prompt..."
                )
            }
            try {
                val polished = MusicEngine.autoPolishPrompt(
                    rawPrompt = draft.promptText,
                    genre = _studioState.value.selectedGenre,
                    mood = _studioState.value.selectedMood,
                    vocalStyle = _studioState.value.vocalStyle,
                    bpm = _studioState.value.selectedBpm,
                    providerId = _llmProviderId.value
                )
                val updatedDraft = draft.copy(promptText = polished)
                _studioState.update {
                    it.copy(
                        isPolishingPrompt = false,
                        currentDraft = updatedDraft,
                        statusMessage = "Music Prompt Generated (Text Only)"
                    )
                }
                triggerDebouncedAutoSave()
            } catch (e: Exception) {
                _studioState.update {
                    it.copy(
                        isPolishingPrompt = false,
                        statusMessage = "Prompt created"
                    )
                }
            }
        }
    }

    fun generateCoverArtOnly() {
        val draft = _studioState.value.currentDraft ?: return
        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    isGeneratingCover = true,
                    statusMessage = "Creating monochromatic cover art..."
                )
            }
            try {
                val coverPath = CoverArtGenerator.generateCoverArt(
                    context = getApplication(),
                    title = draft.title,
                    lyrics = draft.lyrics,
                    genre = draft.genre,
                    mood = draft.mood
                )
                _studioState.update {
                    it.copy(
                        isGeneratingCover = false,
                        currentDraft = draft.copy(coverImagePath = coverPath),
                        statusMessage = "Cover Art Generated // Nothing Monochrome"
                    )
                }
                triggerDebouncedAutoSave()
            } catch (e: Exception) {
                _studioState.update {
                    it.copy(
                        isGeneratingCover = false,
                        statusMessage = "Cover art generation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun importLocalAudio(context: Context, uri: Uri) {
        audioPlayerManager.stopAudio()
        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    isImportingAudio = true,
                    statusMessage = "Reading local audio file..."
                )
            }
            val result = LocalAudioImporter.importAudio(context, uri)
            result.onSuccess { metadata ->
                val withinCloudLimit = LocalAudioImporter.isWithinTranscriptionLimit(metadata.localFile)
                val canTranscribe = withinCloudLimit && _allowCloudTranscription.value
                _studioState.update {
                    it.copy(
                        isTranscribingAudio = canTranscribe,
                        statusMessage = when {
                            !withinCloudLimit -> "Audio imported // File is over the 14 MB cloud transcription limit"
                            !_allowCloudTranscription.value -> "Audio imported locally // cloud transcription is off; no audio uploaded"
                            else -> "Audio imported // Transcribing with Gemini cloud AI..."
                        }
                    )
                }
                val transcribed = if (canTranscribe) {
                    MusicEngine.transcribeLyricsFromAudio(metadata, allowCloudUpload = true)
                } else null
                val draft = transcribed ?: fallbackDraftForImportedAudio(metadata)

                val songEntity = Song(
                    title = draft.title,
                    genre = draft.genre,
                    mood = draft.mood,
                    language = draft.language,
                    promptText = draft.promptText,
                    tags = draft.tags.joinToString(", "),
                    lyrics = draft.lyrics,
                    coverSeed = "local_${System.currentTimeMillis()}",
                    coverConcept = draft.coverConcept,
                    coverImagePath = null,
                    audioUrl = metadata.localFile.absolutePath,
                    isLocal = true
                )
                repository.insertSong(songEntity)

                val status = when {
                    transcribed != null -> "Local track imported // Lyrics transcribed via Gemini cloud"
                    !withinCloudLimit -> "Local track imported // Cloud transcription skipped (file over 14 MB)"
                    !_allowCloudTranscription.value -> "Local track imported // Kept on device; cloud transcription is off"
                    else -> "Local track imported // Cloud AI transcription unavailable — retry from SYNC"
                }
                _studioState.update {
                    it.copy(
                        isImportingAudio = false,
                        isTranscribingAudio = false,
                        isLocalAudioActive = true,
                        localAudioMetadata = metadata,
                        currentDraft = draft,
                        statusMessage = status
                    )
                }

                if (transcribed != null) {
                    syncLyricsWithAudio(context)
                }
            }.onFailure { err ->
                _studioState.update {
                    it.copy(
                        isImportingAudio = false,
                        statusMessage = "Import failed: ${err.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    private fun fallbackDraftForImportedAudio(metadata: ImportedAudioMetadata): GeneratedSongDraft =
        GeneratedSongDraft(
            title = metadata.title.ifBlank { "Local Audio Track" },
            genre = "Imported",
            mood = "Unclassified",
            language = "Auto-Detected",
            promptText = "",
            tags = listOf("imported", "local"),
            lyrics = "",
            sections = emptyList(),
            coverConcept = "Nothing OS monochrome cover for an imported local recording"
        )

    fun transcribeImportedAudio(context: Context, replaceExistingLyrics: Boolean = false) {
        val state = _studioState.value
        val metadata = state.localAudioMetadata
        if (metadata == null || !metadata.localFile.exists()) {
            _studioState.update { it.copy(statusMessage = "Import a local audio file before transcription") }
            return
        }
        if (!_allowCloudTranscription.value) {
            _studioState.update {
                it.copy(statusMessage = "Cloud transcription is off. This build does not yet include a local Whisper model; no audio was uploaded.")
            }
            return
        }
        if (!LocalAudioImporter.isWithinTranscriptionLimit(metadata.localFile)) {
            _studioState.update {
                it.copy(statusMessage = "AI transcription supports audio files up to 14 MB. The imported audio is kept locally.")
            }
            return
        }
        if (!replaceExistingLyrics && !state.currentDraft?.lyrics.isNullOrBlank()) {
            _studioState.update {
                it.copy(statusMessage = "Lyrics already exist. Confirm replacement before transcribing this track.")
            }
            return
        }
        if (state.isTranscribingAudio) return

        viewModelScope.launch {
            _studioState.update {
                it.copy(isTranscribingAudio = true, statusMessage = "Transcribing imported vocals with AI...")
            }
            try {
                val transcribed = MusicEngine.transcribeLyricsFromAudio(metadata, allowCloudUpload = true)
                    ?: throw IllegalStateException("No transcription was returned. Check the Gemini API key and network connection.")
                val updatedDraft = transcribed.copy(coverImagePath = state.currentDraft?.coverImagePath)
                _studioState.update {
                    it.copy(
                        isTranscribingAudio = false,
                        currentDraft = updatedDraft,
                        statusMessage = "Lyrics transcribed // Creating automatic word timing..."
                    )
                }
                recordVersionHistory(updatedDraft)
                triggerDebouncedAutoSave()
                syncLyricsWithAudio(context)
            } catch (e: Exception) {
                _studioState.update {
                    it.copy(
                        isTranscribingAudio = false,
                        statusMessage = "Transcription failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun syncLyricsWithAudio(context: Context) {
        val metadata = _studioState.value.localAudioMetadata
        val lyrics = _studioState.value.currentDraft?.lyrics

        if (lyrics.isNullOrBlank()) {
            _studioState.update { it.copy(statusMessage = "Add lyrics before aligning audio") }
            return
        }
        if (metadata == null || !metadata.localFile.exists()) {
            _studioState.update { it.copy(statusMessage = "Import a local audio file first") }
            return
        }

        val audioUri = Uri.fromFile(metadata.localFile)

        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    isAligningAudio = true,
                    statusMessage = "Creating automatic word timing from local audio duration..."
                )
            }
            val result = LocalWhisperEngine.alignLyricsWithAudio(context, audioUri, lyrics)
            result.onSuccess { karaoke ->
                _studioState.update {
                    it.copy(
                        isAligningAudio = false,
                        karaokeLyrics = karaoke,
                        statusMessage = "Estimated word timings created // ${karaoke.wordTimings.size} words — review and adjust"
                    )
                }
            }.onFailure { err ->
                _studioState.update {
                    it.copy(
                        isAligningAudio = false,
                        statusMessage = "Alignment note: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun exportLyricVideo(
        context: Context,
        resolution: VideoResolution = VideoResolution.RES_1080P,
        aspectRatio: VideoAspectRatio = VideoAspectRatio.PORTRAIT_9_16,
        bitrateMbps: Int = 12,
        fps: Int = 30
    ) {
        val draft = _studioState.value.currentDraft ?: return
        val karaoke = _studioState.value.karaokeLyrics
        val metadata = _studioState.value.localAudioMetadata

        val audioUri = metadata?.let { Uri.fromFile(it.localFile) }
        val wordTimings = karaoke?.wordTimings ?: emptyList()

        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    exportProgress = ExportProgressState(
                        isExporting = true,
                        progress = 0.05f,
                        statusText = "Starting hardware video encoding..."
                    )
                )
            }

            val result = LyricVideoExporter.exportLyricVideo(
                context = context,
                songTitle = draft.title,
                lyrics = draft.lyrics,
                wordTimings = wordTimings,
                coverImagePath = draft.coverImagePath,
                audioUri = audioUri,
                resolution = resolution,
                aspectRatio = aspectRatio,
                bitrateMbps = bitrateMbps,
                fps = fps,
                onProgress = { progressRatio, statusText ->
                    _studioState.update { state ->
                        state.copy(
                            exportProgress = state.exportProgress.copy(
                                progress = progressRatio,
                                statusText = statusText
                            )
                        )
                    }
                }
            )

            result.onSuccess { file ->
                _studioState.update {
                    it.copy(
                        exportProgress = ExportProgressState(
                            isExporting = false,
                            progress = 1.0f,
                            statusText = "Export Complete!",
                            outputFilePath = file.absolutePath
                        ),
                        statusMessage = "Lyric Video Exported // ${file.name}"
                    )
                }
            }.onFailure { err ->
                _studioState.update {
                    it.copy(
                        exportProgress = ExportProgressState(
                            isExporting = false,
                            progress = 0f,
                            error = err.localizedMessage ?: "Export failed"
                        ),
                        statusMessage = "Video export failed"
                    )
                }
            }
        }
    }

    fun playSong(song: Song) {
        val url = song.audioUrl
        if (url.isNullOrBlank()) {
            _studioState.update { it.copy(statusMessage = "No audio attached to ${song.title}") }
            return
        }
        audioPlayerManager.playTrack(
            url = url,
            title = song.title,
            artist = "${song.genre} // ${song.mood}",
            lyrics = song.lyrics,
            coverPath = song.coverImagePath
        )
    }

    fun loadSongIntoStudio(song: Song) {
        audioPlayerManager.stopAudio()
        val draft = GeneratedSongDraft(
            title = song.title,
            genre = song.genre,
            mood = song.mood,
            language = song.language,
            promptText = song.promptText,
            tags = song.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
            lyrics = song.lyrics,
            sections = emptyList(),
            coverConcept = song.coverConcept,
            coverImagePath = song.coverImagePath
        )

        _studioState.update {
            it.copy(
                selectedGenre = song.genre,
                selectedMood = song.mood,
                selectedLanguage = song.language,
                currentDraft = draft,
                statusMessage = "Loaded into Studio // ${song.title}"
            )
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.updateSong(song.copy(isFavorite = !song.isFavorite))
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            repository.deleteSong(song)
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch {
            repository.updateSong(song)
        }
    }

    fun bulkUpdateSongs(songIds: List<Long>, tags: String) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            // Query the database directly: the savedSongs StateFlow can be empty
            // when the library screen is not actively collecting it.
            val songsToUpdate = repository.getSongsByIds(songIds)
            songsToUpdate.forEach { song ->
                repository.updateSong(song.copy(tags = tags))
            }
            _studioState.update {
                it.copy(statusMessage = "Tags updated for ${songsToUpdate.size} track(s)")
            }
        }
    }

    fun regenerateCoverArt(song: Song) {
        viewModelScope.launch {
            _studioState.update { it.copy(statusMessage = "Generating new cover art for ${song.title}...") }
            try {
                val newCover = CoverArtGenerator.generateCoverArt(
                    context = getApplication(),
                    title = song.title,
                    lyrics = song.lyrics,
                    genre = song.genre,
                    mood = song.mood
                )
                val updated = song.copy(coverImagePath = newCover)
                repository.updateSong(updated)
                _studioState.update { it.copy(statusMessage = "New cover art saved // ${song.title}") }
            } catch (e: Exception) {
                _studioState.update { it.copy(statusMessage = "Cover art generation note") }
            }
        }
    }

    fun insertLyricSection(sectionTag: String) {
        val state = _studioState.value
        val draft = state.currentDraft ?: return
        val currentLyrics = draft.lyrics
        val tagToInsert = if (sectionTag.startsWith("[") && sectionTag.endsWith("]")) sectionTag else "[$sectionTag]"

        val updatedLyrics = if (tagToInsert.equals("[Intro]", ignoreCase = true)) {
            if (currentLyrics.contains("[Intro", ignoreCase = true)) {
                _studioState.update { it.copy(statusMessage = "Song already contains [Intro]") }
                return
            }
            "$tagToInsert\n(تقاسيم نغمية تمهد للأغنية)\n\n$currentLyrics"
        } else {
            val trimmed = currentLyrics.trimEnd()
            if (trimmed.isBlank()) "$tagToInsert\n" else "$trimmed\n\n$tagToInsert\n"
        }

        val updatedDraft = draft.copy(
            lyrics = updatedLyrics,
            sections = MusicEngine.parseLyricsIntoSections(updatedLyrics)
        )
        updateCurrentDraft(updatedDraft)
        _studioState.update { it.copy(statusMessage = "Inserted $tagToInsert section tag") }
    }

    fun recordVersionHistory(draft: GeneratedSongDraft) {
        val state = _studioState.value
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val timeString = timeFormat.format(java.util.Date())
        val version = GenerationVersion(
            timestamp = timeString,
            title = draft.title,
            genre = draft.genre,
            mood = draft.mood,
            language = draft.language,
            vocalStyle = state.vocalStyle,
            bpm = state.selectedBpm,
            promptText = draft.promptText,
            lyrics = draft.lyrics,
            tags = draft.tags,
            coverImagePath = draft.coverImagePath
        )
        _studioState.update { current ->
            val updatedHistory = (listOf(version) + current.versionHistory.filter { it.title != version.title || it.timestamp != version.timestamp }).take(5)
            current.copy(versionHistory = updatedHistory)
        }
    }

    fun updateCurrentDraft(draft: GeneratedSongDraft) {
        _studioState.update { it.copy(currentDraft = draft) }
        triggerDebouncedAutoSave()
    }

    fun updateLyrics(lyrics: String) {
        _studioState.update { state ->
            val sections = MusicEngine.parseLyricsIntoSections(lyrics)
            val updated = state.currentDraft?.copy(lyrics = lyrics, sections = sections)
                ?: GeneratedSongDraft(
                    title = "Untitled Draft",
                    genre = state.selectedGenre,
                    mood = state.selectedMood,
                    language = state.selectedLanguage,
                    promptText = "",
                    tags = emptyList(),
                    lyrics = lyrics,
                    sections = sections,
                    coverConcept = ""
                )
            state.copy(currentDraft = updated)
        }
        triggerDebouncedAutoSave()
    }

    fun triggerDebouncedAutoSave() {
        val draft = _studioState.value.currentDraft ?: return
        if (draft.title.isBlank() && draft.lyrics.isBlank()) return

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000L)
            _studioState.update { it.copy(isAutoSaving = true) }
            try {
                val currentId = _studioState.value.activeDraftId ?: 0L
                val now = System.currentTimeMillis()
                val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                val timeString = timeFormat.format(java.util.Date(now))

                val entity = LyricDraft(
                    id = currentId,
                    title = draft.title.ifBlank { "Untitled Draft" },
                    genre = _studioState.value.selectedGenre,
                    mood = _studioState.value.selectedMood,
                    language = _studioState.value.selectedLanguage,
                    userTopic = _studioState.value.userTopic,
                    lyrics = draft.lyrics,
                    promptText = draft.promptText,
                    tags = draft.tags.joinToString(", "),
                    coverConcept = draft.coverConcept,
                    coverImagePath = draft.coverImagePath,
                    lastModified = now
                )
                val savedId = draftsRepository.saveDraft(entity)
                val finalId = if (currentId != 0L) currentId else savedId
                _studioState.update {
                    it.copy(
                        isAutoSaving = false,
                        lastAutoSavedTime = timeString,
                        activeDraftId = finalId
                    )
                }
            } catch (e: Exception) {
                _studioState.update { it.copy(isAutoSaving = false) }
            }
        }
    }

    fun restoreDraft(draft: LyricDraft) {
        audioPlayerManager.stopAudio()
        val songDraft = GeneratedSongDraft(
            title = draft.title,
            genre = draft.genre,
            mood = draft.mood,
            language = draft.language,
            promptText = draft.promptText,
            tags = draft.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
            lyrics = draft.lyrics,
            sections = emptyList(),
            coverConcept = draft.coverConcept,
            coverImagePath = draft.coverImagePath
        )
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val formattedTime = timeFormat.format(java.util.Date(draft.lastModified))

        _studioState.update {
            it.copy(
                selectedGenre = draft.genre,
                selectedMood = draft.mood,
                selectedLanguage = draft.language,
                userTopic = draft.userTopic,
                currentDraft = songDraft,
                activeDraftId = draft.id,
                lastAutoSavedTime = formattedTime,
                statusMessage = "Restored draft // ${draft.title}"
            )
        }
        analyzeSentiment()
    }

    fun deleteDraft(draft: LyricDraft) {
        viewModelScope.launch {
            draftsRepository.deleteDraft(draft)
            if (_studioState.value.activeDraftId == draft.id) {
                _studioState.update { it.copy(activeDraftId = null) }
            }
        }
    }

    fun clearAllDrafts() {
        viewModelScope.launch {
            draftsRepository.clearAllDrafts()
            _studioState.update { it.copy(activeDraftId = null) }
        }
    }

    fun analyzeSentiment() {
        val draft = _studioState.value.currentDraft ?: return
        viewModelScope.launch {
            _studioState.update { it.copy(isAnalyzingSentiment = true, statusMessage = "Analyzing emotional sentiment...") }
            val result = SentimentAnalyzer.analyzeLyrics(draft.lyrics)
            _studioState.update {
                it.copy(
                    sentimentResult = result,
                    isAnalyzingSentiment = false,
                    statusMessage = "Sentiment: ${result.dominantTone}"
                )
            }
        }
    }

    fun savePreset(name: String) {
        val state = _studioState.value
        viewModelScope.launch {
            _studioState.update { it.copy(isSavingPreset = true) }
            val preset = Preset(
                name = name,
                genre = state.selectedGenre,
                mood = state.selectedMood,
                language = state.selectedLanguage,
                vocalStyle = state.vocalStyle,
                bpm = state.selectedBpm,
                reverb = state.vocalReverb
            )
            presetRepository.savePreset(preset)
            _studioState.update {
                it.copy(
                    isSavingPreset = false,
                    statusMessage = "Preset saved // $name"
                )
            }
        }
    }

    fun loadPreset(preset: Preset) {
        _studioState.update {
            it.copy(
                selectedGenre = preset.genre,
                selectedMood = preset.mood,
                selectedLanguage = preset.language,
                vocalStyle = preset.vocalStyle,
                selectedBpm = preset.bpm,
                vocalReverb = preset.reverb,
                statusMessage = "Loaded preset // ${preset.name}"
            )
        }
        triggerDebouncedAutoSave()
    }

    override fun onCleared() {
            super.onCleared()
            audioPlayerManager.stopAudio()
        }

    // Local model download methods
    val localModelManager = com.example.domain.LocalModelManager(getApplication())

    fun downloadModel(modelId: String) {
        val model = com.example.domain.LocalModelManager.AVAILABLE_MODELS.firstOrNull { it.id == modelId } ?: return
        viewModelScope.launch {
            localModelManager.downloadModel(model)
        }
    }

    fun cancelDownload(modelId: String) {
        val model = com.example.domain.LocalModelManager.AVAILABLE_MODELS.firstOrNull { it.id == modelId } ?: return
        localModelManager.cancelDownload(model)
    }

    fun deleteModel(modelId: String) {
        val model = com.example.domain.LocalModelManager.AVAILABLE_MODELS.firstOrNull { it.id == modelId } ?: return
        localModelManager.deleteModel(model)
    }
}

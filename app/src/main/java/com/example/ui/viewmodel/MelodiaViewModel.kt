package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.GenerationQueueItem
import com.example.data.model.GenerationStatus
import com.example.data.model.GenerationType
import com.example.data.model.LyricDraft
import com.example.data.model.Preset
import com.example.data.model.RecentSubGenre
import com.example.data.model.Song
import com.example.data.repository.DraftsRepository
import com.example.data.repository.GenerationQueueRepository
import com.example.data.repository.PresetRepository
import com.example.data.repository.RecentSubGenreRepository
import com.example.data.repository.SongRepository
import com.example.domain.AudioPlayerManager
import com.example.domain.CoverArtGenerator
import com.example.domain.ExportProgressState
import com.example.domain.ImportedAudioMetadata
import com.example.domain.LocalAudioImporter
import com.example.domain.MusicEngine
import com.example.domain.SentimentAnalyzer
import com.example.domain.SentimentResult
import com.example.domain.LocalModelManager
import com.example.network.llm.LlmProviderFactory
import com.example.network.GeneratedSongDraft
import com.example.network.TrebloGenerationRequest
import com.example.network.TrebloGenerationResponse
import com.example.ui.state.StudioUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MelodiaViewModel(
    application: Application,
    private val draftsRepository: DraftsRepository,
    private val presetRepository: PresetRepository,
    private val songRepository: SongRepository,
    private val recentSubGenreRepository: RecentSubGenreRepository,
    private val generationQueueRepository: GenerationQueueRepository,
    private val audioPlayerManager: AudioPlayerManager,
    private val localAudioImporter: LocalAudioImporter
) : AndroidViewModel(application) {

    // LLM Provider state
    private val _llmProviderId = MutableStateFlow(LlmProviderFactory.PROVIDER_GEMINI_NANO)
    val llmProviderId: StateFlow<String> = _llmProviderId

    // Export settings
    private val _defaultExportFormat = MutableStateFlow("MP3")
    val defaultExportFormat: StateFlow<String> = _defaultExportFormat

    private val _allowCloudTranscription = MutableStateFlow(false)
    val allowCloudTranscription: StateFlow<Boolean> = _allowCloudTranscription

    // Local model state
    val availableModels = LocalModelManager.AVAILABLE_MODELS
    val localModelManager = LocalModelManager(application)

    // Studio state
    private val _studioState = MutableStateFlow(StudioUiState())
    val studioState: StateFlow<StudioUiState> = _studioState

    // Drafts
    val allDrafts = draftsRepository.allDrafts
        .map { it.map { draft ->
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(draft.lastModified))
            draft.copy(
                title = draft.title,
                genre = draft.genre,
                mood = draft.mood,
                language = draft.language,
                userTopic = draft.userTopic,
                lyrics = draft.lyrics,
                promptText = draft.promptText,
                tags = draft.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                coverConcept = draft.coverConcept,
                coverImagePath = draft.coverImagePath,
                lastModified = draft.lastModified
            )
        } }

    // Presets
    val allPresets = presetRepository.allPresets

    // Recent sub-genres
    val recentSubGenres = recentSubGenreRepository.recentSubGenres

    // Songs
    val allSongs = songRepository.allSongs

    // Saved drafts and songs for Vault/Queue screens
    val savedDrafts = draftsRepository.allDrafts
    val savedSongs = songRepository.allSongs
    val savedPresets = presetRepository.allPresets

    // Generation queue
    val activeQueueItems = generationQueueRepository.activeQueueItems
    val completedQueueItems = generationQueueRepository.completedQueueItems
    val failedQueueItems = generationQueueRepository.failedQueueItems

    // App version
    private val _appVersion = MutableStateFlow("1.0.0")
    val appVersion: StateFlow<String> = _appVersion

    // Cache size
    private val _cacheSizeFormatted = MutableStateFlow("0 B")
    val cacheSizeFormatted: StateFlow<String> = _cacheSizeFormatted

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Queue processor job
    private var queueProcessorJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        loadAppVersion()
        loadCacheSize()
        startQueueProcessor()
    }

    private fun loadAppVersion() {
        viewModelScope.launch {
            try {
                val packageInfo = getApplication().packageManager.getPackageInfo(getApplication().packageName, 0)
                _appVersion.value = packageInfo.versionName ?: "1.0.0"
            } catch (e: Exception) {
                _appVersion.value = "1.0.0"
            }
        }
    }

    private fun loadCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val size = getApplication().cacheDir.getCacheDirSize()
            _cacheSizeFormatted.value = formatFileSize(size)
        }
    }

    private fun File.getCacheDirSize(): Long {
        if (!exists()) return 0
        if (isFile) return length()
        return listFiles()?.sumOf { it.getCacheDirSize() } ?: 0
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication().cacheDir.deleteRecursively()
            loadCacheSize()
        }
    }

    fun setLlmProvider(providerId: String) {
        _llmProviderId.value = providerId
    }

    // Genre, Mood, Language, Vocal Style setters
    fun setSelectedGenre(genre: String) {
        _studioState.update { it.copy(selectedGenre = genre) }
        triggerDebouncedAutoSave()
    }

    fun setSelectedMood(mood: String) {
        _studioState.update { it.copy(selectedMood = mood) }
        triggerDebouncedAutoSave()
    }

    fun setSelectedLanguage(language: String) {
        _studioState.update { it.copy(selectedLanguage = language) }
        triggerDebouncedAutoSave()
    }

    fun setUserTopic(topic: String) {
        _studioState.update { it.copy(userTopic = topic) }
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

    fun setCoverConcept(concept: String) {
        _studioState.update { it.copy(currentDraft = it.currentDraft?.copy(coverConcept = concept)) }
        triggerDebouncedAutoSave()
    }

    fun setCoverImagePath(path: String?) {
        _studioState.update { it.copy(currentDraft = it.currentDraft?.copy(coverImagePath = path)) }
        triggerDebouncedAutoSave()
    }

    // Export settings
    fun updateDefaultExportFormat(format: String) {
        _defaultExportFormat.value = format
    }

    fun updateAllowCloudTranscription(allow: Boolean) {
        _allowCloudTranscription.value = allow
    }

    // Aliases for StudioScreen
    fun setGenre(genre: String) = setSelectedGenre(genre)
    fun setLanguage(language: String) = setSelectedLanguage(language)
    fun setMoodKeywords(keywords: String) = setUserTopic(keywords)

    // Generation functions
    fun generateLyricsOnly() = generateDraft()
    fun generateMusicPromptOnly() = generateDraft()

    // Vault screen functions
    fun loadSongIntoStudio(song: Song) {
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
        updateCurrentDraft(draft)
        _studioState.update { it.copy(
            selectedGenre = song.genre,
            selectedMood = song.mood,
            selectedLanguage = song.language,
            userTopic = song.promptText,
            coverConcept = song.coverConcept
        )
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.updateSong(song.copy(isFavorite = !song.isFavorite))
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.deleteSong(song)
        }
    }

    fun regenerateCoverArt(song: Song) {
        val current = _studioState.value.currentDraft ?: return
        val draft = current.copy(coverConcept = song.coverConcept)
        updateCurrentDraft(draft)
        generateCoverArt()
    }

    fun playSong(song: Song) {
        audioPlayerManager.playAudio(song.audioUrl ?: "")
    }

    fun generateDraft() {
        val state = _studioState.value
        if (state.userTopic.isBlank() && state.selectedGenre.isBlank()) {
            _studioState.update { it.copy(statusMessage = "Please enter a topic or select a genre") }
            return
        }

        _studioState.update { it.copy(isGenerating = true, statusMessage = "Drafting lyrics...") }

        viewModelScope.launch {
            try {
                val result = MusicEngine.generateDraft(
                    topic = state.userTopic,
                    genre = state.selectedGenre,
                    mood = state.selectedMood,
                    language = state.selectedLanguage,
                    llmProviderId = _llmProviderId.value
                ) ?: throw Exception("Failed to generate draft")

                val sections = MusicEngine.parseLyricsIntoSections(result)
                val draft = GeneratedSongDraft(
                    title = "Untitled Draft",
                    genre = state.selectedGenre,
                    mood = state.selectedMood,
                    language = state.selectedLanguage,
                    promptText = state.userTopic,
                    tags = emptyList(),
                    lyrics = result,
                    sections = sections,
                    coverConcept = ""
                )

                _studioState.update { it.copy(currentDraft = draft, isGenerating = false, statusMessage = "Draft generated") }
                triggerDebouncedAutoSave()
            } catch (e: Exception) {
                _studioState.update { it.copy(isGenerating = false, statusMessage = "Draft generation failed: ${e.localizedMessage}") }
            }
        }
    }

    fun generateCoverArt() {
        val state = _studioState.value
        val draft = state.currentDraft ?: return
        val title = draft.title.ifBlank { "Untitled" }
        val genre = state.selectedGenre

        _studioState.update { it.copy(isGeneratingCover = true, statusMessage = "Generating cover art...") }

        viewModelScope.launch {
            try {
                val coverPath = CoverArtGenerator.generateCoverArt(
                    context = getApplication(),
                    title = title,
                    lyrics = draft.lyrics,
                    genre = genre,
                    mood = state.selectedMood
                )

                if (coverPath != null) {
                    _studioState.update {
                        it.copy(
                            currentDraft = it.currentDraft?.copy(coverImagePath = coverPath),
                            isGeneratingCover = false,
                            statusMessage = "Cover art generated"
                        )
                    }
                    triggerDebouncedAutoSave()
                } else {
                    throw Exception("Failed to generate cover art")
                }
            } catch (e: Exception) {
                _studioState.update { it.copy(isGeneratingCover = false, statusMessage = "Cover art generation failed: ${e.localizedMessage}") }
            }
        }
    }

    fun generateMusic() {
        val state = _studioState.value
        val draft = state.currentDraft ?: return

        _studioState.update { it.copy(isGenerating = true, statusMessage = "Generating music...") }

        viewModelScope.launch {
            try {
                val request = TrebloGenerationRequest(
                    prompt = draft.promptText,
                    tags = draft.tags,
                    lyrics = draft.lyrics.ifBlank { null },
                    instrumental = false,
                    title = draft.title.ifBlank { "Untitled" }
                )

                val response = MusicEngine.generateMusic(request)

                if (response != null && response.audioUrl != null) {
                    val audioPath = MusicEngine.downloadAudio(response.audioUrl!!)

                    _studioState.update { it.copy(isGenerating = false, statusMessage = "Music generated") }
                    // Save as completed song
                    saveCompletedSong(audioPath)
                } else {
                    throw Exception("Failed to generate music")
                }
            } catch (e: Exception) {
                _studioState.update { it.copy(isGenerating = false, statusMessage = "Music generation failed: ${e.localizedMessage}") }
            }
        }
    }

    fun generateFullSong() {
        val state = _studioState.value
        val draft = state.currentDraft ?: return

        _studioState.update { it.copy(isGenerating = true, isGeneratingCover = true, statusMessage = "Generating full song...") }

        viewModelScope.launch {
            try {
                // Generate lyrics if needed
                var lyrics = draft.lyrics
                if (lyrics.isBlank()) {
                    val result = MusicEngine.generateLyricsOnly(
                        topic = state.userTopic,
                        genre = state.selectedGenre,
                        mood = state.selectedMood,
                        language = state.selectedLanguage,
                        llmProviderId = _llmProviderId.value
                    )
                    if (result != null) {
                        lyrics = result
                        _studioState.update { it.copy(currentDraft = it.currentDraft?.copy(lyrics = lyrics)) }
                    } else {
                        throw Exception("Failed to generate lyrics")
                    }
                }

                // Generate music
                val request = TrebloGenerationRequest(
                    prompt = draft.promptText,
                    tags = draft.tags,
                    lyrics = lyrics.ifBlank { null },
                    instrumental = false,
                    title = draft.title.ifBlank { "Untitled" }
                )

                val response = MusicEngine.generateMusic(request)

                if (response != null && response.audioUrl != null) {
                    val audioPath = MusicEngine.downloadAudio(response.audioUrl!!)

                    // Generate cover art
                    val coverPath = CoverArtGenerator.generateCoverArt(
                        context = getApplication(),
                        title = draft.title.ifBlank { "Untitled" },
                        lyrics = draft.lyrics,
                        genre = state.selectedGenre,
                        mood = state.selectedMood
                    )

                    _studioState.update {
                        it.copy(
                            currentDraft = it.currentDraft?.copy(coverImagePath = coverPath),
                            isGenerating = false,
                            isGeneratingCover = false,
                            statusMessage = "Full song generated"
                        )
                    }
                    saveCompletedSong(audioPath, coverPath)
                    triggerDebouncedAutoSave()
                } else {
                    throw Exception("Failed to generate music")
                }
            } catch (e: Exception) {
                _studioState.update { it.copy(isGenerating = false, isGeneratingCover = false, statusMessage = "Full song generation failed: ${e.localizedMessage}") }
            }
        }
    }

    private fun saveCompletedSong(audioPath: String, coverPath: String? = null) {
        val state = _studioState.value
        val draft = state.currentDraft ?: return

        val song = Song(
            title = draft.title,
            genre = state.selectedGenre,
            mood = state.selectedMood,
            language = state.selectedLanguage,
            promptText = draft.promptText,
            lyrics = draft.lyrics,
            tags = draft.tags.joinToString(", "),
            coverSeed = draft.coverConcept,
            coverConcept = draft.coverConcept,
            coverImagePath = coverPath ?: draft.coverImagePath,
            audioUrl = audioPath,
            createdAt = System.currentTimeMillis(),
            isLocal = true
        )

        viewModelScope.launch {
            songRepository.insertSong(song)
            _studioState.update { it.copy(statusMessage = "Song saved to library") }
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
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val timeString = timeFormat.format(Date(now))

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
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(draft.lastModified))

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
        queueProcessorJob?.cancel()
        autoSaveJob?.cancel()
    }

    private fun startQueueProcessor() {
        queueProcessorJob = viewModelScope.launch {
            while (true) {
                val activeCount = generationQueueRepository.getActiveCount()
                if (activeCount > 0) {
                    val items = generationQueueRepository.activeQueueItems.first()
                    val pendingItem = items.find { it.status == GenerationStatus.PENDING }
                    if (pendingItem != null) {
                        processQueueItem(pendingItem)
                    } else {
                        // Wait a bit before checking again
                        delay(1000L)
                    }
                } else {
                    // No active items, wait longer
                    delay(2000L)
                }
            }
        }
    }

    private suspend fun processQueueItem(item: GenerationQueueItem) {
        generationQueueRepository.updateStatus(item.id, GenerationStatus.PROCESSING.name, System.currentTimeMillis())

        try {
            when (item.type) {
                GenerationType.LYRICS -> {
                    // Trigger lyrics generation
                    _studioState.update { it.copy(isGenerating = true, statusMessage = "Generating lyrics...") }

                    val result = MusicEngine.generateLyricsOnly(
                        topic = item.userTopic,
                        genre = item.genre,
                        mood = item.mood,
                        language = item.language,
                        llmProviderId = _llmProviderId.value
                    )

                    if (result != null) {
                        generationQueueRepository.completeItem(
                            item.id,
                            GenerationStatus.COMPLETED.name,
                            System.currentTimeMillis(),
                            null,
                            null,
                            null
                        )
                        _studioState.update { it.copy(
                            currentDraft = it.currentDraft?.copy(lyrics = result),
                            statusMessage = "Lyrics generated successfully",
                            isGenerating = false
                        )}
                    } else {
                        throw Exception("Failed to generate lyrics")
                    }
                }
                GenerationType.MUSIC -> {
                    // Trigger music generation
                    _studioState.update { it.copy(isGenerating = true, statusMessage = "Generating music...") }

                    val request = TrebloGenerationRequest(
                        prompt = item.promptText,
                        tags = item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        lyrics = item.lyrics.ifBlank { null },
                        instrumental = item.isInstrumental,
                        title = item.title
                    )

                    val response = MusicEngine.generateMusic(request)

                    if (response != null && response.audioUrl != null) {
                        // Download the audio file
                        val audioPath = MusicEngine.downloadAudio(response.audioUrl!!)
                        generationQueueRepository.completeItem(
                            item.id,
                            GenerationStatus.COMPLETED.name,
                            System.currentTimeMillis(),
                            audioPath,
                            null,
                            null
                        )
                        _studioState.update { it.copy(
                            statusMessage = "Music generated successfully",
                            isGenerating = false
                        )}
                    } else {
                        throw Exception("Failed to generate music")
                    }
                }
                GenerationType.COVER -> {
                    // Trigger cover art generation
                    _studioState.update { it.copy(isGeneratingCover = true, statusMessage = "Generating cover art...") }

                    val coverPath = CoverArtGenerator.generateCoverArt(
                        context = getApplication(),
                        title = item.title,
                        lyrics = item.lyrics,
                        genre = item.genre,
                        mood = item.mood
                    )

                    if (coverPath != null) {
                        generationQueueRepository.completeItem(
                            item.id,
                            GenerationStatus.COMPLETED.name,
                            System.currentTimeMillis(),
                            null,
                            coverPath,
                            null
                        )
                        _studioState.update { it.copy(
                            currentDraft = it.currentDraft?.copy(coverImagePath = coverPath),
                            statusMessage = "Cover art generated successfully",
                            isGeneratingCover = false
                        )}
                    } else {
                        throw Exception("Failed to generate cover art")
                    }
                }
                GenerationType.FULL_SONG -> {
                    // Trigger full song generation (lyrics + music + cover)
                    _studioState.update { it.copy(isGenerating = true, statusMessage = "Generating full song...") }

                    // First generate lyrics if not provided
                    var lyrics = item.lyrics
                    if (lyrics.isBlank()) {
                        val result = MusicEngine.generateLyricsOnly(
                            topic = item.userTopic,
                            genre = item.genre,
                            mood = item.mood,
                            language = item.language,
                            llmProviderId = _llmProviderId.value
                        )
                        if (result != null) {
                            lyrics = result
                        } else {
                            throw Exception("Failed to generate lyrics")
                        }
                    }

                    // Generate music
                    val request = TrebloGenerationRequest(
                        prompt = item.promptText,
                        tags = item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        lyrics = lyrics.ifBlank { null },
                        instrumental = item.isInstrumental,
                        title = item.title
                    )

                    val response = MusicEngine.generateMusic(request)

                    if (response != null && response.audioUrl != null) {
                        val audioPath = MusicEngine.downloadAudio(response.audioUrl!!)

                        // Generate cover art
                        val coverPath = CoverArtGenerator.generateCoverArt(
                            context = getApplication(),
                            title = item.title,
                            lyrics = item.lyrics,
                            genre = item.genre,
                            mood = item.mood
                        )

                        generationQueueRepository.completeItem(
                            item.id,
                            GenerationStatus.COMPLETED.name,
                            System.currentTimeMillis(),
                            audioPath,
                            coverPath,
                            null
                        )
                        _studioState.update { it.copy(
                            statusMessage = "Full song generated successfully",
                            isGenerating = false,
                            isGeneratingCover = false
                        )}
                    } else {
                        throw Exception("Failed to generate music")
                    }
                }
            }
        } catch (e: Exception) {
            generationQueueRepository.completeItem(
                item.id,
                GenerationStatus.FAILED.name,
                System.currentTimeMillis(),
                null,
                null,
                e.message
            )
            _studioState.update { it.copy(
                statusMessage = "Generation failed: ${e.message}",
                isGenerating = false,
                isGeneratingCover = false
            )}
        }
    }

    fun enqueueGeneration(
        type: GenerationType,
        title: String = "",
        genre: String = "",
        mood: String = "",
        language: String = "",
        userTopic: String = "",
        lyrics: String = "",
        promptText: String = "",
        tags: String = "",
        coverConcept: String = "",
        coverImagePath: String? = null,
        vocalStyle: String = "Auto-Tuned Male",
        selectedBpm: String = "120 BPM",
        vocalReverb: String = "Studio Clean",
        isInstrumental: Boolean = false
    ): Long {
        val state = _studioState.value
        val item = GenerationQueueItem(
            type = type,
            title = title.ifBlank { state.currentDraft?.title ?: "Untitled" },
            genre = genre.ifBlank { state.selectedGenre },
            mood = mood.ifBlank { state.selectedMood },
            language = language.ifBlank { state.selectedLanguage },
            userTopic = userTopic.ifBlank { state.userTopic },
            lyrics = lyrics.ifBlank { state.currentDraft?.lyrics ?: "" },
            promptText = promptText.ifBlank { state.currentDraft?.promptText ?: "" },
            tags = tags.ifBlank { state.currentDraft?.tags.joinToString(", ") ?: "" },
            coverConcept = coverConcept.ifBlank { state.currentDraft?.coverConcept ?: "" },
            coverImagePath = coverImagePath ?: state.currentDraft?.coverImagePath,
            vocalStyle = vocalStyle,
            selectedBpm = selectedBpm,
            vocalReverb = vocalReverb,
            isInstrumental = isInstrumental
        )
        return generationQueueRepository.enqueueItem(item)
    }

    fun cancelQueueItem(itemId: Long) {
        viewModelScope.launch {
            val item = generationQueueRepository.getQueueItemById(itemId)
            if (item != null && item.status.isActive) {
                generationQueueRepository.updateQueueItem(item.copy(status = GenerationStatus.CANCELLED))
            }
        }
    }

    fun retryQueueItem(itemId: Long) {
        viewModelScope.launch {
            val item = generationQueueRepository.getQueueItemById(itemId)
            if (item != null && item.status == GenerationStatus.FAILED) {
                generationQueueRepository.updateQueueItem(item.copy(status = GenerationStatus.PENDING, errorMessage = null))
            }
        }
    }

    fun clearCompletedQueueItems() {
        viewModelScope.launch {
            generationQueueRepository.clearCompletedItems()
        }
    }

    // Local model download methods

    fun downloadModel(modelId: String) {
        val model = LocalModelManager.AVAILABLE_MODELS.firstOrNull { it.id == modelId } ?: return
        viewModelScope.launch {
            localModelManager.downloadModel(model)
        }
    }

    fun cancelDownload(modelId: String) {
        val model = LocalModelManager.AVAILABLE_MODELS.firstOrNull { it.id == modelId } ?: return
        localModelManager.cancelDownload(model)
    }

    fun deleteModel(modelId: String) {
        val model = LocalModelManager.AVAILABLE_MODELS.firstOrNull { it.id == modelId } ?: return
        localModelManager.deleteModel(model)
    }
}
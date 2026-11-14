package com.example.ui.state

import com.example.network.GeneratedSongDraft

data class StudioUiState(
    // Configuration
    val selectedGenre: String = "",
    val selectedLanguage: String = "",
    val userTopic: String = "",
    val selectedMood: String = "",
    val vocalStyle: String = "",
    val selectedBpm: String = "120 BPM",
    val vocalReverb: String = "Studio Clean",

    // Generation state
    val isGenerating: Boolean = false,
    val isGeneratingCover: Boolean = false,
    val statusMessage: String = "",
    val generationProgress: Float = 0f,

    // Current draft being edited
    val currentDraft: GeneratedSongDraft? = null,

    // Export settings
    val defaultExportFormat: String = "MP3",
    val allowCloudTranscription: Boolean = false,

    // Cover art
    val coverConcept: String = "",
    val coverImagePath: String? = null,

    // Draft ID for tracking active draft
    val activeDraftId: Long = 0L,

    // Auto-save state
    val isAutoSaving: Boolean = false,
    val lastAutoSavedTime: String = "",

    // Sentiment analysis state
    val isAnalyzingSentiment: Boolean = false,
    val sentimentResult: String = "",

    // Preset saving state
    val isSavingPreset: Boolean = false
)
package com.example.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _currentTitle = MutableStateFlow("Untitled Track")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    private val _currentArtist = MutableStateFlow("Melodia Studio")
    val currentArtist: StateFlow<String> = _currentArtist.asStateFlow()

    private val _currentLyrics = MutableStateFlow("")
    val currentLyrics: StateFlow<String> = _currentLyrics.asStateFlow()

    private val _currentCoverPath = MutableStateFlow<String?>(null)
    val currentCoverPath: StateFlow<String?> = _currentCoverPath.asStateFlow()

    private var positionJob: Job? = null

    fun playTrack(
        url: String,
        title: String = "Melodia Track",
        artist: String = "Melodia Studio",
        lyrics: String = "",
        coverPath: String? = null
    ) {
        _currentTitle.value = title
        _currentArtist.value = artist
        _currentLyrics.value = lyrics
        _currentCoverPath.value = coverPath

        playAudio(url)
    }

    fun playAudio(url: String) {
        try {
            if (_currentUrl.value == url && mediaPlayer != null) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        pauseAudio()
                    } else {
                        if (_currentPosition.value >= _duration.value.coerceAtLeast(100) - 500) {
                            mp.seekTo(0)
                            _currentPosition.value = 0
                        }
                        mp.start()
                        _isPlaying.value = true
                        startPositionUpdates()
                    }
                }
                return
            }

            stopAudio()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                val uri = android.net.Uri.parse(url)
                if (uri.scheme == "file" || url.startsWith("/")) {
                    val filePath = uri.path ?: url.removePrefix("file://")
                    setDataSource(filePath)
                } else {
                    setDataSource(context, uri)
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _currentUrl.value = url
                    _duration.value = mp.duration.coerceAtLeast(0)
                    startPositionUpdates()
                }
                setOnCompletionListener { mp ->
                    positionJob?.cancel()
                    _isPlaying.value = false
                    _currentPosition.value = mp.duration.coerceAtLeast(0)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    stopAudio()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio stream from URL: $url", e)
            stopAudio()
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = CoroutineScope(Dispatchers.Default).launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPosition.value = mp.currentPosition
                        if (_duration.value == 0 && mp.duration > 0) {
                            _duration.value = mp.duration
                        }
                    }
                }
                delay(100)
            }
        }
    }

    fun restartTrack() {
        try {
            mediaPlayer?.let { mp ->
                mp.seekTo(0)
                _currentPosition.value = 0
                if (!_isPlaying.value) {
                    mp.start()
                    _isPlaying.value = true
                    startPositionUpdates()
                }
            } ?: run {
                _currentUrl.value?.let { url ->
                    playAudio(url)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error restarting track", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.let { mp ->
                val targetMs = positionMs.coerceIn(0, mp.duration.coerceAtLeast(0))
                mp.seekTo(targetMs)
                _currentPosition.value = targetMs
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking to $positionMs", e)
        }
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                } else {
                    it.start()
                    _isPlaying.value = true
                    startPositionUpdates()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error toggling pause", e)
        }
    }

    fun stopAudio() {
        try {
            positionJob?.cancel()
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentUrl.value = null
            _currentPosition.value = 0
            _duration.value = 0
        }
    }
}


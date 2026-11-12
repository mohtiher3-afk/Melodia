package com.example.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.example.service.PlaybackIslandService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private const val TAG = "AudioPlayerManager"

class AudioPlayerManager private constructor(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * F-7: melodic playback must respect audio focus. Focus is requested when a
     * track starts and abandoned when playback stops. On loss we pause instead
     * of fighting the other app (calls, navigation, other players).
     */
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            mp.pause()
                            _isPlaying.value = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error pausing on audio focus loss", e)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try {
                    mediaPlayer?.setVolume(0.2f, 0.2f)
                } catch (e: Exception) {
                    Log.e(TAG, "Error ducking volume", e)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                try {
                    mediaPlayer?.setVolume(1f, 1f)
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring volume", e)
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .setWillPauseWhenDucked(true)
            .build()
            .also { audioFocusRequest = it }
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        try {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        }
    }

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
                PlaybackIslandService.start(context)
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
            // Do not start a new track while another app holds audio focus.
            if (!requestAudioFocus()) {
                Log.w(TAG, "Audio focus not granted; playback postponed")
                return
            }
            _currentUrl.value = url
            PlaybackIslandService.start(context)

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
            abandonAudioFocus()
        }
    }

    companion object {
        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}


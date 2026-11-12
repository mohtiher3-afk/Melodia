package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.domain.AudioPlayerManager
import com.example.R
import com.example.ui.theme.NothingCardSurface
import com.example.ui.theme.NothingBorder
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Optional system-level playback pill. The user must enable it and grant overlay access. */
class PlaybackIslandService : Service() {
    private lateinit var audio: AudioPlayerManager
    private lateinit var windowManager: WindowManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var stopIfIdleJob: Job? = null
    private var mediaSession: MediaSession? = null
    private var islandView: LinearLayout? = null
    private var trackTitle: TextView? = null
    private var playPauseButton: ImageButton? = null
    private var notificationManager: NotificationManager? = null
    private var foregroundStarted = false
    private var lastTrackUrl: String? = null
    private var islandDismissedForTrack = false

    override fun onCreate() {
        super.onCreate()
        audio = AudioPlayerManager.getInstance(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        createMediaSession()
        startAsForeground()
        observePlayback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> audio.pauseAudio()
            ACTION_STOP_PLAYBACK -> audio.stopAudio()
            ACTION_DISMISS_ISLAND -> {
                islandDismissedForTrack = true
                removeIsland()
            }
            ACTION_REFRESH -> refreshPresentation()
        }
        if (!foregroundStarted) startAsForeground()
        refreshPresentation()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        removeIsland()
        refreshPresentation()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        stopIfIdleJob?.cancel()
        serviceScope.cancel()
        removeIsland()
        mediaSession?.run {
            isActive = false
            release()
        }
        mediaSession = null
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        super.onDestroy()
    }

    private fun observePlayback() {
        stateJob = serviceScope.launch {
            val identity = combine(
                audio.isPlaying,
                audio.currentUrl,
                audio.currentTitle,
                audio.currentArtist
            ) { playing, url, title, artist -> TrackIdentity(playing, url, title, artist) }
            val position = combine(
                audio.currentPosition.map { it / 1_000 }.distinctUntilChanged(),
                audio.duration
            ) { positionSeconds, duration -> positionSeconds * 1_000 to duration }
            combine(identity, position) { track, timing ->
                PlaybackSnapshot(
                    track.playing,
                    track.url,
                    track.title,
                    track.artist,
                    timing.first,
                    timing.second
                )
            }.distinctUntilChanged().collect { snapshot ->
                updateSession(snapshot)
                updateNotification(snapshot)
                updateIsland(snapshot)
                if (snapshot.url == null || (snapshot.duration > 0 && snapshot.position >= snapshot.duration)) {
                    stopIfIdleJob?.cancel()
                    stopIfIdleJob = serviceScope.launch {
                        delay(400)
                        val currentUrl = audio.currentUrl.value
                        val ended = audio.duration.value > 0 &&
                            audio.currentPosition.value >= audio.duration.value
                        if (currentUrl == null || ended) stopSelf()
                    }
                } else {
                    stopIfIdleJob?.cancel()
                    stopIfIdleJob = null
                }
            }
        }
    }

    private fun createMediaSession() {
        mediaSession = MediaSession(this, "MelodiaPlayback").apply {
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    if (!audio.isPlaying.value) audio.pauseAudio()
                }

                override fun onPause() {
                    if (audio.isPlaying.value) audio.pauseAudio()
                }

                override fun onStop() {
                    audio.stopAudio()
                }

                override fun onSeekTo(pos: Long) {
                    audio.seekTo(pos.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
                }
            })
            isActive = true
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification(
            audio.currentTitle.value,
            audio.currentArtist.value,
            audio.isPlaying.value
        )
        startForeground(NOTIFICATION_ID, notification)
        foregroundStarted = true
    }

    private fun updateSession(snapshot: PlaybackSnapshot) {
        val session = mediaSession ?: return
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, snapshot.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, snapshot.artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, snapshot.duration.toLong())
                .build()
        )
        val state = when {
            snapshot.url == null -> PlaybackState.STATE_NONE
            snapshot.playing -> PlaybackState.STATE_PLAYING
            snapshot.duration > 0 && snapshot.position >= snapshot.duration -> PlaybackState.STATE_STOPPED
            else -> PlaybackState.STATE_PAUSED
        }
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SEEK_TO
                )
                .setState(state, snapshot.position.toLong(), if (snapshot.playing) 1f else 0f)
                .build()
        )
    }

    private fun updateNotification(snapshot: PlaybackSnapshot) {
        val notification = buildNotification(snapshot.title, snapshot.artist, snapshot.playing)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, artist: String, playing: Boolean): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this,
            REQUEST_PLAY_PAUSE,
            Intent(this, PlaybackIslandService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, PlaybackIslandService::class.java).setAction(ACTION_STOP_PLAYBACK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (playing) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setContentTitle(title.ifBlank { "Melodia" })
            .setContentText(artist.ifBlank { "Audio playback" })
            .setContentIntent(openApp)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(
                Notification.Action.Builder(
                    if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (playing) "Pause" else "Play",
                    playPauseIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    stopIntent
                ).build()
            )
        mediaSession?.sessionToken?.let { token ->
            builder.setStyle(
                Notification.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0)
            )
        }
        return builder.build()
    }

    private fun updateIsland(snapshot: PlaybackSnapshot) {
        if (snapshot.url != lastTrackUrl) {
            lastTrackUrl = snapshot.url
            islandDismissedForTrack = false
        }
        val hasTrack = snapshot.url != null &&
            !(snapshot.duration > 0 && snapshot.position >= snapshot.duration)
        val enabled = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getBoolean(PREFERENCE_ISLAND_ENABLED, false)
        if (!enabled || !hasTrack || islandDismissedForTrack || !Settings.canDrawOverlays(this)) {
            removeIsland()
            return
        }
        if (islandView == null) addIsland()
        trackTitle?.text = snapshot.title.ifBlank { "Melodia playback" }
        playPauseButton?.apply {
            setImageResource(if (snapshot.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            contentDescription = if (snapshot.playing) "Pause playback" else "Resume playback"
        }
    }

    private fun addIsland() {
        if (islandView != null || !Settings.canDrawOverlays(this)) return
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(5), dp(6), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(32).toFloat()
                setColor(NothingCardSurface.toArgbCompat())
                setStroke(dp(1), NothingBorder.toArgbCompat())
            }
            elevation = dp(8).toFloat()
            contentDescription = "Melodia playback controls"
        }

        val waveform = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf(8, 14, 10).forEach { barHeight ->
            val bar = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(2).toFloat()
                    setColor(NothingRed.toArgbCompat())
                }
            }
            waveform.addView(
                bar,
                LinearLayout.LayoutParams(dp(3), dp(barHeight)).apply { marginEnd = dp(2) }
            )
        }
        root.addView(waveform, LinearLayout.LayoutParams(dp(13), dp(18)).apply { marginEnd = dp(10) })

        val title = TextView(this).apply {
            setTextColor(NothingWhite.toArgbCompat())
            textSize = 13f
            typeface = resources.getFont(R.font.space_mono)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setOnClickListener { openApp() }
        }
        root.addView(title, LinearLayout.LayoutParams(0, dp(40), 1f))

        val toggle = ImageButton(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setColorFilter(NothingWhite.toArgbCompat())
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setOnClickListener { sendAction(ACTION_PLAY_PAUSE) }
        }
        root.addView(toggle, LinearLayout.LayoutParams(dp(44), dp(44)))

        val dismiss = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = "Hide floating Island"
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setColorFilter(0xFFBDBDBD.toInt())
            setPadding(dp(13), dp(13), dp(13), dp(13))
            setOnClickListener {
                islandDismissedForTrack = true
                removeIsland()
            }
        }
        root.addView(dismiss, LinearLayout.LayoutParams(dp(44), dp(44)))

        root.setOnClickListener { openApp() }
        val screenWidth = resources.displayMetrics.widthPixels
        val maxWidth = (screenWidth - dp(24)).coerceAtLeast(dp(220)).coerceAtMost(dp(420))
        val params = WindowManager.LayoutParams(
            maxWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = safeTopInset() + dp(6)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
        }

        try {
            windowManager.addView(root, params)
            islandView = root
            trackTitle = title
            playPauseButton = toggle
        } catch (_: SecurityException) {
            removeIsland()
        } catch (_: WindowManager.BadTokenException) {
            removeIsland()
        }
    }

    private fun safeTopInset(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return try {
                val insets = windowManager.currentWindowMetrics.windowInsets
                maxOf(
                    insets.getInsets(WindowInsets.Type.statusBars()).top,
                    insets.displayCutout?.safeInsetTop ?: 0
                )
            } catch (_: Exception) {
                statusBarHeightFallback()
            }
        }
        return statusBarHeightFallback()
    }

    private fun statusBarHeightFallback(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else {
            (24 * resources.displayMetrics.density).toInt()
        }
    }

    private fun refreshPresentation() {
        val snapshot = PlaybackSnapshot(
            audio.isPlaying.value,
            audio.currentUrl.value,
            audio.currentTitle.value,
            audio.currentArtist.value,
            audio.currentPosition.value,
            audio.duration.value
        )
        updateSession(snapshot)
        updateNotification(snapshot)
        updateIsland(snapshot)
    }

    private fun removeIsland() {
        islandView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // Already detached by the system.
            } catch (_: Exception) {
                // Overlay permissions can be revoked while the app is in the background.
            }
        }
        islandView = null
        trackTitle = null
        playPauseButton = null
    }

    private fun sendAction(action: String) {
        startService(Intent(this, PlaybackIslandService::class.java).setAction(action))
    }

    private fun openApp() {
        try {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        } catch (_: Exception) {
            // Keep playback controls usable if the activity cannot be opened.
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Melodia playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls while Melodia audio is active"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private data class PlaybackSnapshot(
        val playing: Boolean,
        val url: String?,
        val title: String,
        val artist: String,
        val position: Int,
        val duration: Int
    )

    private data class TrackIdentity(
        val playing: Boolean,
        val url: String?,
        val title: String,
        val artist: String
    )

    private fun androidx.compose.ui.graphics.Color.toArgbCompat(): Int =
        android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )

    companion object {
        const val PREFERENCES_NAME = "melodia_prefs"
        const val PREFERENCE_ISLAND_ENABLED = "system_island_enabled"
        private const val CHANNEL_ID = "melodia_playback"
        private const val NOTIFICATION_ID = 4107
        private const val REQUEST_OPEN_APP = 4108
        private const val REQUEST_PLAY_PAUSE = 4109
        private const val REQUEST_STOP = 4110
        private const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        private const val ACTION_STOP_PLAYBACK = "com.example.action.STOP_PLAYBACK"
        private const val ACTION_DISMISS_ISLAND = "com.example.action.DISMISS_ISLAND"
        private const val ACTION_REFRESH = "com.example.action.REFRESH_ISLAND"

        fun start(context: Context) {
            // Only run the foreground service (and its notification) when the user
            // actually enabled the Island overlay. Plain in-app playback must not
            // spawn a persistent notification by itself.
            val enabled = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREFERENCE_ISLAND_ENABLED, false)
            if (!enabled) return
            val intent = Intent(context, PlaybackIslandService::class.java).setAction(ACTION_REFRESH)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Normal in-app playback remains available if background service startup is blocked.
            }
        }

        fun refreshIfPlaying(context: Context) {
            val audio = AudioPlayerManager.getInstance(context.applicationContext)
            if (audio.currentUrl.value != null) {
                try {
                    context.startService(
                        Intent(context, PlaybackIslandService::class.java).setAction(ACTION_REFRESH)
                    )
                } catch (_: Exception) {
                    // The Island is optional; in-app playback remains usable.
                }
            }
        }
    }
}

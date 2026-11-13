package com.example.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class IslandState { Collapsed, Expanded, Hidden }

enum class IslandStatus { Idle, Running, Done, Failed, Playing }

/**
 * Singleton controller for the top-of-screen Melodia Island.
 *
 * A singleton rather than a ViewModel: it must be reachable from the Compose
 * root (to render the overlay) and from any ViewModel (to report task
 * progress) without holding a scoped reference. Task flows call
 * start()/update()/done()/fail()/toggle() with no logic changes.
 */
object IslandController {
    private val _state = MutableStateFlow(IslandUiState())
    val state: StateFlow<IslandUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoCollapseJob: Job? = null
    private var endJob: Job? = null

    private fun cancelJobs() {
        autoCollapseJob?.cancel()
        autoCollapseJob = null
        endJob?.cancel()
        endJob = null
    }

    /** Task started: compact island with indeterminate progress. */
    fun start(title: String) {
        cancelJobs()
        _state.value = IslandUiState(
            state = IslandState.Collapsed,
            title = title,
            progress = -1f,
            status = IslandStatus.Running
        )
    }

    /** Update determinate progress (0..1) for the running task. */
    fun update(progress: Float) {
        if (_state.value.status != IslandStatus.Running) return
        _state.value = _state.value.copy(progress = progress.coerceIn(0f, 1f))
    }

    /** Task completed: show Done briefly, then go Idle. */
    fun done(title: String) {
        cancelJobs()
        _state.value = _state.value.copy(
            state = IslandState.Expanded,
            title = title,
            progress = 1f,
            status = IslandStatus.Done
        )
        endJob = scope.launch {
            delay(END_MS)
            _state.value = IslandUiState()
        }
    }

    /** Task failed: show Failed briefly, then go Idle. */
    fun fail(title: String) {
        cancelJobs()
        _state.value = _state.value.copy(
            state = IslandState.Expanded,
            title = title,
            progress = 0f,
            status = IslandStatus.Failed
        )
        endJob = scope.launch {
            delay(END_MS)
            _state.value = IslandUiState()
        }
    }

    /** Show playback state: equalizer + track title. */
    fun showPlayback(title: String) {
        cancelJobs()
        _state.value = IslandUiState(
            state = IslandState.Collapsed,
            title = title,
            progress = -1f,
            status = IslandStatus.Playing
        )
    }

    /** Hide playback island. */
    fun hidePlayback() {
        cancelJobs()
        _state.value = IslandUiState()
    }

    /** Toggle the island. Expanded running tasks auto-collapse after 4s. */
    fun toggle() {
        val current = _state.value
        if (current.status == IslandStatus.Idle) return
        cancelJobs()
        val next = if (current.state == IslandState.Expanded) {
            IslandState.Collapsed
        } else {
            IslandState.Expanded
        }
        _state.value = current.copy(state = next)
        if (next == IslandState.Expanded && current.status == IslandStatus.Running) {
            autoCollapseJob = scope.launch {
                delay(AUTO_COLLAPSE_MS)
                if (_state.value.status == IslandStatus.Running) {
                    _state.value = _state.value.copy(state = IslandState.Collapsed)
                }
            }
        }
    }

    fun reset() {
        cancelJobs()
        _state.value = IslandUiState()
    }

    private const val AUTO_COLLAPSE_MS = 4000L
    private const val END_MS = 2000L
}

data class IslandUiState(
    val state: IslandState = IslandState.Collapsed,
    val title: String = "",
    val progress: Float = -1f,
    val status: IslandStatus = IslandStatus.Idle
)
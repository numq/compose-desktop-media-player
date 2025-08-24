package io.github.numq.cdmp.player

import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class PlayerController : Player {
    abstract suspend fun setRenderTargetController(target: RenderTarget): Result<RenderTarget>

    abstract suspend fun changePlaybackSpeedController(factor: Float): Result<Unit>

    abstract suspend fun changeVolumeController(value: Float): Result<Unit>

    abstract suspend fun toggleMuteController(isMuted: Boolean): Result<Unit>

    abstract suspend fun prepareController(location: String, renderTargetType: RenderTargetType): Result<Unit>

    abstract suspend fun releaseController(): Result<Unit>

    abstract suspend fun playController(): Result<Unit>

    abstract suspend fun pauseController(): Result<Unit>

    abstract suspend fun resumeController(): Result<Unit>

    abstract suspend fun stopController(): Result<Unit>

    abstract suspend fun seekController(millis: Long): Result<Unit>

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val mutex = Mutex()

    private val _renderTarget = MutableStateFlow<RenderTarget>(RenderTarget.None)

    override val renderTarget = _renderTarget.asStateFlow()

    private var seekJob: Job? = null

    private val _state = MutableStateFlow(PlayerState())

    override val state = _state.asStateFlow()

    internal fun checkLocation(location: String): String {
        val file = File(location)

        check(file.exists()) { "File does not exist: $location" }

        check(file.isFile) { "Not a file: $location" }

        check(file.canRead()) { "Cannot read file: $location" }

        return file.absolutePath
    }

    internal fun checkReadyStatus(block: PlayerStatus.Ready.() -> Unit = {}) {
        val status = _state.value.status

        check(status is PlayerStatus.Ready) { "Media player is not ready" }

        block(status)
    }

    internal fun updateReadyStatus(block: PlayerStatus.Ready.() -> Unit = {}) =
        (state as? PlayerStatus.Ready)?.let(block)

    internal fun updateStatus(status: PlayerStatus) {
        _state.value = _state.value.copy(status = status)
    }

    override suspend fun setRenderTarget(
        target: RenderTarget
    ) = setRenderTargetController(target).mapCatching { renderTarget ->
        _renderTarget.update { renderTarget }
    }

    internal fun updateTimestamp(timestamp: Duration) {
        val currentTimestamp = timestamp.coerceAtLeast(Duration.ZERO)

        _state.update { currentState ->
            when (val status = currentState.status) {
                is PlayerStatus.Ready.Playing -> PlayerStatus.Ready.Playing(
                    media = status.media, timestamp = currentTimestamp
                )

                is PlayerStatus.Ready.Paused -> PlayerStatus.Ready.Paused(
                    media = status.media, timestamp = currentTimestamp
                )

                is PlayerStatus.Ready.Seeking -> PlayerStatus.Ready.Seeking(
                    media = status.media, timestamp = currentTimestamp
                )

                else -> null
            }?.let { status -> currentState.copy(status = status) } ?: currentState
        }
    }

    override suspend fun changePlaybackSpeed(factor: Float) = mutex.withLock {
        factor.coerceIn(.5f, 2f).let { playbackSpeedFactor ->
            changePlaybackSpeedController(factor = playbackSpeedFactor).onSuccess {
                _state.update { it.copy(playbackSpeedFactor = playbackSpeedFactor) }
            }
        }
    }

    override suspend fun changeVolume(value: Float) = mutex.withLock {
        value.coerceIn(0f, 1f).let { volume ->
            changeVolumeController(value = volume).onSuccess {
                _state.update { it.copy(volume = volume) }
            }
        }
    }

    override suspend fun toggleMute(isMuted: Boolean) = mutex.withLock {
        toggleMuteController(isMuted).onSuccess {
            _state.update { it.copy(isMuted = isMuted) }
        }
    }

    override suspend fun prepare(location: String, renderTargetType: RenderTargetType) = mutex.withLock {
        runCatching {
            if (state.value.status is PlayerStatus.Ready) {
                releaseController().getOrThrow()
            }

            prepareController(location = location, renderTargetType = renderTargetType).getOrThrow()
        }
    }

    override suspend fun release() = mutex.withLock {
        releaseController()
    }

    override suspend fun play() = mutex.withLock {
        playController()
    }

    override suspend fun pause() = mutex.withLock {
        pauseController()
    }

    override suspend fun resume() = mutex.withLock {
        resumeController()
    }

    override suspend fun stop() = mutex.withLock {
        stopController()
    }

    override suspend fun seekTo(timestamp: Duration) = mutex.withLock {
        runCatching {
            val status = state.value.status

            if (status is PlayerStatus.Ready) {
                seekJob?.cancel()

                seekJob = coroutineScope.launch {
                    delay(100.milliseconds)

                    seekController(
                        millis = timestamp.coerceIn(Duration.ZERO, status.media.duration).inWholeMilliseconds
                    ).getOrThrow()
                }
            }
        }
    }

    override suspend fun close() = runCatching {
        coroutineScope.cancel()
    }
}
package io.github.numq.cdmp.player

import io.github.numq.cdmp.rendering.RenderBackend
import io.github.numq.cdmp.rendering.RenderTarget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class PlayerController : Player {
    abstract suspend fun setRenderTargetController(renderTarget: RenderTarget): Result<Unit>

    abstract suspend fun changePlaybackSpeedController(factor: Float): Result<Unit>

    abstract suspend fun changeVolumeController(volume: Float): Result<Unit>

    abstract suspend fun changeMuteController(isMuted: Boolean): Result<Unit>

    abstract suspend fun prepareController(
        location: String, renderBackend: RenderBackend, playbackSpeedFactor: Float, volume: Float, isMuted: Boolean
    ): Result<Unit>

    abstract suspend fun releaseController(): Result<Unit>

    abstract suspend fun playController(): Result<Unit>

    abstract suspend fun pauseController(): Result<Unit>

    abstract suspend fun resumeController(): Result<Unit>

    abstract suspend fun stopController(): Result<Unit>

    abstract suspend fun seekController(millis: Long): Result<Unit>

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val playerMutex = Mutex()

    private val renderMutex = Mutex()

    private var seekJob: Job? = null

    private val _playerStatus = MutableStateFlow<PlayerStatus>(PlayerStatus.Empty)

    override val playerStatus = _playerStatus.asStateFlow()

    private val _renderTarget = MutableStateFlow<RenderTarget>(RenderTarget.None)

    override val renderTarget = _renderTarget.asStateFlow()

    private fun checkLocation(location: String): String {
        val file = File(location)

        check(file.exists()) { "File does not exist: $location" }

        check(file.isFile) { "Not a file: $location" }

        check(file.canRead()) { "Cannot read file: $location" }

        return file.absolutePath
    }

    internal suspend fun requireEmptyStatus(block: suspend PlayerStatus.Empty.() -> Unit = {}) {
        val status = playerStatus.value

        check(status is PlayerStatus.Empty) { "Media player is not empty" }

        block(status)
    }

    internal suspend fun requireReadyStatus(block: suspend PlayerStatus.Ready.() -> Unit = {}) {
        val status = playerStatus.value

        check(status is PlayerStatus.Ready) { "Media player is not ready" }

        block(status)
    }

    internal suspend fun ifReadyStatus(block: suspend PlayerStatus.Ready.() -> Unit = {}) {
        (playerStatus.value as? PlayerStatus.Ready)?.block()
    }

    internal fun updateReadyStatus(
        block: PlayerStatus.Ready.() -> Unit = {}
    ) = (playerStatus.value as? PlayerStatus.Ready)?.let(block)

    internal fun updateStatus(playerStatus: PlayerStatus) {
        _playerStatus.value = playerStatus
    }

    internal fun updateTimestamp(timestamp: Duration) {
        val currentTimestamp = timestamp.coerceAtLeast(Duration.ZERO)

        when (val status = playerStatus.value) {
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
        }?.let { playerStatus ->
            _playerStatus.value = playerStatus
        }
    }

    internal suspend fun setRenderTarget(renderTarget: RenderTarget) = renderMutex.withLock {
        setRenderTargetController(renderTarget = renderTarget).onSuccess {
            _renderTarget.value = renderTarget
        }.onFailure {
            _renderTarget.value = RenderTarget.None
        }
    }

    override suspend fun changePlaybackSpeed(factor: Float) = playerMutex.withLock {
        changePlaybackSpeedController(factor = factor)
    }

    override suspend fun changeVolume(volume: Float) = playerMutex.withLock {
        changeVolumeController(volume = volume)
    }

    override suspend fun changeMute(isMuted: Boolean) = playerMutex.withLock {
        changeMuteController(isMuted = isMuted)
    }

    override suspend fun prepare(
        location: String,
        renderBackend: RenderBackend,
        playbackSpeedFactor: Float,
        volume: Float,
        isMuted: Boolean,
    ) = playerMutex.withLock {
        prepareController(
            location = checkLocation(location = location),
            renderBackend = renderBackend,
            playbackSpeedFactor = playbackSpeedFactor,
            volume = volume,
            isMuted = isMuted
        )
    }

    override suspend fun release() = playerMutex.withLock {
        ifReadyStatus {

        }
        releaseController()
    }

    override suspend fun play() = playerMutex.withLock {
        playController()
    }

    override suspend fun pause() = playerMutex.withLock {
        pauseController()
    }

    override suspend fun resume() = playerMutex.withLock {
        resumeController()
    }

    override suspend fun stop() = playerMutex.withLock {
        stopController()
    }

    override suspend fun seekTo(timestamp: Duration) = playerMutex.withLock {
        runCatching {
            when (val playerStatus = playerStatus.value) {
                is PlayerStatus.Ready -> {
                    seekJob?.cancelAndJoin()

                    seekJob = coroutineScope.launch {
                        delay(100.milliseconds)

                        seekController(
                            millis = timestamp.coerceIn(Duration.ZERO, playerStatus.media.duration).inWholeMilliseconds
                        ).getOrThrow()
                    }
                }

                else -> Unit
            }
        }
    }

    override suspend fun close() = runCatching {
        coroutineScope.cancel()

        _renderTarget.getAndUpdate { RenderTarget.None }.close().getOrThrow()
    }
}
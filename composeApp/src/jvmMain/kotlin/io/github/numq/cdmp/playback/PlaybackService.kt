package io.github.numq.cdmp.playback

import io.github.numq.cdmp.player.Player
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.RenderBackend
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

interface PlaybackService : RenderTargetProvider {
    val playbackState: StateFlow<PlaybackState>

    suspend fun selectPlaybackBackend(playbackBackend: PlaybackBackend): Result<Unit>

    suspend fun selectRenderBackend(renderBackend: RenderBackend): Result<Unit>

    suspend fun changePlaybackSpeed(factor: Float): Result<Unit>

    suspend fun changeVolume(value: Float): Result<Unit>

    suspend fun changeMute(isMuted: Boolean): Result<Unit>

    suspend fun prepare(location: String): Result<Unit>

    suspend fun release(): Result<Unit>

    suspend fun play(): Result<Unit>

    suspend fun pause(): Result<Unit>

    suspend fun resume(): Result<Unit>

    suspend fun stop(): Result<Unit>

    suspend fun seekTo(timestamp: Duration): Result<Unit>

    suspend fun close(): Result<Unit>

    class Default(
        initialPlaybackState: PlaybackState,
        private val klarityPlayerController: Player,
        private val vlcjPlayerController: Player,
        private val jfxPlayerController: Player,
    ) : PlaybackService {
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        private val mutex = Mutex()

        private val _playbackBackend = MutableStateFlow(initialPlaybackState.playbackBackend)

        private val playbackBackend = _playbackBackend.asStateFlow()

        private val _renderBackend = MutableStateFlow(initialPlaybackState.renderBackend)

        private val renderBackend = _renderBackend.asStateFlow()

        private val playerStatus = combine(
            playbackBackend,
            klarityPlayerController.playerStatus,
            vlcjPlayerController.playerStatus,
            jfxPlayerController.playerStatus
        ) { playbackBackend, klarityPlayerStatus, vlcjPlayerStatus, jfxPlayerStatus ->
            when (playbackBackend) {
                PlaybackBackend.KLARITY -> klarityPlayerStatus

                PlaybackBackend.VLCJ -> vlcjPlayerStatus

                PlaybackBackend.JFX -> jfxPlayerStatus
            }
        }

        private val _playbackSpeedFactor = MutableStateFlow(initialPlaybackState.playbackSpeedFactor)

        private val playbackSpeedFactor = _playbackSpeedFactor.asStateFlow()

        private val _volume = MutableStateFlow(initialPlaybackState.volume)

        private val volume = _volume.asStateFlow()

        private val _isMuted = MutableStateFlow(initialPlaybackState.isMuted)

        private val isMuted = _isMuted.asStateFlow()

        override val playbackState = combine(
            combine(
                playbackBackend, renderBackend, playerStatus
            ) { playbackBackend, renderBackend, playerStatus ->
                Triple(playbackBackend, renderBackend, playerStatus)
            }, combine(
                playbackSpeedFactor, volume, isMuted
            ) { playbackSpeedFactor, volume, isMuted ->
                Triple(playbackSpeedFactor, volume, isMuted)
            }) { (playbackBackend, renderBackend, playerStatus), (playbackSpeedFactor, volume, isMuted) ->
            PlaybackState(
                playbackBackend = playbackBackend,
                renderBackend = renderBackend,
                playerStatus = playerStatus,
                playbackSpeedFactor = playbackSpeedFactor,
                volume = volume,
                isMuted = isMuted
            )
        }.stateIn(scope = coroutineScope, started = SharingStarted.Eagerly, initialValue = initialPlaybackState)

        override val renderTarget = combine(
            playbackBackend,
            renderBackend,
            klarityPlayerController.renderTarget,
            vlcjPlayerController.renderTarget,
            jfxPlayerController.renderTarget
        ) { playbackBackend, renderBackend, klarityRenderTarget, vlcjRenderTarget, jfxRenderTarget ->
            when (playbackBackend) {
                PlaybackBackend.KLARITY -> klarityRenderTarget

                PlaybackBackend.VLCJ -> vlcjRenderTarget

                PlaybackBackend.JFX -> jfxRenderTarget
            }
        }.stateIn(scope = coroutineScope, started = SharingStarted.Eagerly, initialValue = RenderTarget.None)

        private fun getPlayer(backend: PlaybackBackend): Player = when (backend) {
            PlaybackBackend.KLARITY -> klarityPlayerController

            PlaybackBackend.VLCJ -> vlcjPlayerController

            PlaybackBackend.JFX -> jfxPlayerController
        }

        private suspend inline fun <reified T> withPlaybackBackend(
            block: suspend Player.() -> T
        ) = getPlayer(playbackBackend.value).block()

        private suspend fun selectBackend(
            playbackBackend: PlaybackBackend?, renderBackend: RenderBackend?
        ) = mutex.withLock {
            runCatching {
                val currentState = playbackState.value

                val currentPlaybackBackend = this.playbackBackend.value

                val currentRenderBackend = this.renderBackend.value

                val targetPlaybackBackend = playbackBackend ?: currentPlaybackBackend

                val targetRenderBackend = renderBackend ?: currentRenderBackend

                if (targetPlaybackBackend == currentPlaybackBackend && targetRenderBackend == currentRenderBackend) {
                    return@runCatching
                }

                _playbackBackend.value = targetPlaybackBackend

                _renderBackend.value = targetRenderBackend

                val currentPlayer = getPlayer(currentPlaybackBackend)

                val targetPlayer = getPlayer(targetPlaybackBackend)

                if (currentState.playerStatus is PlayerStatus.Ready) {
                    currentPlayer.release().getOrThrow()

                    targetPlayer.prepare(
                        location = currentState.playerStatus.media.location,
                        renderBackend = targetRenderBackend,
                        playbackSpeedFactor = currentState.playbackSpeedFactor,
                        volume = currentState.volume,
                        isMuted = currentState.isMuted
                    ).getOrThrow()
                }
            }
        }

        override suspend fun selectPlaybackBackend(playbackBackend: PlaybackBackend) =
            selectBackend(playbackBackend = playbackBackend, renderBackend = null)

        override suspend fun selectRenderBackend(renderBackend: RenderBackend) =
            selectBackend(playbackBackend = null, renderBackend = renderBackend)

        override suspend fun changePlaybackSpeed(factor: Float) = mutex.withLock {
            val playbackSpeedFactor = factor.coerceIn(.5f, 2f)

            withPlaybackBackend {
                changePlaybackSpeed(factor = factor).onSuccess {
                    _playbackSpeedFactor.value = playbackSpeedFactor
                }
            }
        }

        override suspend fun changeVolume(value: Float) = mutex.withLock {
            val volume = value.coerceIn(0f, 1f)

            withPlaybackBackend {
                changeVolume(value = value).onSuccess {
                    _volume.value = volume
                }
            }
        }

        override suspend fun changeMute(isMuted: Boolean) = mutex.withLock {
            withPlaybackBackend {
                changeMute(isMuted = isMuted).onSuccess {
                    _isMuted.value = isMuted
                }
            }
        }

        override suspend fun prepare(location: String) = mutex.withLock {
            withPlaybackBackend {
                with(playbackState.value) {
                    prepare(
                        location = location,
                        renderBackend = renderBackend,
                        playbackSpeedFactor = playbackSpeedFactor,
                        volume = volume,
                        isMuted = isMuted
                    )
                }
            }
        }

        override suspend fun release() = mutex.withLock {
            withPlaybackBackend {
                release()
            }
        }

        override suspend fun play() = mutex.withLock {
            withPlaybackBackend {
                play()
            }
        }

        override suspend fun pause() = mutex.withLock {
            withPlaybackBackend {
                pause()
            }
        }

        override suspend fun resume() = mutex.withLock {
            withPlaybackBackend {
                resume()
            }
        }

        override suspend fun stop() = mutex.withLock {
            withPlaybackBackend {
                stop()
            }
        }

        override suspend fun seekTo(timestamp: Duration) = mutex.withLock {
            withPlaybackBackend {
                seekTo(timestamp = timestamp)
            }
        }

        override suspend fun close() = runCatching {
            coroutineScope.cancel()
        }
    }
}
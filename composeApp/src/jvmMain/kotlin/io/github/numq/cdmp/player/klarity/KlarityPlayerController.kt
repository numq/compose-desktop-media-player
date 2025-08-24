package io.github.numq.cdmp.player.klarity

import io.github.numq.cdmp.player.PlayerController
import io.github.numq.cdmp.player.PlayerMedia
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import io.github.numq.klarity.player.KlarityPlayer
import io.github.numq.klarity.probe.ProbeManager
import io.github.numq.klarity.renderer.Renderer
import io.github.numq.klarity.state.PlayerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration.Companion.milliseconds

class KlarityPlayerController(private val klarityPlayer: KlarityPlayer) : PlayerController() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        coroutineScope.launch {
            combine(
                klarityPlayer.state, klarityPlayer.playbackTimestamp, transform = { state, timestamp ->
                    when (state) {
                        is PlayerState.Empty -> PlayerStatus.Empty

                        is PlayerState.Preparing -> PlayerStatus.Preparing

                        is PlayerState.Releasing -> PlayerStatus.Releasing

                        is PlayerState.Error -> PlayerStatus.Error(exception = state.exception)

                        is PlayerState.Ready -> with(state.media) {
                            val media = PlayerMedia(
                                id = id.toString(),
                                location = location,
                                width = videoFormat?.width,
                                height = videoFormat?.height,
                                duration = duration
                            )

                            when (state) {
                                is PlayerState.Ready.Stopped -> PlayerStatus.Ready.Stopped(media = media)

                                is PlayerState.Ready.Playing -> PlayerStatus.Ready.Playing(
                                    media = media, timestamp = timestamp
                                )

                                is PlayerState.Ready.Paused -> PlayerStatus.Ready.Paused(
                                    media = media, timestamp = timestamp
                                )

                                is PlayerState.Ready.Completed -> PlayerStatus.Ready.Completed(media = media)

                                is PlayerState.Ready.Seeking -> PlayerStatus.Ready.Seeking(
                                    media = media, timestamp = timestamp
                                )
                            }
                        }
                    }
                }).collect(::updateStatus)
        }
    }

    override suspend fun setRenderTargetController(target: RenderTarget) = runCatching {
        klarityPlayer.detachRenderer().getOrThrow()?.close()?.getOrThrow()

        when (target) {
            is RenderTarget.Klarity -> {
                klarityPlayer.attachRenderer(renderer = target.renderer).getOrThrow()

                target
            }

            else -> RenderTarget.None
        }
    }

    override suspend fun changePlaybackSpeedController(factor: Float) =
        klarityPlayer.changeSettings(settings = klarityPlayer.settings.value.copy(playbackSpeedFactor = factor))

    override suspend fun changeVolumeController(value: Float) =
        klarityPlayer.changeSettings(settings = klarityPlayer.settings.value.copy(volume = value))

    override suspend fun toggleMuteController(isMuted: Boolean) =
        klarityPlayer.changeSettings(settings = klarityPlayer.settings.value.copy(isMuted = isMuted))

    override suspend fun prepareController(location: String, renderTargetType: RenderTargetType) = runCatching {
        ProbeManager.probe(location = location).getOrNull()?.videoFormat?.let { (width, height) ->
            Renderer.create(width = width, height = height).getOrNull()?.let { renderer ->
                setRenderTarget(target = RenderTarget.Klarity(renderer = renderer)).getOrThrow()
            }
        }

        klarityPlayer.prepare(location = checkLocation(location = location)).onFailure {
            setRenderTarget(target = RenderTarget.None).getOrThrow()
        }.getOrThrow()
    }

    override suspend fun releaseController() = runCatching {
        try {
            klarityPlayer.release().getOrThrow()
        } finally {
            setRenderTarget(target = RenderTarget.None).getOrThrow()
        }
    }

    override suspend fun playController() = klarityPlayer.play()

    override suspend fun pauseController() = klarityPlayer.pause()

    override suspend fun resumeController() = klarityPlayer.resume()

    override suspend fun stopController() = klarityPlayer.stop()

    override suspend fun seekController(millis: Long) = runCatching {
        klarityPlayer.seekTo(timestamp = millis.milliseconds).getOrThrow()

        klarityPlayer.resume().getOrThrow()
    }

    override suspend fun close() = runCatching {
        super.close()

        coroutineScope.cancel()

        klarityPlayer.detachRenderer().getOrNull()?.close()?.getOrThrow()

        klarityPlayer.close().getOrThrow()
    }
}
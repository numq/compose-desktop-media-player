package io.github.numq.cdmp.player.klarity

import io.github.numq.cdmp.player.PlayerController
import io.github.numq.cdmp.player.PlayerMedia
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.RenderBackend
import io.github.numq.cdmp.rendering.RenderTarget
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

    override suspend fun setRenderTargetController(renderTarget: RenderTarget) = runCatching {
        klarityPlayer.detachRenderer().getOrThrow()?.close()?.getOrThrow()

        when (renderTarget) {
            is RenderTarget.Klarity -> klarityPlayer.attachRenderer(renderer = renderTarget.renderer).getOrThrow()

            else -> Unit
        }
    }

    override suspend fun changePlaybackSpeedController(factor: Float) = klarityPlayer.changeSettings(
        settings = klarityPlayer.settings.value.copy(playbackSpeedFactor = factor)
    )

    override suspend fun changeVolumeController(volume: Float) =
        klarityPlayer.changeSettings(settings = klarityPlayer.settings.value.copy(volume = volume))

    override suspend fun changeMuteController(isMuted: Boolean) =
        klarityPlayer.changeSettings(settings = klarityPlayer.settings.value.copy(isMuted = isMuted))

    override suspend fun prepareController(
        location: String, renderBackend: RenderBackend, playbackSpeedFactor: Float, volume: Float, isMuted: Boolean
    ) = runCatching {
        val renderer = ProbeManager.probe(location = location).getOrThrow().videoFormat?.let { (width, height) ->
            Renderer.create(width = width, height = height).getOrThrow()
        }

        if (renderer != null) {
            setRenderTarget(renderTarget = RenderTarget.Klarity(renderer = renderer)).getOrThrow()
        }

        klarityPlayer.prepare(location = location).onFailure {
            setRenderTarget(renderTarget = RenderTarget.None).getOrThrow()
        }.mapCatching {
            changePlaybackSpeedController(factor = playbackSpeedFactor).getOrThrow()

            changeVolumeController(volume = volume).getOrThrow()

            changeMuteController(isMuted = isMuted).getOrThrow()
        }.getOrThrow()
    }

    override suspend fun releaseController() = setRenderTarget(renderTarget = RenderTarget.None).mapCatching {
        klarityPlayer.release()
    }.getOrThrow()

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

        setRenderTarget(renderTarget = RenderTarget.None).getOrThrow()

        klarityPlayer.release().getOrThrow()
    }
}
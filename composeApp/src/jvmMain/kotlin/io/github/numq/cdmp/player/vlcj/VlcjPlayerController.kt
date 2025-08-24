package io.github.numq.cdmp.player.vlcj

import io.github.numq.cdmp.player.PlayerController
import io.github.numq.cdmp.player.PlayerMedia
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.BufferRenderer
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import java.awt.Canvas
import java.nio.ByteBuffer
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class VlcjPlayerController(
    private val mediaPlayerFactory: MediaPlayerFactory, private val mediaPlayer: EmbeddedMediaPlayer
) : PlayerController() {
    private val canvas = Canvas()

    private val eventListener = object : MediaPlayerEventAdapter() {
        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            updateTimestamp(newTime.milliseconds)
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            (state.value.status as? PlayerStatus.Ready)?.run {
                updateStatus(PlayerStatus.Ready.Completed(media = media))
            }
        }

        override fun error(mediaPlayer: MediaPlayer) {
            updateStatus(PlayerStatus.Error(exception = Exception("VLCJ playback error")))
        }
    }

    override suspend fun setRenderTargetController(target: RenderTarget) = runCatching {
        when (target) {
            is RenderTarget.Vlcj.Swing -> {
                mediaPlayer.videoSurface().set(mediaPlayerFactory.videoSurfaces().newVideoSurface(canvas))

                target
            }

            is RenderTarget.Vlcj.Skia -> {
                val videoSurface = mediaPlayerFactory.videoSurfaces().newVideoSurface(
                    object : BufferFormatCallback {
                        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                            val alignment = 32

                            val bytesPerPixel = 4

                            val width = target.bufferRenderer.width

                            val height = target.bufferRenderer.height

                            val pitch = ((width * bytesPerPixel + alignment - 1) / alignment) * alignment

                            return BufferFormat("BGRA", width, height, intArrayOf(pitch), intArrayOf(height))
                        }

                        override fun allocatedBuffers(buffers: Array<out ByteBuffer>) = Unit
                    }, { mediaPlayer, nativeBuffers, bufferFormat ->
                        nativeBuffers.firstOrNull()?.let { buffer ->
                            val width = bufferFormat.width

                            val height = bufferFormat.height

                            val pitch = bufferFormat.pitches[0]

                            val pixels = ByteArray(width * height * 4)

                            for (y in 0 until height) {
                                val srcPos = y * pitch

                                val dstPos = y * width * 4

                                buffer.position(srcPos)

                                buffer.get(pixels, dstPos, width * 4)
                            }

                            buffer.rewind()

                            pixels
                        }?.let { bytes ->
                            target.bufferRenderer.render(bytes = bytes)
                        }
                    }, true
                )

                mediaPlayer.videoSurface().set(videoSurface)

                target
            }

            else -> {
                mediaPlayer.videoSurface().set(null)

                (renderTarget.value as? RenderTarget.Vlcj.Skia)?.bufferRenderer?.close()

                RenderTarget.None
            }
        }
    }

    override suspend fun changePlaybackSpeedController(factor: Float) = runCatching {
        check(mediaPlayer.controls().setRate(factor.coerceIn(.5f, 2f))) { "Unable to change playback speed" }
    }

    override suspend fun changeVolumeController(value: Float) = runCatching {
        check(mediaPlayer.audio().setVolume((value * 100).toInt())) { "Unable to change volume" }
    }

    override suspend fun toggleMuteController(isMuted: Boolean) = runCatching {
        mediaPlayer.audio().isMute = isMuted
    }

    override suspend fun prepareController(location: String, renderTargetType: RenderTargetType) = runCatching {
        updateStatus(PlayerStatus.Preparing)

        mediaPlayer.events().addMediaPlayerEventListener(eventListener)

        check(mediaPlayer.media().prepare(checkLocation(location = location))) { "Unable to prepare media" }

        check(mediaPlayer.media().parsing().parse()) { "Unable to parse media" }

        var duration = Duration.ZERO

        try {
            withTimeout(5.seconds) {
                while (isActive) {
                    duration = mediaPlayer.media().info().duration().milliseconds

                    if (!duration.isNegative()) break

                    delay(100.milliseconds)
                }
            }
        } catch (_: TimeoutCancellationException) {
            error("Unable to get media duration")
        }

        val info = mediaPlayer.media().info()

        val location = info.mrl()

        val videoTrack = info.videoTracks().firstOrNull()

        val width = videoTrack?.width()?.takeIf { it > 0 }

        val height = videoTrack?.height()?.takeIf { it > 0 }

        if (width != null && height != null) {
            val target = when (renderTargetType) {
                RenderTargetType.SKIA -> RenderTarget.Vlcj.Skia(
                    bufferRenderer = BufferRenderer.create(
                        width = width, height = height
                    )
                )

                RenderTargetType.SWING -> RenderTarget.Vlcj.Swing(canvas = canvas)
            }

            setRenderTarget(target = target).getOrThrow()
        }

        val media = PlayerMedia(
            id = UUID.randomUUID().toString(),
            location = location,
            width = width,
            height = height,
            duration = duration.coerceAtLeast(Duration.ZERO)
        )

        updateStatus(PlayerStatus.Ready.Stopped(media = media))
    }

    override suspend fun releaseController() = runCatching {
        checkReadyStatus()

        mediaPlayer.release()

        mediaPlayer.events().removeMediaPlayerEventListener(eventListener)

        setRenderTarget(target = RenderTarget.None).getOrThrow()

        updateStatus(PlayerStatus.Empty)
    }

    override suspend fun playController() = runCatching {
        checkReadyStatus {
            mediaPlayer.controls().play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun pauseController() = runCatching {
        checkReadyStatus {
            if (this !is PlayerStatus.Ready.Playing) return@checkReadyStatus

            mediaPlayer.controls().pause()

            updateStatus(PlayerStatus.Ready.Paused(media = media, timestamp = timestamp))
        }
    }

    override suspend fun resumeController() = runCatching {
        checkReadyStatus {
            if (this !is PlayerStatus.Ready.Paused) return@checkReadyStatus

            mediaPlayer.controls().play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun stopController() = runCatching {
        checkReadyStatus {
            mediaPlayer.controls().stop()

            updateStatus(PlayerStatus.Ready.Stopped(media = media))
        }
    }

    override suspend fun seekController(millis: Long) = runCatching {
        checkReadyStatus {
            updateStatus(PlayerStatus.Ready.Seeking(media = media, timestamp = timestamp))

            mediaPlayer.controls().setTime(millis)

            mediaPlayer.controls().play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun close() = runCatching {
        super.close()

        mediaPlayer.release()

        (renderTarget.value as? RenderTarget.Vlcj.Skia)?.bufferRenderer?.close()

        Unit
    }
}
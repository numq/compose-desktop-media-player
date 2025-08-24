package io.github.numq.cdmp.player.jfx

import com.sun.media.jfxmedia.control.VideoDataBuffer
import com.sun.media.jfxmedia.control.VideoFormat
import com.sun.media.jfxmedia.events.NewFrameEvent
import com.sun.media.jfxmedia.events.VideoRendererListener
import io.github.numq.cdmp.player.PlayerController
import io.github.numq.cdmp.player.PlayerMedia
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.BufferRenderer
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import javafx.beans.value.ChangeListener
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.milliseconds
import com.sun.media.jfxmedia.MediaPlayer as UnderlyingMediaPlayer

class JfxPlayerController : PlayerController() {
    private val mediaView = MediaView()

    private var mediaPlayer = AtomicReference<MediaPlayer?>(null)

    private val listener = ChangeListener<Duration> { _, _, updatedTimestamp ->
        updateTimestamp(updatedTimestamp.toMillis().milliseconds)
    }

    private val rendererListener = object : VideoRendererListener {
        override fun videoFrameUpdated(event: NewFrameEvent) {
            val frameData = event.frameData

            try {
                val buffer = frameData.convertToFormat(VideoFormat.BGRA_PRE).getBufferForPlane(
                    VideoDataBuffer.PACKED_FORMAT_PLANE
                )

                val bytes = ByteArray(buffer.remaining()).also(buffer::get)

                (renderTarget.value as? RenderTarget.Jfx.Skia)?.bufferRenderer?.render(bytes = bytes)
            } finally {
                frameData.releaseFrame()
            }
        }

        override fun releaseVideoFrames() = Unit
    }

    private fun getUnderlyingPlayer(mediaPlayer: MediaPlayer) =
        (mediaPlayer.javaClass.getDeclaredMethod("retrieveJfxPlayer").apply {
            isAccessible = true
        }.invoke(mediaPlayer) as? UnderlyingMediaPlayer)

    private fun createPlayerMedia(media: Media): PlayerMedia {
        val location = media.source

        val width = media.width.takeIf { it > 0 }

        val height = media.height.takeIf { it > 0 }

        val duration = when {
            media.duration.isIndefinite || media.duration == Duration.UNKNOWN -> 0L

            else -> media.duration.toMillis().toLong()
        }.milliseconds

        return PlayerMedia(
            id = UUID.randomUUID().toString(), location = location, width = width, height = height, duration = duration
        )
    }

    override suspend fun setRenderTargetController(target: RenderTarget) = runCatching {
        when (target) {
            is RenderTarget.Jfx.Swing -> {
                target.mediaView.mediaPlayer = mediaPlayer.get()

                target
            }

            is RenderTarget.Jfx.Skia -> {
                val player = mediaPlayer.get()?.let(::getUnderlyingPlayer)

                mediaView.mediaPlayer = null

                checkNotNull(player) { "Could not get underlying JFX player" }

                player.videoRenderControl.addVideoRendererListener(rendererListener)

                target
            }

            else -> {
                mediaView.mediaPlayer = null

                mediaPlayer.get()?.let(::getUnderlyingPlayer)?.videoRenderControl?.removeVideoRendererListener(
                    rendererListener
                )

                (renderTarget.value as? RenderTarget.Jfx.Skia)?.bufferRenderer?.close()

                RenderTarget.None
            }
        }
    }

    override suspend fun changePlaybackSpeedController(factor: Float) = runCatching {
        mediaPlayer.get()?.rate = factor.toDouble()
    }

    override suspend fun changeVolumeController(value: Float) = runCatching {
        mediaPlayer.get()?.volume = state.value.volume.toDouble()
    }

    override suspend fun toggleMuteController(isMuted: Boolean) = runCatching {
        mediaPlayer.get()?.isMute = isMuted
    }

    override suspend fun prepareController(location: String, renderTargetType: RenderTargetType) = runCatching {
        check(state.value.status is PlayerStatus.Empty) { "Media player is already prepared" }

        updateStatus(PlayerStatus.Preparing)

        try {
            val file = File(checkLocation(location = location))

            val player = MediaPlayer(Media(file.toURI().toString())).apply {
                rate = state.value.playbackSpeedFactor.toDouble()

                volume = state.value.volume.toDouble()

                isMute = state.value.isMuted

                setOnHalted {
                    if (error != null) {
                        updateStatus(PlayerStatus.Error(exception = error))
                    }
                }
            }

            mediaPlayer.set(player)

            player.currentTimeProperty().addListener(listener)

            val media = suspendCoroutine { continuation ->
                val currentStatus = player.status

                when (currentStatus) {
                    MediaPlayer.Status.READY -> continuation.resume(createPlayerMedia(media = player.media))

                    MediaPlayer.Status.UNKNOWN, MediaPlayer.Status.STALLED -> player.setOnReady {
                        continuation.resume(createPlayerMedia(media = player.media))
                    }

                    else -> continuation.resumeWithException(Exception("Unexpected player status: $currentStatus"))
                }
            }

            player.setOnEndOfMedia {
                updateReadyStatus {
                    updateStatus(PlayerStatus.Ready.Completed(media = media))
                }
            }

            val width = media.width?.takeIf { it > 0 }

            val height = media.height?.takeIf { it > 0 }

            if (width != null && height != null) {
                val target = when (renderTargetType) {
                    RenderTargetType.SKIA -> RenderTarget.Jfx.Skia(
                        bufferRenderer = BufferRenderer.create(
                            width = width, height = height
                        )
                    )

                    RenderTargetType.SWING -> RenderTarget.Jfx.Swing(mediaView = mediaView)
                }

                setRenderTarget(target = target).getOrThrow()
            }

            updateStatus(PlayerStatus.Ready.Stopped(media = media))
        } catch (e: Exception) {
            updateStatus(PlayerStatus.Error(exception = e))
        }
    }

    override suspend fun releaseController() = runCatching {
        checkReadyStatus()

        updateStatus(PlayerStatus.Releasing)

        try {
            mediaPlayer.get()?.let(::getUnderlyingPlayer)?.videoRenderControl?.removeVideoRendererListener(
                rendererListener
            )

            setRenderTarget(target = RenderTarget.None).getOrThrow()

            mediaPlayer.getAndSet(null)?.apply {
                currentTimeProperty()?.removeListener(listener)

                dispose()
            }

            updateStatus(PlayerStatus.Empty)
        } catch (e: Exception) {
            updateStatus(PlayerStatus.Error(exception = e))
        }
    }

    override suspend fun playController() = runCatching {
        checkReadyStatus {
            mediaPlayer.get()?.play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun pauseController() = runCatching {
        checkReadyStatus {
            if (this !is PlayerStatus.Ready.Playing) return@checkReadyStatus

            mediaPlayer.get()?.pause()

            updateStatus(PlayerStatus.Ready.Paused(media = media, timestamp = timestamp))
        }
    }

    override suspend fun resumeController() = runCatching {
        checkReadyStatus {
            if (this !is PlayerStatus.Ready.Paused) return@checkReadyStatus

            mediaPlayer.get()?.play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun stopController() = runCatching {
        checkReadyStatus {
            mediaPlayer.get()?.stop()

            updateStatus(PlayerStatus.Ready.Stopped(media = media))
        }
    }

    override suspend fun seekController(millis: Long) = runCatching {
        checkReadyStatus {
            updateStatus(PlayerStatus.Ready.Seeking(media = media, timestamp = timestamp))

            mediaPlayer.get()?.seek(Duration(millis.toDouble()))

            mediaPlayer.get()?.play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun close() = runCatching {
        super.close()

        mediaPlayer.getAndSet(null)?.dispose()

        (renderTarget.value as? RenderTarget.Jfx.Skia)?.bufferRenderer?.close()

        Unit
    }
}
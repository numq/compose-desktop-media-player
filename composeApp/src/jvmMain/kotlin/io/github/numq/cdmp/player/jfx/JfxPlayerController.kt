package io.github.numq.cdmp.player.jfx

import com.sun.media.jfxmedia.control.VideoDataBuffer
import com.sun.media.jfxmedia.control.VideoFormat
import com.sun.media.jfxmedia.events.NewFrameEvent
import com.sun.media.jfxmedia.events.VideoRendererListener
import io.github.numq.cdmp.player.PlayerController
import io.github.numq.cdmp.player.PlayerMedia
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.BufferRenderer
import io.github.numq.cdmp.rendering.RenderBackend
import io.github.numq.cdmp.rendering.RenderTarget
import javafx.beans.value.ChangeListener
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.sun.media.jfxmedia.MediaPlayer as UnderlyingMediaPlayer

class JfxPlayerController(private val mediaView: MediaView) : PlayerController() {
    private val listener = ChangeListener<Duration> { _, _, updatedTimestamp ->
        if (playerStatus.value is PlayerStatus.Ready) {
            updateTimestamp(updatedTimestamp.toMillis().milliseconds)
        }
    }

    private val mediaPlayer = AtomicReference<MediaPlayer?>(null)

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
        (mediaPlayer::class.java.getDeclaredMethod("retrieveJfxPlayer").apply {
            isAccessible = true
        }.invoke(mediaPlayer) as? UnderlyingMediaPlayer)

    private fun createPlayerMedia(location: String, media: Media): PlayerMedia {
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

    override suspend fun setRenderTargetController(renderTarget: RenderTarget) = runCatching {
        when (val currentRenderTarget = this.renderTarget.value) {
            is RenderTarget.Jfx.Skia -> {
                mediaPlayer.get()?.let(::getUnderlyingPlayer)?.videoRenderControl?.removeVideoRendererListener(
                    rendererListener
                )

                currentRenderTarget.bufferRenderer.close()
            }

            is RenderTarget.Jfx.Awt -> mediaView.mediaPlayer = null

            else -> Unit
        }

        when (renderTarget) {
            is RenderTarget.Jfx.Skia -> {
                val player = mediaPlayer.get()?.let(::getUnderlyingPlayer)

                checkNotNull(player) { "Could not get underlying JFX player" }

                player.videoRenderControl.addVideoRendererListener(rendererListener)
            }

            is RenderTarget.Jfx.Awt -> mediaView.mediaPlayer = mediaPlayer.get()

            else -> Unit
        }
    }

    override suspend fun changePlaybackSpeedController(factor: Float) = runCatching {
        mediaPlayer.get()?.rate = factor.toDouble()
    }

    override suspend fun changeVolumeController(volume: Float) = runCatching {
        mediaPlayer.get()?.volume = volume.toDouble()
    }

    override suspend fun changeMuteController(isMuted: Boolean) = runCatching {
        mediaPlayer.get()?.isMute = isMuted
    }

    override suspend fun prepareController(
        location: String, renderBackend: RenderBackend, playbackSpeedFactor: Float, volume: Float, isMuted: Boolean
    ) = runCatching {
        requireEmptyStatus {
            updateStatus(PlayerStatus.Preparing)

            try {
                var jfxMedia: Media? = null

                withTimeoutOrNull(5.seconds) {
                    while (currentCoroutineContext().isActive && jfxMedia == null) {
                        runCatching {
                            jfxMedia = Media(File(location).toURI().toURL().toExternalForm())

                            100.milliseconds
                        }
                    }
                }

                checkNotNull(jfxMedia) { "Could not open JFX media" }

                val player = MediaPlayer(jfxMedia)

                player.currentTimeProperty().addListener(listener)

                player.setOnHalted {
                    player.error?.let { error ->
                        updateStatus(PlayerStatus.Error(exception = error))
                    }
                }

                player.setOnEndOfMedia {
                    updateReadyStatus {
                        updateStatus(PlayerStatus.Ready.Completed(media = media))
                    }
                }

                val isPrepared = withTimeoutOrNull(5.seconds) {
                    while (currentCoroutineContext().isActive) {
                        if (player.status == MediaPlayer.Status.READY) {
                            break
                        }

                        delay(100.milliseconds)
                    }

                    true
                }

                checkNotNull(isPrepared) { "Could not prepare JFX media player" }

                player.rate = playbackSpeedFactor.toDouble()

                player.volume = volume.toDouble()

                player.isMute = isMuted

                mediaPlayer.set(player)

                val playerMedia = createPlayerMedia(location = location, media = player.media)

                val width = playerMedia.width

                val height = playerMedia.height

                if (width != null && height != null) {
                    val renderTarget = when (renderBackend) {
                        RenderBackend.SKIA -> RenderTarget.Jfx.Skia(
                            bufferRenderer = BufferRenderer.create(width = width, height = height)
                        )

                        RenderBackend.AWT -> RenderTarget.Jfx.Awt(mediaView = mediaView)
                    }

                    setRenderTarget(renderTarget = renderTarget).getOrThrow()
                }

                if (renderBackend == RenderBackend.SKIA) {
                    player.seek(Duration.ZERO)
                }

                updateStatus(PlayerStatus.Ready.Stopped(media = playerMedia))
            } catch (e: Exception) {
                updateStatus(PlayerStatus.Error(exception = e))
            }
        }
    }

    override suspend fun releaseController() = runCatching {
        updateStatus(PlayerStatus.Releasing)

        setRenderTarget(renderTarget = RenderTarget.None).getOrThrow()

        mediaPlayer.getAndSet(null)?.apply {
            dispose()

            currentTimeProperty()?.removeListener(listener)
        }

        updateStatus(PlayerStatus.Empty)
    }

    override suspend fun playController() = runCatching {
        ifReadyStatus {
            mediaPlayer.get()?.play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun pauseController() = runCatching {
        ifReadyStatus {
            if (this is PlayerStatus.Ready.Playing) {
                mediaPlayer.get()?.pause()

                updateStatus(PlayerStatus.Ready.Paused(media = media, timestamp = timestamp))
            }
        }
    }

    override suspend fun resumeController() = runCatching {
        ifReadyStatus {
            if (this is PlayerStatus.Ready.Paused) {
                mediaPlayer.get()?.play()

                updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
            }
        }
    }

    override suspend fun stopController() = runCatching {
        ifReadyStatus {
            mediaPlayer.get()?.stop()

            updateStatus(PlayerStatus.Ready.Stopped(media = media))
        }
    }

    override suspend fun seekController(millis: Long) = runCatching {
        ifReadyStatus {
            updateStatus(PlayerStatus.Ready.Seeking(media = media, timestamp = timestamp))

            mediaPlayer.get()?.seek(Duration(millis.toDouble()))

            mediaPlayer.get()?.play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun close() = runCatching {
        super.close()

        setRenderTarget(renderTarget = RenderTarget.None).getOrThrow()

        mediaPlayer.getAndSet(null)?.apply {
            dispose()

            currentTimeProperty()?.removeListener(listener)
        }

        Unit
    }
}
package io.github.numq.cdmp.player.vlcj

import io.github.numq.cdmp.player.PlayerController
import io.github.numq.cdmp.player.PlayerMedia
import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.BufferRenderer
import io.github.numq.cdmp.rendering.RenderBackend
import io.github.numq.cdmp.rendering.RenderTarget
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.MediaParsedStatus
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import java.awt.Canvas
import java.nio.ByteBuffer
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class VlcjPlayerController(
    private val canvas: Canvas,
    private val mediaPlayerFactory: MediaPlayerFactory,
    private val mediaPlayer: EmbeddedMediaPlayer
) : PlayerController() {
    private val eventListener = object : MediaPlayerEventAdapter() {
        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            if (playerStatus.value is PlayerStatus.Ready) {
                updateTimestamp(newTime.milliseconds)
            }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            (playerStatus.value as? PlayerStatus.Ready)?.media?.let { media ->
                updateStatus(PlayerStatus.Ready.Completed(media = media))
            }
        }

        override fun error(mediaPlayer: MediaPlayer) {
            updateStatus(PlayerStatus.Error(exception = Exception("VLCJ playback error")))
        }
    }

    init {
        mediaPlayer.events().addMediaPlayerEventListener(eventListener)

        mediaPlayer.videoSurface().set(mediaPlayerFactory.videoSurfaces().newVideoSurface(canvas))
    }

    override suspend fun setRenderTargetController(renderTarget: RenderTarget) = runCatching {
        when (val currentRenderTarget = this.renderTarget.value) {
            is RenderTarget.Vlcj.Skia -> currentRenderTarget.bufferRenderer.close()

            is RenderTarget.Vlcj.Awt -> mediaPlayer.videoSurface().set(null)

            else -> Unit
        }

        when (renderTarget) {
            is RenderTarget.Vlcj.Skia -> {
                val bufferFormatCallback = object : BufferFormatCallback {
                    override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                        val width = renderTarget.bufferRenderer.width

                        val height = renderTarget.bufferRenderer.height

                        val alignment = 32

                        val bytesPerPixel = 4

                        val pitch = ((width * bytesPerPixel + alignment - 1) / alignment) * alignment

                        return BufferFormat("BGRA", width, height, intArrayOf(pitch), intArrayOf(height))
                    }

                    override fun newFormatSize(
                        bufferWidth: Int, bufferHeight: Int, displayWidth: Int, displayHeight: Int
                    ) = Unit

                    override fun allocatedBuffers(buffers: Array<out ByteBuffer>) = Unit
                }

                val renderCallback = object : RenderCallback {
                    override fun display(
                        mediaPlayer: MediaPlayer,
                        nativeBuffers: Array<out ByteBuffer>,
                        bufferFormat: BufferFormat,
                        displayWidth: Int,
                        displayHeight: Int
                    ) {
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
                        }?.let(renderTarget.bufferRenderer::render)
                    }

                    override fun lock(mediaPlayer: MediaPlayer) = Unit

                    override fun unlock(mediaPlayer: MediaPlayer) = Unit
                }

                val videoSurface = mediaPlayerFactory.videoSurfaces().newVideoSurface(
                    bufferFormatCallback, renderCallback, true
                )

                mediaPlayer.videoSurface().set(videoSurface)
            }

            is RenderTarget.Vlcj.Awt -> mediaPlayer.videoSurface().set(
                mediaPlayerFactory.videoSurfaces().newVideoSurface(canvas)
            )

            else -> Unit
        }
    }

    override suspend fun changePlaybackSpeedController(factor: Float) = runCatching {
        check(mediaPlayer.controls().setRate(factor)) { "Unable to change playback speed" }
    }

    override suspend fun changeVolumeController(volume: Float) = runCatching {
        check(mediaPlayer.audio().setVolume((volume * 100).toInt())) { "Unable to change volume" }
    }

    override suspend fun changeMuteController(isMuted: Boolean) = runCatching {
        mediaPlayer.audio().isMute = isMuted
    }

    override suspend fun prepareController(
        location: String, renderBackend: RenderBackend, playbackSpeedFactor: Float, volume: Float, isMuted: Boolean
    ) = runCatching {
        updateStatus(PlayerStatus.Preparing)

        check(mediaPlayer.media().prepare(location)) { "Unable to prepare media" }

        check(mediaPlayer.media().parsing().parse()) { "Unable to parse media" }

        val isPrepared = withTimeoutOrNull(5.seconds) {
            while (currentCoroutineContext().isActive) {
                if (mediaPlayer.media().parsing().status() == MediaParsedStatus.DONE) {
                    break
                }

                delay(100.milliseconds)
            }

            true
        }

        checkNotNull(isPrepared) { "Could not prepare VLCJ media player" }

        mediaPlayer.controls().setRate(playbackSpeedFactor)

        mediaPlayer.audio().setVolume((volume * 100).toInt())

        mediaPlayer.audio().isMute = isMuted

        val duration = mediaPlayer.media().info().duration().milliseconds

        val info = mediaPlayer.media().info()

        val videoTrack = info.videoTracks().firstOrNull()

        val width = videoTrack?.width()?.takeIf { it > 0 }

        val height = videoTrack?.height()?.takeIf { it > 0 }

        if (width != null && height != null) {
            val renderTarget = when (renderBackend) {
                RenderBackend.SKIA -> RenderTarget.Vlcj.Skia(
                    bufferRenderer = BufferRenderer.create(
                        width = width, height = height
                    )
                )

                RenderBackend.AWT -> RenderTarget.Vlcj.Awt(canvas = canvas)
            }

            setRenderTarget(renderTarget = renderTarget).getOrThrow()

            if (renderTarget is RenderTarget.Vlcj.Skia) {
                check(mediaPlayer.media().startPaused(location)) { "Unable to start paused media" }
            }
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
        updateStatus(PlayerStatus.Releasing)

        setRenderTarget(renderTarget = RenderTarget.None).getOrThrow()

        mediaPlayer.controls().stop()

        mediaPlayer.media().reset()

        updateStatus(PlayerStatus.Empty)
    }

    override suspend fun playController() = runCatching {
        ifReadyStatus {
            mediaPlayer.controls().play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun pauseController() = runCatching {
        ifReadyStatus {
            if (this is PlayerStatus.Ready.Playing) {
                mediaPlayer.controls().pause()

                updateStatus(PlayerStatus.Ready.Paused(media = media, timestamp = timestamp))
            }
        }
    }

    override suspend fun resumeController() = runCatching {
        ifReadyStatus {
            if (this is PlayerStatus.Ready.Paused) {
                mediaPlayer.controls().play()

                updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
            }
        }
    }

    override suspend fun stopController() = runCatching {
        ifReadyStatus {
            mediaPlayer.controls().stop()

            updateStatus(PlayerStatus.Ready.Stopped(media = media))
        }
    }

    override suspend fun seekController(millis: Long) = runCatching {
        ifReadyStatus {
            updateStatus(PlayerStatus.Ready.Seeking(media = media, timestamp = timestamp))

            mediaPlayer.controls().setTime(millis)

            mediaPlayer.controls().play()

            updateStatus(PlayerStatus.Ready.Playing(media = media, timestamp = timestamp))
        }
    }

    override suspend fun close() = runCatching {
        super.close()

        setRenderTarget(renderTarget = RenderTarget.None).getOrThrow()

        mediaPlayer.release()
    }
}
package vlcj

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.sun.jna.NativeLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import player.PlayerComponentController
import player.PlayerState
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import java.nio.file.Paths
import kotlin.io.path.pathString

class VlcjDefaultController : PlayerComponentController {

    init {
        NativeLibrary.addSearchPath(
            RuntimeUtil.getLibVlcLibraryName(),
            Paths.get(System.getProperty("user.dir"), "lib", "libvlc.dll").pathString
        )
    }

    private val stateListener = object : MediaPlayerEventAdapter() {
        override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
            mediaPlayer.audio().setVolume(50)
            _state.update { it.copy(duration = mediaPlayer.status().length()) }
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = true) }
        }

        override fun paused(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun stopped(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            _state.update { it.copy(isPlaying = false) }
        }

        override fun muted(mediaPlayer: MediaPlayer, muted: Boolean) {
            _state.update { it.copy(isMuted = muted) }
        }

        override fun volumeChanged(mediaPlayer: MediaPlayer, volume: Float) {
            _state.update { it.copy(volume = volume) }
        }

        override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
            _state.update { it.copy(timestamp = newTime) }
        }
    }

    private val embeddedComponent = EmbeddedMediaPlayerComponent()

    private val player = embeddedComponent.mediaPlayer()

    override val component: Component
        get() = embeddedComponent.videoSurfaceComponent()

    private val _preview = MutableStateFlow<ImageBitmap?>(null)

    override val preview: StateFlow<ImageBitmap?>
        get() = _preview.asStateFlow()

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun load(url: String) {
        player.events().addMediaPlayerEventListener(stateListener)
        player.media().startPaused(url)
    }

    override fun play() {
        player.controls().play()
    }

    override fun pause() {
        player.controls().setPause(true)
    }

    override fun stop() {
        player.controls().stop()
    }

    override fun dispose() {
        player.events().removeMediaPlayerEventListener(stateListener)
        player.release()
    }

    override fun seekTo(timestamp: Long) {
        player.controls().setTime(timestamp)
    }

    override fun setVolume(value: Float) {
        player.audio().setVolume((value * 100).toInt().coerceIn(0..100))
    }

    override fun toggleSound() {
        player.audio().mute()
    }

    override fun updatePreview() {
        player.video()?.videoDimension()?.run { Pair(width, height) }?.let { (width, height) ->
            player.snapshots().get(width, height)?.toComposeImageBitmap()?.let {
                _preview.value = it
            }
        }
    }
}
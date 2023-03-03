package javafx

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import controller.PlayerController
import exception.catch
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.embed.swing.SwingFXUtils
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import player.PlayerState
import java.io.File
import kotlin.math.roundToLong

class JfxController : PlayerController {

    internal var onLoad: ((MediaView) -> Unit)? = null

    private fun setupListeners(player: MediaPlayer) = catch {
        with(player) {
            val currentTimeListener = ChangeListener<Duration> { _, _, newValue ->
                _state.update { it.copy(timestamp = newValue.toMillis().roundToLong()) }
            }
            setOnReady {
                volume = .5
                currentTimeProperty()?.addListener(currentTimeListener)
                _state.update { it.copy(duration = cycleDuration.toMillis().roundToLong()) }
            }
            setOnHalted { currentTimeProperty()?.removeListener(currentTimeListener) }
            setOnPlaying { _state.update { it.copy(isPlaying = true) } }
            setOnPaused { _state.update { it.copy(isPlaying = false) } }
            setOnStopped { _state.update { it.copy(isPlaying = false) } }
            setOnEndOfMedia { _state.update { it.copy(isPlaying = false, timestamp = it.duration) } }
            setOnError { println(error.localizedMessage) }
        }
    }

    private val view by lazy { MediaView() }

    private var player: MediaPlayer? = null

    private val _preview = MutableStateFlow<ImageBitmap?>(null)

    override val preview: StateFlow<ImageBitmap?>
        get() = _preview.asStateFlow()

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun load(url: String) = catch {
        val media = Media(File(url).toURI().toASCIIString())
        media.setOnError { media.error?.let(::println) }
        if (media.error == null) {
            dispose()
            player = MediaPlayer(media).apply(::setupListeners).also {
                view.mediaPlayer?.stop()
                view.mediaPlayer?.dispose()
                view.mediaPlayer = it
                onLoad?.invoke(view)
            }
        }
    }

    override fun play() = catch {
        player?.play()
    }

    override fun pause() = catch {
        player?.pause()
    }

    override fun stop() = catch {
        player?.stop()
    }

    override fun dispose() = catch {
        player?.stop()
        player?.dispose()
        player = null
    }

    override fun seekTo(timestamp: Long) = catch {
        player?.seek(Duration(timestamp.toDouble()))
    }

    override fun setVolume(value: Float) = catch {
        value.toDouble().coerceIn(0.0, 1.0).let { v ->
            player?.volume = v
            _state.update { it.copy(volume = v.toFloat()) }
        }
    }

    override fun toggleSound() = catch {
        player?.run {
            isMute = !isMute
            _state.update { it.copy(isMuted = isMute) }
        }
    }

    override fun updatePreview() = catch {
        Platform.runLater {
            SwingFXUtils.fromFXImage(view.snapshot(null, null), null)?.toComposeImageBitmap()?.let {
                _preview.value = it
            }
        }
    }
}
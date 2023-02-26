package javafx

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import player.PlayerComponentController
import player.PlayerState
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import kotlin.math.roundToLong

class JfxDefaultController : PlayerComponentController {

    private fun setupListeners(player: MediaPlayer?, onReady: () -> Unit) = player?.runCatching {
        val currentTimeListener = ChangeListener<Duration> { _, _, newValue ->
            _state.update { it.copy(timestamp = newValue.toMillis().roundToLong()) }
        }
        setOnReady {
            volume = .5
            _state.update { it.copy(duration = cycleDuration.toMillis().roundToLong()) }
            currentTimeProperty()?.addListener(currentTimeListener)
            onReady()
        }
        setOnHalted { currentTimeProperty()?.removeListener(currentTimeListener) }
        setOnPlaying { _state.update { it.copy(isPlaying = true) } }
        setOnPaused { _state.update { it.copy(isPlaying = false) } }
        setOnStopped { _state.update { it.copy(isPlaying = false) } }
        setOnEndOfMedia { _state.update { it.copy(isPlaying = false, timestamp = it.duration) } }
        setOnError { println(error.localizedMessage) }
    }?.onFailure { println(it.localizedMessage) }

    private val view by lazy { MediaView() }

    override val component by lazy { JFXPanel() }

    private val _preview = MutableStateFlow<ImageBitmap?>(null)

    override val preview: StateFlow<ImageBitmap?>
        get() = _preview.asStateFlow()

    private val _state = MutableStateFlow(PlayerState())

    override val state: StateFlow<PlayerState>
        get() = _state.asStateFlow()

    override fun load(url: String) {
        val player = MediaPlayer(Media(url))
        setupListeners(player) {
            val videoWidth = player.media.width
            val videoHeight = player.media.height
            component.preferredSize = Dimension(videoWidth, videoHeight)
            component.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    view.fitWidth = component.width.toDouble()
                    view.fitHeight = component.height.toDouble()
                }
            })

            val stackPane = StackPane(view)
            val scene = Scene(stackPane)
            component.scene = scene
        }
        view.mediaPlayer = player
    }

    override fun play() {
        view.mediaPlayer?.play()
    }

    override fun pause() {
        view.mediaPlayer?.pause()
    }

    override fun stop() {
        view.mediaPlayer?.stop()
    }

    override fun dispose() {
        view.mediaPlayer?.dispose()
    }

    override fun seekTo(timestamp: Long) {
        view.mediaPlayer?.seek(Duration(timestamp.toDouble()))
    }

    override fun setVolume(value: Float) {
        value.toDouble().coerceIn(0.0, 1.0).let { v ->
            view.mediaPlayer?.volume = v
            _state.update { it.copy(volume = v.toFloat()) }
        }
    }

    override fun toggleSound() {
        view.mediaPlayer?.run {
            isMute = !isMute
            _state.update { it.copy(isMuted = isMute) }
        }
    }

    override fun updatePreview() = Platform.runLater {
        SwingFXUtils.fromFXImage(view.snapshot(null, null), null)?.toComposeImageBitmap()?.let {
            _preview.value = it
        }
    }
}
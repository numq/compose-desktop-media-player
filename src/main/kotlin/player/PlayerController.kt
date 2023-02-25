package player

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val preview: StateFlow<ImageBitmap?>
    val state: StateFlow<PlayerState>
    fun load(url: String)
    fun play()
    fun pause()
    fun stop()
    fun dispose()
    fun seekTo(timestamp: Long)
    fun setVolume(value: Float)
    fun toggleSound()
    fun updatePreview()
}
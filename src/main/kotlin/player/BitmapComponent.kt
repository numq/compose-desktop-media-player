package player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap

@Composable
fun BitmapComponent(modifier: Modifier = Modifier, controller: PlayerFrameController) {
    val frameBytes by controller.frameBytes.collectAsState()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val (width, height) = constraints.run { Pair(maxWidth, maxHeight) }
        val bitmap = remember(frameBytes) {
            Bitmap().apply {
                allocN32Pixels(width, height, true)
                frameBytes?.let(::installPixels)
            }.asComposeImageBitmap()
        }
        frameBytes?.run {
            Image(bitmap, "frame")
        }
    }
}
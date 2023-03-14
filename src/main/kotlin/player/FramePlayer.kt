package player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap

@Composable
fun FramePlayer(
    modifier: Modifier = Modifier,
    size: IntSize,
    bytes: ByteArray?,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = remember(size, bytes) {
            Bitmap().apply {
                allocN32Pixels(size.width, size.height, true)
                bytes?.let(::installPixels)
            }.asComposeImageBitmap()
        }
        bytes?.run { Image(bitmap, "frame") } ?: CircularProgressIndicator()
    }
}
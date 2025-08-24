package io.github.numq.cdmp.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun BufferRendererComponent(modifier: Modifier, bufferRenderer: BufferRenderer) {
    val generationId by bufferRenderer.generationId.collectAsState()

    Surface {
        key(generationId) {
            Canvas(modifier = modifier) {
                drawIntoCanvas { canvas ->
                    bufferRenderer.draw(canvas = canvas.nativeCanvas, width = size.width, height = size.height)
                }
            }
        }
    }
}
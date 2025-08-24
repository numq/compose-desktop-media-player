package io.github.numq.cdmp.player.vlcj

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import io.github.numq.cdmp.rendering.BufferRendererComponent
import io.github.numq.cdmp.rendering.RenderTarget
import java.awt.BorderLayout
import javax.swing.JPanel

@Composable
fun VlcjPlayerComponent(renderTarget: RenderTarget.Vlcj) {
    when (renderTarget) {
        is RenderTarget.Vlcj.Swing -> SwingPanel(
            background = Color.Black, factory = {
                JPanel(BorderLayout()).apply {
                    add(renderTarget.canvas, BorderLayout.CENTER)
                }
            }, modifier = Modifier.fillMaxSize()
        )

        is RenderTarget.Vlcj.Skia -> BufferRendererComponent(
            modifier = Modifier.fillMaxSize(),
            bufferRenderer = renderTarget.bufferRenderer
        )
    }
}
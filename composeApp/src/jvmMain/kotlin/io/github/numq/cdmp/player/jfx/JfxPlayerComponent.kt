package io.github.numq.cdmp.player.jfx

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import io.github.numq.cdmp.rendering.BufferRendererComponent
import io.github.numq.cdmp.rendering.RenderTarget
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane

@Composable
fun JfxPlayerComponent(renderTarget: RenderTarget.Jfx) {
    when (renderTarget) {
        is RenderTarget.Jfx.Skia -> BufferRendererComponent(
            modifier = Modifier.fillMaxSize(), bufferRenderer = renderTarget.bufferRenderer
        )

        is RenderTarget.Jfx.Awt -> with(renderTarget) {
            SwingPanel(
                background = Color.Black, factory = {
                    JFXPanel().apply {
                        Platform.runLater {
                            scene = Scene(StackPane(mediaView)).apply {
                                fill = javafx.scene.paint.Color.BLACK
                            }

                            StackPane.setAlignment(mediaView, javafx.geometry.Pos.CENTER)

                            mediaView.apply {
                                isPreserveRatio = true

                                fitWidthProperty().bind(scene.widthProperty())

                                fitHeightProperty().bind(scene.heightProperty())
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxSize()
            )
        }
    }
}
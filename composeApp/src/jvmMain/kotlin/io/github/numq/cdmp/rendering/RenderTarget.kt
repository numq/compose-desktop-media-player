package io.github.numq.cdmp.rendering

import io.github.numq.klarity.renderer.Renderer
import javafx.scene.media.MediaView
import java.awt.Canvas

sealed interface RenderTarget {
    data object None : RenderTarget

    sealed interface Jfx : RenderTarget {
        data class Skia(val bufferRenderer: BufferRenderer) : Jfx

        data class Swing(val mediaView: MediaView) : Jfx
    }

    sealed interface Vlcj : RenderTarget {
        data class Skia(val bufferRenderer: BufferRenderer) : Vlcj

        data class Swing(val canvas: Canvas) : Vlcj
    }

    data class Klarity(val renderer: Renderer) : RenderTarget
}
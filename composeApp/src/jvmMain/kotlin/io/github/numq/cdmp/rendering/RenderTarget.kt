package io.github.numq.cdmp.rendering

import io.github.numq.klarity.renderer.Renderer
import javafx.scene.media.MediaView
import java.awt.Canvas

sealed interface RenderTarget {
    suspend fun close(): Result<Unit>

    data object None : RenderTarget {
        override suspend fun close() = Result.success(Unit)
    }

    data class Klarity(val renderer: Renderer) : RenderTarget {
        override suspend fun close() = renderer.close()
    }

    sealed interface Vlcj : RenderTarget {
        data class Skia(val bufferRenderer: BufferRenderer) : Vlcj {
            override suspend fun close() = runCatching {
                bufferRenderer.close()
            }
        }

        data class Awt(val canvas: Canvas) : Vlcj {
            override suspend fun close() = Result.success(Unit)
        }
    }

    sealed interface Jfx : RenderTarget {
        data class Skia(val bufferRenderer: BufferRenderer) : Jfx {
            override suspend fun close() = runCatching {
                bufferRenderer.close()
            }
        }

        data class Awt(val mediaView: MediaView) : Jfx {
            override suspend fun close() = Result.success(Unit)
        }
    }
}
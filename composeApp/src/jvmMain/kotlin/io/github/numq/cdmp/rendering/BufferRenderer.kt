package io.github.numq.cdmp.rendering

import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.skia.Canvas
import java.io.Closeable

interface BufferRenderer : Closeable {
    val width: Int

    val height: Int

    val generationId: StateFlow<Int>

    fun render(bytes: ByteArray)

    fun draw(canvas: Canvas, width: Float, height: Float)

    companion object {
        fun create(width: Int, height: Int): BufferRenderer = SkiaBufferRenderer(width = width, height = height)
    }
}
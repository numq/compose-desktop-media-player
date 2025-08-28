package io.github.numq.cdmp.rendering

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.*
import kotlin.math.max
import kotlin.math.min

class SkiaBufferRenderer(override val width: Int, override val height: Int) : BufferRenderer {
    private val lock = Any()

    private val _generationId = MutableStateFlow(0)

    override val generationId = _generationId.asStateFlow()

    private val imageInfo = ImageInfo(
        width = width, height = height, colorType = ColorType.BGRA_8888, alphaType = ColorAlphaType.PREMUL
    )

    private val minByteSize = imageInfo.computeMinByteSize()

    private val pixmap = Pixmap()

    private val surface = Surface.makeRaster(imageInfo = imageInfo)

    override fun render(bytes: ByteArray) = synchronized(lock) {
        check(bytes.size >= minByteSize) { "Invalid render input size" }

        Data.makeFromBytes(bytes = bytes).use { buffer ->
            if (pixmap.isClosed) return

            pixmap.reset(info = imageInfo, buffer = buffer, rowBytes = imageInfo.minRowBytes)

            if (surface.isClosed) return

            surface.writePixels(pixmap = pixmap, x = 0, y = 0)

            _generationId.value = surface.generationId
        }
    }

    override fun draw(canvas: Canvas, width: Float, height: Float) = synchronized(lock) {
        val imageWidth = surface.width

        val imageHeight = surface.height

        if (imageWidth <= 0 || imageHeight <= 0) return@synchronized

        canvas.save()

        val sigma = 8f

        val backgroundScaleX = width / imageWidth

        val backgroundScaleY = height / imageHeight

        val backgroundScale = max(backgroundScaleX, backgroundScaleY)

        canvas.save()

        canvas.scale(backgroundScale, backgroundScale)

        val backgroundOffsetX = (width / backgroundScale - imageWidth) / 2f

        val backgroundOffsetY = (height / backgroundScale - imageHeight) / 2f

        canvas.translate(backgroundOffsetX, backgroundOffsetY)

        if (surface.isClosed) return@synchronized

        surface.draw(canvas = canvas, x = 0, y = 0, Paint().apply {
            imageFilter = ImageFilter.makeBlur(
                sigmaX = sigma, sigmaY = sigma, mode = FilterTileMode.CLAMP
            )
        })

        canvas.restore()

        val scaleX = width / imageWidth

        val scaleY = height / imageHeight

        val scale = min(scaleX, scaleY)

        val scaledWidth = imageWidth * scale

        val scaledHeight = imageHeight * scale

        val offsetX = (width - scaledWidth) / 2f

        val offsetY = (height - scaledHeight) / 2f

        canvas.translate(offsetX, offsetY)

        canvas.scale(scale, scale)

        if (surface.isClosed) return@synchronized

        surface.draw(canvas = canvas, x = 0, y = 0, paint = null)

        canvas.restore()
    }

    override fun close() = synchronized(lock) {
        if (!surface.isClosed) {
            surface.close()
        }

        if (!pixmap.isClosed) {
            pixmap.close()
        }
    }
}
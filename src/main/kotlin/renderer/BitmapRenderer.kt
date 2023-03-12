package renderer

import org.jetbrains.skia.Bitmap

interface BitmapRenderer {
    val bitmap: Bitmap
    val frameRate: Double
}
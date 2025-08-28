package io.github.numq.cdmp.rendering

enum class RenderBackend(val displayName: String) {
    SKIA(displayName = "Skia"), AWT(displayName = "AWT"),
}
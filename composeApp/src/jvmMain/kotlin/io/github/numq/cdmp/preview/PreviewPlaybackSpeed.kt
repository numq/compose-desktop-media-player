package io.github.numq.cdmp.preview

enum class PreviewPlaybackSpeed(val factor: Float, val label: String) {
    SLOW(factor = .5f, label = "0.5x"), NORMAL(factor = 1f, label = "1x"), FAST(factor = 2f, label = "2x")
}
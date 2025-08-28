package io.github.numq.cdmp.preview

import io.github.numq.cdmp.playback.PlaybackState
import io.github.numq.cdmp.rendering.RenderTarget

data class PreviewState(
    val playbackState: PlaybackState,
    val renderTarget: RenderTarget = RenderTarget.None
)
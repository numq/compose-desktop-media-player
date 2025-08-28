package io.github.numq.cdmp.rendering

import io.github.numq.cdmp.interactor.Interactor
import io.github.numq.cdmp.playback.PlaybackService

class SelectRenderBackend(private val playbackService: PlaybackService) : Interactor<SelectRenderBackend.Input, Unit> {
    data class Input(val renderBackend: RenderBackend)

    override suspend fun execute(input: Input) = playbackService.selectRenderBackend(
        renderBackend = input.renderBackend
    )
}
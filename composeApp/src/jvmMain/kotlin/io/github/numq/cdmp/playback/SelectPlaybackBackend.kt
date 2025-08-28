package io.github.numq.cdmp.playback

import io.github.numq.cdmp.interactor.Interactor

class SelectPlaybackBackend(
    private val playbackService: PlaybackService
) : Interactor<SelectPlaybackBackend.Input, Unit> {
    data class Input(val playbackBackend: PlaybackBackend)

    override suspend fun execute(input: Input) = playbackService.selectPlaybackBackend(
        playbackBackend = input.playbackBackend
    )
}
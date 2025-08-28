package io.github.numq.cdmp.playback

import io.github.numq.cdmp.interactor.Interactor

class ChangeMute(private val playbackService: PlaybackService) : Interactor<ChangeMute.Input, Unit> {
    data class Input(val isMuted: Boolean)

    override suspend fun execute(input: Input) = playbackService.changeMute(isMuted = input.isMuted)
}
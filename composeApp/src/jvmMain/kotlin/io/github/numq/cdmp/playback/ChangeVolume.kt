package io.github.numq.cdmp.playback

import io.github.numq.cdmp.interactor.Interactor

class ChangeVolume(private val playbackService: PlaybackService) : Interactor<ChangeVolume.Input, Unit> {
    data class Input(val volume: Float)

    override suspend fun execute(input: Input) = playbackService.changeVolume(value = input.volume)
}
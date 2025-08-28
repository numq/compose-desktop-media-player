package io.github.numq.cdmp.playback

import io.github.numq.cdmp.interactor.Interactor

class ChangePlaybackSpeed(private val playbackService: PlaybackService) :
    Interactor<ChangePlaybackSpeed.Input, Unit> {
    sealed interface Input {
        data object Decrease : Input

        data object Increase : Input

        data object Reset : Input
    }

    override suspend fun execute(input: Input): Result<Unit> {
        val factor = when (input) {
            is Input.Decrease -> .5f

            is Input.Increase -> 2f

            is Input.Reset -> 1f
        }

        return playbackService.changePlaybackSpeed(factor = factor)
    }
}
package io.github.numq.cdmp.playback

import io.github.numq.cdmp.interactor.Interactor
import kotlinx.coroutines.flow.StateFlow

class GetPlaybackState(private val playbackService: PlaybackService) : Interactor<Unit, StateFlow<PlaybackState>> {
    override suspend fun execute(input: Unit) = Result.success(playbackService.playbackState)
}
package io.github.numq.cdmp.location

import io.github.numq.cdmp.interactor.Interactor
import io.github.numq.cdmp.playback.PlaybackService

class UnloadLocation(
    private val playbackService: PlaybackService, private val locationRepository: LocationRepository
) : Interactor<Unit, Unit> {
    override suspend fun execute(input: Unit) = with(input) {
        playbackService.release().mapCatching {
            locationRepository.unloadLocation().getOrThrow()
        }
    }
}
package io.github.numq.cdmp.location

import io.github.numq.cdmp.interactor.Interactor
import io.github.numq.cdmp.playback.PlaybackService

class UploadLocation(
    private val playbackService: PlaybackService, private val locationRepository: LocationRepository
) : Interactor<UploadLocation.Input, Unit> {
    data class Input(val location: String)

    override suspend fun execute(input: Input) = with(input) {
        playbackService.prepare(location = location).mapCatching {
            locationRepository.uploadLocation(location = location).getOrThrow()
        }
    }
}
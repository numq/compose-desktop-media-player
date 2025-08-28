package io.github.numq.cdmp.location

import io.github.numq.cdmp.interactor.Interactor
import kotlinx.coroutines.flow.StateFlow

class GetLocation(private val locationRepository: LocationRepository) : Interactor<Unit, StateFlow<String?>> {
    override suspend fun execute(input: Unit) = Result.success(locationRepository.location)
}
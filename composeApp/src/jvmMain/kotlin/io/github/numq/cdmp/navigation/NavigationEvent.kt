package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.event.Event
import kotlinx.coroutines.flow.StateFlow
import java.util.*

sealed class NavigationEvent private constructor() : Event<UUID> {
    override val key: UUID = UUID.randomUUID()

    data class Error(val exception: Exception) : NavigationEvent()

    data class CollectLocation(val location: StateFlow<String?>) : NavigationEvent()

    data class StartLocationUploading(val block: suspend () -> Result<Unit>) : NavigationEvent()

    data class StartLocationUnloading(val block: suspend () -> Result<Unit>) : NavigationEvent()

    data class RequestPlaybackBackendSelection(val block: suspend () -> Result<Unit>) : NavigationEvent()

    data class RequestRenderBackendSelection(val block: suspend () -> Result<Unit>) : NavigationEvent()
}
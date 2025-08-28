package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.feature.Feature
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.ext.getFullName

class NavigationFeature(reducer: NavigationReducer) : Feature<NavigationCommand, NavigationState, NavigationEvent>(
    initialState = NavigationState(),
    coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer = reducer
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val jobs = mutableMapOf<String, Job>()

    init {
        coroutineScope.launch {
            events.collect { event ->
                val key = event::class.getFullName()

                jobs[key]?.cancel()

                when (event) {
                    is NavigationEvent.CollectLocation -> event.location.onEach { location ->
                        execute(NavigationCommand.Location.HandleUpdate(location = location))
                    }.launchIn(this)

                    is NavigationEvent.StartLocationUploading -> launch {
                        event.block.invoke().onFailure { throwable ->
                            execute(NavigationCommand.Location.HandleFailure(throwable = throwable))
                        }
                    }

                    is NavigationEvent.RequestPlaybackBackendSelection -> launch {
                        event.block.invoke()
                    }

                    is NavigationEvent.RequestRenderBackendSelection -> launch {
                        event.block.invoke()
                    }

                    else -> null
                }?.let { job ->
                    jobs[key] = job
                }
            }
        }

        coroutineScope.launch {
            execute(NavigationCommand.Location.GetUpdates)
        }

        invokeOnClose {
            coroutineScope.cancel()
        }
    }
}
package io.github.numq.cdmp.location

import io.github.numq.cdmp.feature.Reducer
import io.github.numq.cdmp.navigation.NavigationCommand
import io.github.numq.cdmp.navigation.NavigationEvent
import io.github.numq.cdmp.navigation.NavigationState
import io.github.numq.cdmp.throwable.exception

class NavigationLocationReducer(
    private val getLocation: GetLocation,
    private val uploadLocation: UploadLocation,
    private val unloadLocation: UnloadLocation,
) : Reducer<NavigationCommand.Location, NavigationState, NavigationEvent> {
    override suspend fun reduce(state: NavigationState, command: NavigationCommand.Location) = when (command) {
        is NavigationCommand.Location.GetUpdates -> getLocation.execute(Unit).fold(onSuccess = { location ->
            transition(state, NavigationEvent.CollectLocation(location = location))
        }, onFailure = { throwable ->
            transition(state, NavigationEvent.Error(exception = throwable.exception))
        })

        is NavigationCommand.Location.HandleUpdate -> {
            val locationStatus = when (val location = command.location) {
                null -> LocationStatus.Empty

                else -> LocationStatus.Uploaded(location = location)
            }

            transition(state.copy(locationStatus = locationStatus))
        }

        is NavigationCommand.Location.HandleFailure -> transition(
            state.copy(locationStatus = LocationStatus.Empty),
            NavigationEvent.Error(exception = command.throwable.exception)
        )

        is NavigationCommand.Location.StartUploading -> transition(
            state.copy(locationStatus = LocationStatus.Uploading), NavigationEvent.StartLocationUploading {
                runCatching {
                    if (state.locationStatus is LocationStatus.Uploaded) {
                        unloadLocation.execute(Unit).getOrThrow()
                    }

                    uploadLocation.execute(UploadLocation.Input(location = command.location)).getOrThrow()
                }
            })

        is NavigationCommand.Location.StartUnloading -> when (state.locationStatus) {
            is LocationStatus.Uploaded -> transition(
                state.copy(locationStatus = LocationStatus.Unloading), NavigationEvent.StartLocationUnloading {
                    unloadLocation.execute(Unit)
                })

            else -> transition(state)
        }
    }
}
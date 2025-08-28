package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.feature.Reducer
import io.github.numq.cdmp.location.NavigationLocationReducer
import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.playback.PlaybackBackend
import io.github.numq.cdmp.playback.SelectPlaybackBackend
import io.github.numq.cdmp.rendering.SelectRenderBackend

class NavigationReducer(
    private val selectPlaybackBackend: SelectPlaybackBackend,
    private val selectRenderBackend: SelectRenderBackend,
    private val navigationInteractionReducer: NavigationInteractionReducer,
    private val navigationLocationReducer: NavigationLocationReducer
) : Reducer<NavigationCommand, NavigationState, NavigationEvent> {
    override suspend fun reduce(state: NavigationState, command: NavigationCommand) = when (command) {
        is NavigationCommand.Interaction -> navigationInteractionReducer.reduce(state, command)

        is NavigationCommand.Location -> navigationLocationReducer.reduce(state, command)

        is NavigationCommand.NavigateTo -> transition(
            state.copy(tab = command.tab), NavigationEvent.RequestPlaybackBackendSelection {
                selectPlaybackBackend.execute(
                    input = SelectPlaybackBackend.Input(
                        playbackBackend = when (command.tab) {
                            NavigationTab.KLARITY -> PlaybackBackend.KLARITY

                            NavigationTab.VLCJ -> PlaybackBackend.VLCJ

                            NavigationTab.JFX -> PlaybackBackend.JFX
                        }
                    )
                )
            }
        )

        is NavigationCommand.SelectRenderBackend -> transition(
            state.copy(renderBackend = command.renderBackend), NavigationEvent.RequestRenderBackendSelection {
                selectRenderBackend.execute(
                    SelectRenderBackend.Input(renderBackend = command.renderBackend)
                )
            })
    }
}
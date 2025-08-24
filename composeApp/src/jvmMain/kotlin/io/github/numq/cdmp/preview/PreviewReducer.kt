package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Reducer
import io.github.numq.cdmp.player.Player

class PreviewReducer(
    private val player: Player,
    private val previewInteractionReducer: PreviewInteractionReducer,
    private val previewPlaybackReducer: PreviewPlaybackReducer,
) : Reducer<PreviewCommand, PreviewState, PreviewEvent> {
    override suspend fun reduce(state: PreviewState, command: PreviewCommand) = when (command) {
        is PreviewCommand.Interaction -> previewInteractionReducer.reduce(state, command)

        is PreviewCommand.Playback -> previewPlaybackReducer.reduce(state, command)

        is PreviewCommand.Initialize -> transition(
            state, PreviewEvent.CollectPlayerState(playerState = player.state)
        )

        is PreviewCommand.HandlePlayerState -> transition(state.copy(playerState = command.playerState))
    }
}
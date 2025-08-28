package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Reducer
import io.github.numq.cdmp.playback.GetPlaybackState
import io.github.numq.cdmp.throwable.exception

class PreviewReducer(
    private val getPlaybackState: GetPlaybackState,
    private val previewPlaybackReducer: PreviewPlaybackReducer
) : Reducer<PreviewCommand, PreviewState, PreviewEvent> {
    override suspend fun reduce(state: PreviewState, command: PreviewCommand) = when (command) {
        is PreviewCommand.Playback -> previewPlaybackReducer.reduce(state, command)

        is PreviewCommand.Initialize -> getPlaybackState.execute(Unit).fold(onSuccess = { playbackState ->
            transition(state, PreviewEvent.CollectPlaybackState(playbackState = playbackState))
        }, onFailure = { throwable ->
            transition(state, PreviewEvent.Error(exception = throwable.exception))
        })

        is PreviewCommand.HandlePlaybackState -> transition(state.copy(playbackState = command.playbackState))
    }
}
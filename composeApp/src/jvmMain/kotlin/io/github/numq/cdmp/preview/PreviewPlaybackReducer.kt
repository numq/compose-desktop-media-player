package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Reducer
import io.github.numq.cdmp.player.Player

class PreviewPlaybackReducer(
    private val player: Player,
) : Reducer<PreviewCommand.Playback, PreviewState, PreviewEvent> {
    override suspend fun reduce(
        state: PreviewState,
        command: PreviewCommand.Playback,
    ) = when (command) {
        is PreviewCommand.Playback.ChangePlaybackSpeed -> player.changePlaybackSpeed(factor = command.factor)

        is PreviewCommand.Playback.ChangeVolume -> player.changeVolume(value = command.value)

        is PreviewCommand.Playback.ToggleMute -> player.toggleMute(isMuted = command.isMuted)

        is PreviewCommand.Playback.Prepare -> player.prepare(
            location = command.location, renderTargetType = state.renderTargetType
        )

        is PreviewCommand.Playback.Release -> player.release()

        is PreviewCommand.Playback.Play -> player.play()

        is PreviewCommand.Playback.Pause -> player.pause()

        is PreviewCommand.Playback.Resume -> player.resume()

        is PreviewCommand.Playback.Stop -> player.stop()

        is PreviewCommand.Playback.SeekTo -> player.seekTo(timestamp = command.timestamp)
    }.fold(onSuccess = {
        transition(state)
    }, onFailure = { t ->
        transition(state, PreviewEvent.Error(Exception(t)))
    })
}
package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Reducer
import io.github.numq.cdmp.playback.ChangeMute
import io.github.numq.cdmp.playback.ChangePlaybackSpeed
import io.github.numq.cdmp.playback.ChangeVolume
import io.github.numq.cdmp.playback.ControlPlayback
import io.github.numq.cdmp.throwable.exception

class PreviewPlaybackReducer(
    private val changePlaybackSpeed: ChangePlaybackSpeed,
    private val changeVolume: ChangeVolume,
    private val changeMute: ChangeMute,
    private val controlPlayback: ControlPlayback
) : Reducer<PreviewCommand.Playback, PreviewState, PreviewEvent> {
    override suspend fun reduce(state: PreviewState, command: PreviewCommand.Playback) = when (command) {
        is PreviewCommand.Playback.Speed -> when (command) {
            is PreviewCommand.Playback.Speed.Increase -> changePlaybackSpeed.execute(ChangePlaybackSpeed.Input.Increase)

            is PreviewCommand.Playback.Speed.Decrease -> changePlaybackSpeed.execute(ChangePlaybackSpeed.Input.Decrease)

            is PreviewCommand.Playback.Speed.Reset -> changePlaybackSpeed.execute(ChangePlaybackSpeed.Input.Reset)
        }

        is PreviewCommand.Playback.ChangeVolume -> changeVolume.execute(ChangeVolume.Input(volume = command.value))

        is PreviewCommand.Playback.ToggleMute -> changeMute.execute(ChangeMute.Input(isMuted = !state.playbackState.isMuted))

        is PreviewCommand.Playback.Controls -> {
            val playbackCommand = when (command) {
                is PreviewCommand.Playback.Controls.Play -> ControlPlayback.PlaybackCommand.Play

                is PreviewCommand.Playback.Controls.Pause -> ControlPlayback.PlaybackCommand.Pause

                is PreviewCommand.Playback.Controls.Resume -> ControlPlayback.PlaybackCommand.Resume

                is PreviewCommand.Playback.Controls.Stop -> ControlPlayback.PlaybackCommand.Stop

                is PreviewCommand.Playback.Controls.SeekTo -> ControlPlayback.PlaybackCommand.SeekTo(timestamp = command.timestamp)
            }

            controlPlayback.execute(playbackCommand)
        }
    }.fold(onSuccess = {
        transition(state)
    }, onFailure = { throwable ->
        transition(state, PreviewEvent.Error(throwable.exception))
    })
}
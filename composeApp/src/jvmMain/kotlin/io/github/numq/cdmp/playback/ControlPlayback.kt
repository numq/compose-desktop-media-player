package io.github.numq.cdmp.playback

import io.github.numq.cdmp.interactor.Interactor
import kotlin.time.Duration

class ControlPlayback(
    private val playbackService: PlaybackService
) : Interactor<ControlPlayback.PlaybackCommand, Unit> {
    sealed interface PlaybackCommand {
        data object Play : PlaybackCommand

        data object Pause : PlaybackCommand

        data object Resume : PlaybackCommand

        data object Stop : PlaybackCommand

        data class SeekTo(val timestamp: Duration) : PlaybackCommand
    }

    override suspend fun execute(input: PlaybackCommand) = with(input) input@{
        with(playbackService) {
            when (this@input) {
                is PlaybackCommand.Play -> play()

                is PlaybackCommand.Pause -> pause()

                is PlaybackCommand.Resume -> resume()

                is PlaybackCommand.Stop -> stop()

                is PlaybackCommand.SeekTo -> seekTo(timestamp = timestamp)
            }
        }
    }
}
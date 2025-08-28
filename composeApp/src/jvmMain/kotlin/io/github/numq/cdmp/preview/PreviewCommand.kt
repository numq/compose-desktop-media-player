package io.github.numq.cdmp.preview

import io.github.numq.cdmp.playback.PlaybackState
import kotlin.time.Duration

sealed interface PreviewCommand {
    sealed interface Playback : PreviewCommand {
        sealed interface Speed : Playback {
            data object Increase : Speed

            data object Decrease : Speed

            data object Reset : Speed
        }

        data class ChangeVolume(val value: Float) : Playback

        data object ToggleMute : Playback

        sealed interface Controls : Playback {
            data object Play : Controls

            data object Pause : Controls

            data object Resume : Controls

            data object Stop : Controls

            data class SeekTo(val timestamp: Duration) : Controls
        }
    }

    data object Initialize : PreviewCommand

    data class HandlePlaybackState(val playbackState: PlaybackState) : PreviewCommand
}
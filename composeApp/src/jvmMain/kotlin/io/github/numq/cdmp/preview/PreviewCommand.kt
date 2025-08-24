package io.github.numq.cdmp.preview

import io.github.numq.cdmp.player.PlayerState
import kotlin.time.Duration

sealed interface PreviewCommand {
    sealed interface Interaction : PreviewCommand {
        data object ShowFileChooser : Interaction

        data object HideFileChooser : Interaction

        data object ShowInputDialog : Interaction

        data object HideInputDialog : Interaction

        data object SetDragAndDropActive : Interaction

        data object SetDragAndDropInactive : Interaction
    }

    sealed interface Playback : PreviewCommand {
        data class ChangePlaybackSpeed(val factor: Float) : Playback

        data class ChangeVolume(val value: Float) : Playback

        data class ToggleMute(val isMuted: Boolean) : Playback

        data class Prepare(val location: String) : Playback

        data object Release : Playback

        data object Play : Playback

        data object Pause : Playback

        data object Resume : Playback

        data object Stop : Playback

        data class SeekTo(val timestamp: Duration) : Playback
    }

    data object Initialize : PreviewCommand

    data class HandlePlayerState(val playerState: PlayerState) : PreviewCommand
}
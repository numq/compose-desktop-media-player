package io.github.numq.cdmp.playback

import io.github.numq.cdmp.player.PlayerStatus
import io.github.numq.cdmp.rendering.RenderBackend

data class PlaybackState(
    val playbackBackend: PlaybackBackend,
    val renderBackend: RenderBackend,
    val playerStatus: PlayerStatus = PlayerStatus.Empty,
    val playbackSpeedFactor: Float = 1f,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
)
package io.github.numq.cdmp.player

data class PlayerState(
    val status: PlayerStatus = PlayerStatus.Empty,
    val playbackSpeedFactor: Float = 1f,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
)
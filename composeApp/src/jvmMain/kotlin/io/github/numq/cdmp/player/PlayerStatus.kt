package io.github.numq.cdmp.player

import kotlin.time.Duration

sealed interface PlayerStatus {
    data object Empty : PlayerStatus

    data object Preparing : PlayerStatus

    data object Releasing : PlayerStatus

    data class Error(val exception: Exception) : PlayerStatus

    sealed interface Ready : PlayerStatus {
        val media: PlayerMedia

        val timestamp: Duration

        data class Playing(override val media: PlayerMedia, override val timestamp: Duration) : Ready

        data class Paused(override val media: PlayerMedia, override val timestamp: Duration) : Ready

        data class Stopped(override val media: PlayerMedia) : Ready {
            override val timestamp = Duration.ZERO
        }

        data class Completed(override val media: PlayerMedia) : Ready {
            override val timestamp = media.duration
        }

        data class Seeking(override val media: PlayerMedia, override val timestamp: Duration) : Ready
    }
}
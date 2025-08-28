package io.github.numq.cdmp.preview

import io.github.numq.cdmp.event.Event
import io.github.numq.cdmp.playback.PlaybackState
import kotlinx.coroutines.flow.Flow
import java.util.*

sealed class PreviewEvent private constructor() : Event<UUID> {
    override val key: UUID = UUID.randomUUID()

    data class Error(val exception: Exception) : PreviewEvent()

    data class CollectPlaybackState(val playbackState: Flow<PlaybackState>) : PreviewEvent()
}
package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Feature
import io.github.numq.cdmp.playback.PlaybackState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.ext.getFullName

class PreviewFeature(
    reducer: PreviewReducer, initialPlaybackState: PlaybackState
) : Feature<PreviewCommand, PreviewState, PreviewEvent>(
    initialState = PreviewState(playbackState = initialPlaybackState),
    coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    reducer = reducer
) {
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val jobs = mutableMapOf<String, Job>()

    init {
        coroutineScope.launch {
            events.collect { event ->
                val key = event::class.getFullName()

                jobs[key]?.cancel()

                when (event) {
                    is PreviewEvent.CollectPlaybackState -> event.playbackState.onEach { playbackState ->
                        execute(PreviewCommand.HandlePlaybackState(playbackState = playbackState))
                    }.launchIn(this)

                    else -> null
                }?.let { job ->
                    jobs[key] = job
                }
            }
        }

        coroutineScope.launch {
            execute(PreviewCommand.Initialize)
        }

        invokeOnClose {
            coroutineScope.cancel()
        }
    }
}
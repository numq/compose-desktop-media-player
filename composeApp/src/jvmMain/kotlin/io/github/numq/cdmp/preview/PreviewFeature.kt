package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Feature
import io.github.numq.cdmp.rendering.RenderTargetType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.ext.getFullName

class PreviewFeature(
    reducer: PreviewReducer, renderTargetType: RenderTargetType
) : Feature<PreviewCommand, PreviewState, PreviewEvent>(
    initialState = PreviewState(renderTargetType = renderTargetType),
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
                    is PreviewEvent.CollectPlayerState -> launch {
                        event.playerState.collectLatest { playerState ->
                            execute(PreviewCommand.HandlePlayerState(playerState = playerState))
                        }
                    }

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
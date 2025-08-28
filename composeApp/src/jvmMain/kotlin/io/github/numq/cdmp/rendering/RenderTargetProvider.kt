package io.github.numq.cdmp.rendering

import kotlinx.coroutines.flow.StateFlow

interface RenderTargetProvider {
    val renderTarget: StateFlow<RenderTarget>
}
package io.github.numq.cdmp.player

import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

interface Player {
    val state: StateFlow<PlayerState>

    val renderTarget: StateFlow<RenderTarget>

    suspend fun setRenderTarget(target: RenderTarget): Result<Unit>

    suspend fun changePlaybackSpeed(factor: Float): Result<Unit>

    suspend fun changeVolume(value: Float): Result<Unit>

    suspend fun toggleMute(isMuted: Boolean): Result<Unit>

    suspend fun prepare(location: String, renderTargetType: RenderTargetType): Result<Unit>

    suspend fun release(): Result<Unit>

    suspend fun play(): Result<Unit>

    suspend fun pause(): Result<Unit>

    suspend fun resume(): Result<Unit>

    suspend fun stop(): Result<Unit>

    suspend fun seekTo(timestamp: Duration): Result<Unit>

    suspend fun close(): Result<Unit>
}
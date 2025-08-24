package io.github.numq.cdmp.preview

import io.github.numq.cdmp.player.PlayerState
import io.github.numq.cdmp.rendering.RenderTargetType

data class PreviewState(
    val renderTargetType: RenderTargetType,
    val playerState: PlayerState = PlayerState(),
    val isFileChooserVisible: Boolean = false,
    val isInputDialogVisible: Boolean = false,
    val isDragAndDropActive: Boolean = false,
)
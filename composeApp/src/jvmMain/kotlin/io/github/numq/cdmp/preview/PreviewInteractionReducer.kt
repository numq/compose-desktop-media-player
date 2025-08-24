package io.github.numq.cdmp.preview

import io.github.numq.cdmp.feature.Reducer

class PreviewInteractionReducer : Reducer<PreviewCommand.Interaction, PreviewState, PreviewEvent> {
    override suspend fun reduce(
        state: PreviewState,
        command: PreviewCommand.Interaction,
    ) = when (command) {
        is PreviewCommand.Interaction.ShowFileChooser -> transition(state.copy(isFileChooserVisible = true))

        is PreviewCommand.Interaction.HideFileChooser -> transition(state.copy(isFileChooserVisible = false))

        is PreviewCommand.Interaction.ShowInputDialog -> transition(state.copy(isInputDialogVisible = true))

        is PreviewCommand.Interaction.HideInputDialog -> transition(state.copy(isInputDialogVisible = false))

        is PreviewCommand.Interaction.SetDragAndDropActive -> transition(
            state.copy(isInputDialogVisible = false, isDragAndDropActive = true)
        )

        is PreviewCommand.Interaction.SetDragAndDropInactive -> transition(state.copy(isDragAndDropActive = false))
    }
}
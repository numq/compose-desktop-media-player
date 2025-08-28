package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.feature.Reducer

class NavigationInteractionReducer : Reducer<NavigationCommand.Interaction, NavigationState, NavigationEvent> {
    override suspend fun reduce(
        state: NavigationState,
        command: NavigationCommand.Interaction,
    ) = when (command) {
        is NavigationCommand.Interaction.ShowFileChooser -> {
            // todo

            transition(state.copy(isFileChooserVisible = true))
        }

        is NavigationCommand.Interaction.HideFileChooser -> {
            // todo

            transition(state.copy(isFileChooserVisible = false))
        }

        is NavigationCommand.Interaction.ShowInputDialog -> {
            // todo

            transition(state.copy(isInputDialogVisible = true))
        }

        is NavigationCommand.Interaction.HideInputDialog -> {
            // todo

            transition(state.copy(isInputDialogVisible = false))
        }

        is NavigationCommand.Interaction.SetDragAndDropActive -> transition(
            state.copy(isInputDialogVisible = false, isDragAndDropActive = true)
        )

        is NavigationCommand.Interaction.SetDragAndDropInactive -> transition(state.copy(isDragAndDropActive = false))
    }
}
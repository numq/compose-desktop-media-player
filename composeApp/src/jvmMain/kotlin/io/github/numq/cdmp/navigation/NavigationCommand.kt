package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.rendering.RenderBackend

sealed interface NavigationCommand {
    sealed interface Interaction : NavigationCommand {
        data object ShowFileChooser : Interaction

        data object HideFileChooser : Interaction

        data object ShowInputDialog : Interaction

        data object HideInputDialog : Interaction

        data object SetDragAndDropActive : Interaction

        data object SetDragAndDropInactive : Interaction
    }

    sealed interface Location : NavigationCommand {
        data object GetUpdates : Location

        data class HandleUpdate(val location: String?) : Location

        data class HandleFailure(val throwable: Throwable) : Location

        data class StartUploading(val location: String) : Location

        data object StartUnloading : Location
    }

    data class NavigateTo(val tab: NavigationTab) : NavigationCommand

    data class SelectRenderBackend(val renderBackend: RenderBackend) : NavigationCommand
}
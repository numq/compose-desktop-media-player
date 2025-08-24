package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.rendering.RenderTargetType

sealed interface NavigationCommand {
    data class NavigateTo(val tab: NavigationTab) : NavigationCommand

    data class ChangeRenderTargetType(val renderTargetType: RenderTargetType) : NavigationCommand
}
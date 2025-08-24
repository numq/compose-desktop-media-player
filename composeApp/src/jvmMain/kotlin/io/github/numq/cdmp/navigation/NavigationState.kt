package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.rendering.RenderTargetType

data class NavigationState(
    val tab: NavigationTab = NavigationTab.KLARITY,
    val renderTargetType: RenderTargetType = RenderTargetType.SKIA
)
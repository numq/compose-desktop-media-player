package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.location.LocationStatus
import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.rendering.RenderBackend

data class NavigationState(
    val locationStatus: LocationStatus = LocationStatus.Empty,
    val tab: NavigationTab = NavigationTab.KLARITY,
    val renderBackend: RenderBackend = RenderBackend.SKIA,
    val isFileChooserVisible: Boolean = false,
    val isInputDialogVisible: Boolean = false,
    val isDragAndDropActive: Boolean = false,
)
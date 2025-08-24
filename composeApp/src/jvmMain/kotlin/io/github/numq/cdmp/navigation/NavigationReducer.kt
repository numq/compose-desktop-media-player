package io.github.numq.cdmp.navigation

import io.github.numq.cdmp.feature.Reducer

class NavigationReducer : Reducer<NavigationCommand, NavigationState, NavigationEvent> {
    override suspend fun reduce(state: NavigationState, command: NavigationCommand) = when (command) {
        is NavigationCommand.NavigateTo -> transition(state.copy(tab = command.tab))

        is NavigationCommand.ChangeRenderTargetType -> transition(state.copy(renderTargetType = command.renderTargetType))
    }
}
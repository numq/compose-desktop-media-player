package io.github.numq.cdmp.navigation.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.numq.cdmp.di.Scope
import io.github.numq.cdmp.player.jfx.JfxPlayerComponent
import io.github.numq.cdmp.player.jfx.JfxPlayerController
import io.github.numq.cdmp.preview.PreviewFeature
import io.github.numq.cdmp.preview.PreviewPlaybackReducer
import io.github.numq.cdmp.preview.PreviewView
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import kotlinx.coroutines.flow.map
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.component.getScopeId
import org.koin.core.component.getScopeName
import org.koin.core.parameter.parametersOf

@Composable
fun JfxNavigationTab(
    renderTargetType: RenderTargetType,
    onRenderTargetTypeChange: (RenderTargetType) -> Unit,
) {
    val koin = getKoin()

    val scope = koin.getOrCreateScope(Scope.JFX.getScopeId(), Scope.JFX.getScopeName())

    val player = koinInject<JfxPlayerController>(scope = scope)

    val previewPlaybackReducer = koinInject<PreviewPlaybackReducer>(scope = scope) {
        parametersOf(player)
    }

    val previewFeature = koinInject<PreviewFeature>(scope = scope) {
        parametersOf(player, previewPlaybackReducer, renderTargetType)
    }

    val renderTarget by player.renderTarget.map { renderTarget ->
        renderTarget as? RenderTarget.Jfx
    }.collectAsState(RenderTarget.None)

    DisposableEffect(Unit) {
        onDispose {
            scope.close()
        }
    }

    PreviewView(
        feature = previewFeature,
        renderTargetType = renderTargetType,
        onRenderTargetTypeChange = onRenderTargetTypeChange,
        isRenderTargetTypeChangeable = true,
        isOverlaySupported = renderTarget is RenderTarget.Jfx.Skia
    ) {
        if (renderTarget is RenderTarget.Jfx) {
            JfxPlayerComponent(renderTarget = renderTarget as RenderTarget.Jfx)
        }
    }
}
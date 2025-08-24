package io.github.numq.cdmp.navigation.tab

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.numq.cdmp.di.Scope
import io.github.numq.cdmp.player.klarity.KlarityPlayerController
import io.github.numq.cdmp.preview.PreviewFeature
import io.github.numq.cdmp.preview.PreviewPlaybackReducer
import io.github.numq.cdmp.preview.PreviewView
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetType
import io.github.numq.klarity.renderer.Renderer
import io.github.numq.klarity.renderer.compose.Background
import io.github.numq.klarity.renderer.compose.Foreground
import io.github.numq.klarity.renderer.compose.RendererComponent
import kotlinx.coroutines.flow.map
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.component.getScopeId
import org.koin.core.component.getScopeName
import org.koin.core.parameter.parametersOf

@Composable
fun KlarityNavigationTab(renderTargetType: RenderTargetType) {
    val koin = getKoin()

    val scope = koin.getOrCreateScope(Scope.KLARITY.getScopeId(), Scope.KLARITY.getScopeName())

    val player = koinInject<KlarityPlayerController>(scope = scope)

    val previewPlaybackReducer = koinInject<PreviewPlaybackReducer>(scope = scope) {
        parametersOf(player)
    }

    val previewFeature = koinInject<PreviewFeature>(scope = scope) {
        parametersOf(player, previewPlaybackReducer, renderTargetType)
    }

    val renderer by player.renderTarget.map { renderTarget ->
        (renderTarget as? RenderTarget.Klarity)?.renderer
    }.collectAsState(null)

    DisposableEffect(Unit) {
        onDispose {
            scope.close()
        }
    }

    PreviewView(
        feature = previewFeature,
        renderTargetType = renderTargetType,
        onRenderTargetTypeChange = {},
        isRenderTargetTypeChangeable = false,
        isOverlaySupported = true
    ) {
        if (renderer != null) {
            RendererComponent(
                modifier = Modifier.fillMaxSize(),
                foreground = Foreground(renderer = renderer as Renderer),
                background = Background.Blur(),
                placeholder = {
                    CircularProgressIndicator()
                })
        }
    }
}
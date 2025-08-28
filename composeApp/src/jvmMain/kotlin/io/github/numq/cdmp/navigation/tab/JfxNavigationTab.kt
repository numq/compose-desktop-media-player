package io.github.numq.cdmp.navigation.tab

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.numq.cdmp.player.jfx.JfxPlayerComponent
import io.github.numq.cdmp.preview.PreviewFeature
import io.github.numq.cdmp.preview.PreviewView
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetProvider
import org.koin.compose.koinInject

@Composable
fun JfxNavigationTab(previewFeature: PreviewFeature = koinInject()) {
    val renderTarget by koinInject<RenderTargetProvider>().renderTarget.collectAsState()

    PreviewView(feature = previewFeature, isOverlaySupported = renderTarget is RenderTarget.Jfx.Skia) {
        when (renderTarget) {
            is RenderTarget.None -> Unit

            is RenderTarget.Jfx -> JfxPlayerComponent(renderTarget = renderTarget as RenderTarget.Jfx)

            else -> CircularProgressIndicator()
        }
    }
}
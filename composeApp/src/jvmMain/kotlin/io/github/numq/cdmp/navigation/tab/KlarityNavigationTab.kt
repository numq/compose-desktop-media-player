package io.github.numq.cdmp.navigation.tab

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.numq.cdmp.preview.PreviewFeature
import io.github.numq.cdmp.preview.PreviewView
import io.github.numq.cdmp.rendering.RenderTarget
import io.github.numq.cdmp.rendering.RenderTargetProvider
import io.github.numq.klarity.renderer.compose.Background
import io.github.numq.klarity.renderer.compose.Foreground
import io.github.numq.klarity.renderer.compose.RendererComponent
import org.koin.compose.koinInject

@Composable
fun KlarityNavigationTab(previewFeature: PreviewFeature = koinInject()) {
    val renderTarget by koinInject<RenderTargetProvider>().renderTarget.collectAsState()

    PreviewView(feature = previewFeature, isOverlaySupported = true) {
        when (renderTarget) {
            is RenderTarget.Klarity -> RendererComponent(
                modifier = Modifier.fillMaxSize(),
                foreground = Foreground(renderer = (renderTarget as RenderTarget.Klarity).renderer),
                background = Background.Blur(),
                placeholder = {
                    CircularProgressIndicator()
                })

            is RenderTarget.None -> Unit

            else -> CircularProgressIndicator()
        }
    }
}
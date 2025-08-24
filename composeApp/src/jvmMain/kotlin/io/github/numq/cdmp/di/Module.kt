package io.github.numq.cdmp.di

import io.github.numq.cdmp.navigation.NavigationFeature
import io.github.numq.cdmp.navigation.NavigationReducer
import io.github.numq.cdmp.performance.PerformanceMonitor
import io.github.numq.cdmp.performance.SystemPerformanceMonitor
import io.github.numq.cdmp.player.Player
import io.github.numq.cdmp.player.jfx.JfxPlayerController
import io.github.numq.cdmp.player.klarity.KlarityPlayerController
import io.github.numq.cdmp.player.vlcj.VlcjPlayerController
import io.github.numq.cdmp.preview.PreviewFeature
import io.github.numq.cdmp.preview.PreviewInteractionReducer
import io.github.numq.cdmp.preview.PreviewPlaybackReducer
import io.github.numq.cdmp.preview.PreviewReducer
import io.github.numq.cdmp.rendering.RenderTargetType
import io.github.numq.klarity.player.KlarityPlayer
import kotlinx.coroutines.runBlocking
import org.koin.core.component.getScopeName
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import uk.co.caprica.vlcj.factory.MediaPlayerFactory

private val performance = module {
    single { SystemPerformanceMonitor() } bind PerformanceMonitor::class
}

private val jfx = module {
    scope(Scope.JFX.getScopeName()) {
        scoped { JfxPlayerController() } bind Player::class onClose { runBlocking { it?.close() } }
    }
}

private val vlcj = module {
    scope(Scope.VLCJ.getScopeName()) {
        scoped { MediaPlayerFactory() }

        scoped { get<MediaPlayerFactory>().mediaPlayers().newEmbeddedMediaPlayer() }

        scoped { VlcjPlayerController(mediaPlayerFactory = get(), mediaPlayer = get()) } bind Player::class onClose {
            runBlocking {
                it?.close()?.getOrNull()
            }
        }
    }
}

private val klarity = module {
    scope(Scope.KLARITY.getScopeName()) {
        scoped { KlarityPlayer.create().getOrThrow() } onClose {
            runBlocking {
                it?.close()?.getOrNull()
            }
        }

        scoped { KlarityPlayerController(klarityPlayer = get()) } bind Player::class onClose {
            runBlocking {
                it?.close()?.getOrNull()
            }
        }
    }
}

private val preview = module {
    factory { PreviewInteractionReducer() }

    factory { (player: Player) -> PreviewPlaybackReducer(player = player) }

    factory { (player: Player, previewPlaybackReducer: PreviewPlaybackReducer, renderTargetType: RenderTargetType) ->
        PreviewFeature(
            reducer = PreviewReducer(
                player = player, previewInteractionReducer = get(), previewPlaybackReducer = previewPlaybackReducer
            ), renderTargetType = renderTargetType
        )
    } onClose { it?.close() }
}

private val navigation = module {
    single { NavigationReducer() }

    single { NavigationFeature(get()) } onClose { it?.close() }
}

internal val appModule = listOf(performance, jfx, vlcj, klarity, preview, navigation)
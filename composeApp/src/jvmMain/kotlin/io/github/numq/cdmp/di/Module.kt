package io.github.numq.cdmp.di

import io.github.numq.cdmp.location.*
import io.github.numq.cdmp.navigation.NavigationFeature
import io.github.numq.cdmp.navigation.NavigationInteractionReducer
import io.github.numq.cdmp.navigation.NavigationReducer
import io.github.numq.cdmp.performance.PerformanceMonitor
import io.github.numq.cdmp.performance.SystemPerformanceMonitor
import io.github.numq.cdmp.playback.*
import io.github.numq.cdmp.player.Player
import io.github.numq.cdmp.player.jfx.JfxPlayerController
import io.github.numq.cdmp.player.klarity.KlarityPlayerController
import io.github.numq.cdmp.player.vlcj.VlcjPlayerController
import io.github.numq.cdmp.preview.PreviewFeature
import io.github.numq.cdmp.preview.PreviewPlaybackReducer
import io.github.numq.cdmp.preview.PreviewReducer
import io.github.numq.cdmp.rendering.RenderBackend
import io.github.numq.cdmp.rendering.RenderTargetProvider
import io.github.numq.cdmp.rendering.SelectRenderBackend
import io.github.numq.klarity.player.KlarityPlayer
import javafx.scene.media.MediaView
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import java.awt.Canvas

private val performance = module {
    single { SystemPerformanceMonitor() } bind PerformanceMonitor::class
}

private val location = module {
    single { LocationRepository.Default() } bind LocationRepository::class

    factory { GetLocation(locationRepository = get()) }

    factory { UploadLocation(playbackService = get(), locationRepository = get()) }

    factory { UnloadLocation(playbackService = get(), locationRepository = get()) }
}

private val playback = module {
    single { PlaybackState(playbackBackend = PlaybackBackend.KLARITY, renderBackend = RenderBackend.SKIA) }

    single {
        PlaybackService.Default(
            initialPlaybackState = get(),
            klarityPlayerController = get<KlarityPlayerController>(),
            vlcjPlayerController = get<VlcjPlayerController>(),
            jfxPlayerController = get<JfxPlayerController>()
        )
    } bind PlaybackService::class onClose {
        runBlocking {
            it?.close()?.getOrNull()
        }
    }

    factory { SelectPlaybackBackend(playbackService = get()) }

    factory { GetPlaybackState(playbackService = get()) }

    factory { ChangePlaybackSpeed(playbackService = get()) }

    factory { ChangeVolume(playbackService = get()) }

    factory { ControlPlayback(playbackService = get()) }

    factory { ChangeMute(playbackService = get()) }
}

private val klarity = module {
    single { KlarityPlayer.create().getOrThrow() } onClose {
        runBlocking {
            it?.close()?.getOrNull()
        }
    }

    single {
        KlarityPlayerController(klarityPlayer = get())
    } bind Player::class onClose {
        runBlocking {
            it?.close()?.getOrNull()
        }
    }
}

private val vlcj = module {
    single { Canvas() }

    single { MediaPlayerFactory() }

    single { get<MediaPlayerFactory>().mediaPlayers().newEmbeddedMediaPlayer() }

    single {
        VlcjPlayerController(canvas = get(), mediaPlayerFactory = get(), mediaPlayer = get())
    } bind Player::class onClose {
        runBlocking {
            it?.close()?.getOrNull()
        }
    }
}

private val jfx = module {
    single { MediaView() }

    single {
        JfxPlayerController(mediaView = get())
    } bind Player::class onClose {
        runBlocking {
            it?.close()?.getOrNull()
        }
    }
}

private val preview = module {
    single {
        PreviewPlaybackReducer(
            changePlaybackSpeed = get(), changeVolume = get(), changeMute = get(), controlPlayback = get()
        )
    }

    single { PreviewReducer(getPlaybackState = get(), previewPlaybackReducer = get()) }

    single { PreviewFeature(reducer = get(), initialPlaybackState = get<PlaybackState>()) } onClose { it?.close() }
}

private val rendering = module {
    single<RenderTargetProvider> { get<PlaybackService>() }

    factory { SelectRenderBackend(playbackService = get()) }
}

private val navigation = module {
    single { NavigationInteractionReducer() }

    single {
        NavigationLocationReducer(
            getLocation = get(),
            uploadLocation = get(),
            unloadLocation = get(),
        )
    }

    single {
        NavigationReducer(
            selectPlaybackBackend = get(),
            selectRenderBackend = get(),
            navigationInteractionReducer = get(),
            navigationLocationReducer = get()
        )
    }

    single { NavigationFeature(reducer = get()) } onClose { it?.close() }
}

internal val appModule = listOf(performance, location, playback, klarity, vlcj, jfx, preview, rendering, navigation)
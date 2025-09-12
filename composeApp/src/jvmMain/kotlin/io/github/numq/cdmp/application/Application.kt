package io.github.numq.cdmp.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import io.github.numq.cdmp.decoration.WindowDecoration
import io.github.numq.cdmp.decoration.WindowDecorationColors
import io.github.numq.cdmp.di.appModule
import io.github.numq.cdmp.navigation.NavigationView
import io.github.numq.cdmp.theme.ApplicationTheme
import io.github.numq.klarity.player.KlarityPlayer
import javafx.application.Platform
import org.koin.core.context.startKoin
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

private const val APP_NAME = "Compose Desktop Media Player"

private val minimumWindowSize = DpSize(900.dp, 600.dp)

fun main() {
    startKoin { modules(appModule) }

    Platform.setImplicitExit(false)

    Platform.startup {}

    check(NativeDiscovery().discover()) { "Unable to find VLC binaries" }

    KlarityPlayer.load().getOrThrow()

    application {
        val isSystemInDarkTheme = isSystemInDarkTheme()

        ApplicationTheme(isSystemInDarkTheme) {
            WindowDecoration(
                isDarkTheme = isSystemInDarkTheme,
                title = APP_NAME,
                initialWindowSize = minimumWindowSize,
                minimumWindowSize = minimumWindowSize,
                isTransparent = false,
                windowDecorationColors = WindowDecorationColors(switchSchemeButton = { Color.Unspecified }),
                onCloseRequest = {
                    Platform.exit()

                    exitApplication()
                },
                titleContent = {
                    Text(APP_NAME, color = MaterialTheme.colorScheme.primary)
                },
                windowContent = {
                    NavigationView()
                })
        }
    }
}
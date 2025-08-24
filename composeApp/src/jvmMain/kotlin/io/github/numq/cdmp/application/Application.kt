package io.github.numq.cdmp.application

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.sun.jna.NativeLibrary.addSearchPath
import io.github.numq.cdmp.decoration.WindowDecoration
import io.github.numq.cdmp.decoration.WindowDecorationColors
import io.github.numq.cdmp.di.appModule
import io.github.numq.cdmp.navigation.NavigationView
import io.github.numq.cdmp.theme.ApplicationTheme
import io.github.numq.klarity.player.KlarityPlayer
import javafx.application.Platform
import org.koin.core.context.startKoin
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.nio.file.Paths
import kotlin.io.path.pathString

private const val APP_NAME = "Compose Desktop Media Player"

private val minimumWindowSize = DpSize(900.dp, 600.dp)

fun main() {
    startKoin { modules(appModule) }

    Platform.setImplicitExit(false)

    Platform.startup {}

    addSearchPath(
        RuntimeUtil.getLibVlcLibraryName(),
        Paths.get(System.getProperty("user.dir"), "libs", "libvlc.dll").pathString
    )

    check(NativeDiscovery().discover()) { "Unable to find VLC binaries" }

    KlarityPlayer.load().getOrThrow()

    application {
        val isSystemInDarkTheme = isSystemInDarkTheme()

        ApplicationTheme(isSystemInDarkTheme) {
            WindowDecoration(
                isDarkTheme = isSystemInDarkTheme,
                initialWindowSize = minimumWindowSize,
                minimumWindowSize = minimumWindowSize,
                isTransparent = false,
                windowDecorationColors = WindowDecorationColors(switchSchemeButton = { Color.Unspecified }),
                onCloseRequest = {
                    Platform.exit()

                    exitApplication()
                },
                title = {
                    Text(APP_NAME, color = MaterialTheme.colorScheme.primary)
                },
                content = {
                    NavigationView()
                })
        }
    }
}
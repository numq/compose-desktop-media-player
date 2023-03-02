package application

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import javafx.application.Platform
import navigation.Navigation

fun main() = application {
    Platform.setImplicitExit(false)
    Window(onCloseRequest = {
        Platform.exit()
        exitApplication()
    }) {
        MaterialTheme {
            Navigation()
        }
    }
}
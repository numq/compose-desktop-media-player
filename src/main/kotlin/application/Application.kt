package application

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import navigation.Navigation

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            Navigation()
        }
    }
}
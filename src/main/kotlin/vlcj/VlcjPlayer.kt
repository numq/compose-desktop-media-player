package vlcj

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import player.PlayerComponentContainer

@Composable
fun VlcjPlayer(url: String) {
    val componentController = remember(url) { VlcjComponentController() }
    PlayerComponentContainer(
        Modifier.fillMaxSize(),
        url,
        componentController.component,
        componentController
    )
}
package player.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import player.DefaultControls
import player.PlayerController

@Composable
fun FramePlayer(
    modifier: Modifier = Modifier,
    url: String,
    size: IntSize,
    bytes: ByteArray?,
    controller: PlayerController,
) {
    DisposableEffect(url) {
        if (url.isNotBlank()) controller.load(url)
        onDispose {
            controller.dispose()
        }
    }
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FrameContainer(Modifier.weight(1f), size, bytes)
        DefaultControls(Modifier.fillMaxWidth(), controller)
    }
}
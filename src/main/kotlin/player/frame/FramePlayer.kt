package player.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import player.PlayerController
import player.DefaultControls

@Composable
fun FramePlayer(
    modifier: Modifier = Modifier,
    url: String,
    size: IntSize,
    bytes: ByteArray?,
    controller: PlayerController,
) {
    LaunchedEffect(url) {
        if (url.isNotBlank()) controller.load(url)
    }
    DisposableEffect(url) {
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
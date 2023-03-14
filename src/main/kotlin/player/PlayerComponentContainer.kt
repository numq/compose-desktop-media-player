package player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import controller.PlayerController
import java.awt.Component

@Composable
fun PlayerComponentContainer(
    modifier: Modifier = Modifier,
    url: String,
    component: Component,
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
        ComponentPlayer(Modifier.weight(1f), component)
        DefaultControls(Modifier.fillMaxWidth(), controller)
    }
}
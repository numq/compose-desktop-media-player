package navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import player.Input
import javafx.JfxComponentController
import javafx.JfxFrameController
import player.PlayerSource
import vlcj.VlcjComponentController
import vlcj.VlcjFrameController

@Composable
fun Navigation() {
    val (destination, setDestination) = remember { mutableStateOf<Destination>(Destination.Jfx()) }
    Scaffold(Modifier.fillMaxSize(), bottomBar = {
        BottomNavigation {
            Destination.values.map {
                BottomNavigationItem(destination == it, icon = {
                    Text(it.title)
                }, onClick = {
                    setDestination(it.updateUrl(destination.url))
                })
            }
        }
    }) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.weight(1f)) {
                when (destination) {
                    is Destination.Jfx -> {
                        val componentController = remember(destination.url) { JfxComponentController() }
                        val frameController = remember(destination.url) { JfxFrameController() }
                        PlayerSource(
                            destination.url,
                            componentController.component,
                            componentController,
                            frameController.size.collectAsState(null).value?.run {
                                IntSize(first, second)
                            } ?: IntSize.Zero,
                            frameController.bytes.collectAsState(null).value,
                            frameController
                        )
                    }
                    is Destination.Vlcj -> {
                        val componentController = remember(destination.url) { VlcjComponentController() }
                        val frameController = remember(destination.url) { VlcjFrameController() }
                        PlayerSource(
                            destination.url,
                            componentController.component,
                            componentController,
                            frameController.size.collectAsState(null).value?.run {
                                IntSize(first, second)
                            } ?: IntSize.Zero,
                            frameController.bytes.collectAsState(null).value,
                            frameController
                        )
                    }
                }
            }
            Input {
                setDestination(destination.updateUrl(it))
            }
        }
    }
}
package javafx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import player.PlayerComponentContainer
import player.PlayerFrameContainer

@Composable
fun JfxPlayer(url: String) {
    val componentController = remember(url) { JfxComponentController() }
    val frameController = remember(url) { JfxFrameController() }
    val (currentTabIndex, setCurrentTabIndex) = remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween) {
        TabRow(currentTabIndex, Modifier.fillMaxWidth()) {
            Tab(currentTabIndex == 0, onClick = {
                setCurrentTabIndex(0)
            }, text = {
                Text("Component")
            })
            Tab(currentTabIndex == 1, onClick = {
                setCurrentTabIndex(1)
            }, text = {
                Text("Frame")
            })
        }
        when (currentTabIndex) {
            0 -> PlayerComponentContainer(
                Modifier.weight(1f),
                url,
                componentController.component,
                componentController
            )
            1 -> PlayerFrameContainer(
                Modifier.weight(1f),
                url,
                frameController.size.collectAsState(null).value?.run { IntSize(first, second) } ?: IntSize.Zero,
                frameController.bytes.collectAsState(null).value,
                frameController
            )
        }
    }
}
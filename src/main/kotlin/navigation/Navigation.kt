package navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import input.Input
import javacv.JcvPlayer
import javafx.JfxPlayer
import vlcj.VlcjPlayer

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
                    is Destination.Jfx -> JfxPlayer(destination.url)
                    is Destination.Vlcj -> VlcjPlayer(destination.url)
                    is Destination.Jcv -> JcvPlayer(destination.url)
                }
            }
            Input {
                setDestination(destination.updateUrl(it))
            }
        }
    }
}
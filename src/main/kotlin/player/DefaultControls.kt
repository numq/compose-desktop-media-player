package player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import extension.formatTimestamp
import kotlin.math.roundToLong

@Composable
fun DefaultControls(modifier: Modifier = Modifier, controller: PlayerController) {

    val state by controller.state.collectAsState()

    val animatedTimestamp by animateFloatAsState(state.timestamp.toFloat())

    Column(
        modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Slider(
            value = animatedTimestamp,
            onValueChange = { controller.seekTo(it.roundToLong()) },
            valueRange = 0f..state.duration.toFloat(),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(state.timestamp.formatTimestamp())
            }
            if (state.isPlaying) {
                IconButton(controller::pause) {
                    Icon(Icons.Rounded.Pause, "pause media")
                }
            } else {
                IconButton(controller::play) {
                    Icon(Icons.Rounded.PlayArrow, "play media")
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isMuted || state.volume == 0f) IconButton(controller::toggleSound) {
                        Icon(Icons.Rounded.VolumeOff, "volume off")
                    }
                    else {
                        if (state.volume < .5f) IconButton(controller::toggleSound) {
                            Icon(Icons.Rounded.VolumeDown, "volume low")
                        } else IconButton(controller::toggleSound) {
                            Icon(Icons.Rounded.VolumeUp, "volume high")
                        }
                    }
                    Slider(
                        value = state.volume,
                        onValueChange = controller::setVolume,
                        modifier = Modifier.width(128.dp)
                    )
                }
            }
        }
    }
}
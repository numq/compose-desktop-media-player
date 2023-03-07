package player

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import controller.PlayerController
import extension.formatTimestamp
import kotlin.math.roundToLong

@Composable
fun DefaultControls(modifier: Modifier = Modifier, controller: PlayerController) {

    val (seekingTimestamp, setSeekingTimestamp) = remember { mutableStateOf<Float?>(null) }

    with(controller) {
        with(state.collectAsState().value) {
            Column(
                modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Slider(
                    value = seekingTimestamp ?: timestamp.toFloat(),
                    onValueChange = {
                        setSeekingTimestamp(it)
                    },
                    onValueChangeFinished = {
                        seekingTimestamp?.let(Float::roundToLong)?.let(::seekTo)
                        setSeekingTimestamp(null)
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPlaying) {
                        IconButton(controller::pause) {
                            Icon(Icons.Rounded.Pause, "pause media")
                        }
                    } else {
                        IconButton(controller::play) {
                            Icon(Icons.Rounded.PlayArrow, "play media")
                        }
                    }
                    Text(timestamp.formatTimestamp())
                    Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        if (isMuted || volume == 0f) IconButton(controller::toggleSound) {
                            Icon(Icons.Rounded.VolumeOff, "volume off")
                        }
                        else {
                            if (volume < .5f) IconButton(controller::toggleSound) {
                                Icon(Icons.Rounded.VolumeDown, "volume low")
                            } else IconButton(controller::toggleSound) {
                                Icon(Icons.Rounded.VolumeUp, "volume high")
                            }
                        }
                        Slider(
                            value = volume,
                            onValueChange = controller::setVolume,
                            modifier = Modifier.fillMaxWidth(.5f)
                        )
                    }
                }
            }
        }
    }
}
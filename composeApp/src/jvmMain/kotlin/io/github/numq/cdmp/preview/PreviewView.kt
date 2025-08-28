package io.github.numq.cdmp.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import io.github.numq.cdmp.player.PlayerControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

@Composable
fun PreviewView(
    feature: PreviewFeature,
    isOverlaySupported: Boolean,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.Default }

    val state by feature.state.collectAsState()

    val error by feature.events.filterIsInstance<PreviewEvent.Error>().collectAsState(null)

    val exceptions = remember { mutableStateListOf<Exception?>() }

    LaunchedEffect(error) {
        error?.exception?.let(exceptions::add)
    }

    Surface {
        PlayerControls(
            playbackState = state.playbackState,
            isOverlaySupported = isOverlaySupported,
            increasePlaybackSpeed = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Speed.Increase)
                }
            },
            decreasePlaybackSpeed = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Speed.Decrease)
                }
            },
            resetPlaybackSpeed = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Speed.Reset)
                }
            },
            changeVolume = { value ->
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.ChangeVolume(value = value))
                }
            },
            toggleMute = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.ToggleMute)
                }
            },
            play = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Controls.Play)
                }
            },
            pause = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Controls.Pause)
                }
            },
            resume = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Controls.Resume)
                }
            },
            stop = {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Controls.Stop)
                }
            },
            seekTo = { timestamp ->
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Controls.SeekTo(timestamp = timestamp))
                }
            },
            content = content
        )
    }
}
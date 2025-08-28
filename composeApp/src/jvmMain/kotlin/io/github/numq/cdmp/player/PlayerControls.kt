package io.github.numq.cdmp.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.numq.cdmp.playback.PlaybackState
import io.github.numq.cdmp.preview.PreviewPlaybackSpeed
import io.github.numq.cdmp.timestamp.formatTimestamp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerControls(
    playbackState: PlaybackState,
    isOverlaySupported: Boolean,
    increasePlaybackSpeed: () -> Unit,
    decreasePlaybackSpeed: () -> Unit,
    resetPlaybackSpeed: () -> Unit,
    changeVolume: (Float) -> Unit,
    toggleMute: () -> Unit,
    play: () -> Unit,
    pause: () -> Unit,
    resume: () -> Unit,
    stop: () -> Unit,
    seekTo: (timestamp: Duration) -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (playbackState.playerStatus !is PlayerStatus.Error && isOverlaySupported) {
            content()
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            when {
                playbackState.playerStatus is PlayerStatus.Error -> Text(
                    playbackState.playerStatus.exception.localizedMessage ?: "Unknown playback error",
                    style = MaterialTheme.typography.headlineLarge
                )

                !isOverlaySupported -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    content()
                }
            }
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = if (isOverlaySupported) .5f else 1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
                ) {
                    Slider(
                        value = (playbackState.playerStatus as? PlayerStatus.Ready)?.timestamp?.inWholeMilliseconds?.toFloat()
                            ?: 0f,
                        onValueChange = {
                            seekTo(it.toLong().milliseconds)
                        },
                        valueRange = 0f..((playbackState.playerStatus as? PlayerStatus.Ready)?.media?.duration?.inWholeMilliseconds?.toFloat()
                            ?: 1f),
                        modifier = Modifier.fillMaxWidth().alpha(
                            if (playbackState.playerStatus is PlayerStatus.Ready) 1f else 0f
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(
                                space = 8.dp, alignment = Alignment.CenterHorizontally
                            ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(.5f), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    (playbackState.playerStatus as? PlayerStatus.Ready)?.timestamp?.inWholeMilliseconds?.formatTimestamp()
                                        ?: ""
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                SingleChoiceSegmentedButtonRow {
                                    PreviewPlaybackSpeed.entries.forEachIndexed { index, playbackSpeed ->
                                        SegmentedButton(
                                            selected = playbackSpeed.factor == playbackState.playbackSpeedFactor,
                                            onClick = {
                                                when (playbackSpeed.factor) {
                                                    .5f -> decreasePlaybackSpeed()

                                                    1f -> resetPlaybackSpeed()

                                                    2f -> increasePlaybackSpeed()
                                                }
                                            },
                                            shape = MaterialTheme.shapes.extraSmall,
                                            label = {
                                                Text(playbackSpeed.label, color = MaterialTheme.colorScheme.onSurface)
                                            })

                                        if (index < PreviewPlaybackSpeed.entries.lastIndex) {
                                            Spacer(Modifier.width(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.weight(1f).alpha(
                                if (playbackState.playerStatus is PlayerStatus.Ready) 1f else 0f
                            ), horizontalArrangement = Arrangement.spacedBy(
                                space = 8.dp, alignment = Alignment.CenterHorizontally
                            ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = stop,
                                enabled = playbackState.playerStatus !is PlayerStatus.Ready.Stopped,
                                modifier = Modifier.alpha(if (playbackState.playerStatus !is PlayerStatus.Ready.Stopped) 1f else .5f)
                            ) {
                                Icon(Icons.Default.Stop, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            when (playbackState.playerStatus) {
                                is PlayerStatus.Ready.Playing -> IconButton(onClick = pause) {
                                    Icon(Icons.Default.Pause, null, tint = MaterialTheme.colorScheme.onSurface)
                                }

                                else -> IconButton(onClick = {
                                    when (playbackState.playerStatus) {
                                        is PlayerStatus.Ready.Paused -> resume()

                                        is PlayerStatus.Ready.Stopped -> play()

                                        is PlayerStatus.Ready.Completed -> {
                                            stop()

                                            play()
                                        }

                                        else -> Unit
                                    }
                                }) {
                                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(
                                space = 8.dp, alignment = Alignment.CenterHorizontally
                            ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    playbackState.playerStatus::class.simpleName.toString(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(
                                        space = 8.dp, alignment = Alignment.CenterHorizontally
                                    ), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = toggleMute) {
                                        Icon(
                                            when {
                                                playbackState.isMuted -> Icons.AutoMirrored.Filled.VolumeMute

                                                playbackState.volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff

                                                playbackState.volume < .5f -> Icons.AutoMirrored.Filled.VolumeDown

                                                else -> Icons.AutoMirrored.Filled.VolumeUp
                                            }, null, tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Slider(
                                        value = playbackState.volume,
                                        onValueChange = changeVolume,
                                        modifier = Modifier.width(128.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
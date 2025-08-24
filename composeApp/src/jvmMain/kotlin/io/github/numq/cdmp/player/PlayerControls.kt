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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.cdmp.preview.PreviewPlaybackSpeed
import io.github.numq.cdmp.rendering.RenderTargetType
import io.github.numq.cdmp.timestamp.formatTimestamp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerControls(
    playerState: PlayerState,
    isRenderTargetTypeChangeable: Boolean,
    isOverlaySupported: Boolean,
    renderTargetType: RenderTargetType,
    onRenderTargetTypeChange: (RenderTargetType) -> Unit,
    changePlaybackSpeed: (factor: Float) -> Unit,
    changeVolume: (value: Float) -> Unit,
    toggleMute: (isMuted: Boolean) -> Unit,
    play: () -> Unit,
    pause: () -> Unit,
    resume: () -> Unit,
    stop: () -> Unit,
    seekTo: (timestamp: Duration) -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isOverlaySupported) {
            content()
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = if (isOverlaySupported) .5f else 1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp, alignment = Alignment.CenterHorizontally
                    ), verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        Text(
                            (playerState.status as? PlayerStatus.Ready)?.media?.location ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp, alignment = Alignment.CenterHorizontally
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(RenderTargetType.SKIA.displayName)
                        Switch(
                            checked = isRenderTargetTypeChangeable && renderTargetType == RenderTargetType.SWING,
                            onCheckedChange = {
                                onRenderTargetTypeChange(
                                    when (renderTargetType) {
                                        RenderTargetType.SKIA -> RenderTargetType.SWING

                                        RenderTargetType.SWING -> RenderTargetType.SKIA
                                    }
                                )
                            },
                            enabled = isRenderTargetTypeChangeable
                        )
                        Text(RenderTargetType.SWING.displayName)
                    }
                }
            }
            if (!isOverlaySupported) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
                        value = (playerState.status as? PlayerStatus.Ready)?.timestamp?.inWholeMilliseconds?.toFloat()
                            ?: 0f,
                        onValueChange = {
                            seekTo(it.toLong().milliseconds)
                        },
                        valueRange = 0f..((playerState.status as? PlayerStatus.Ready)?.media?.duration?.inWholeMilliseconds?.toFloat()
                            ?: 1f),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = playerState.status is PlayerStatus.Ready
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
                                    (playerState.status as? PlayerStatus.Ready)?.timestamp?.inWholeMilliseconds?.formatTimestamp()
                                        ?: ""
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                SingleChoiceSegmentedButtonRow {
                                    PreviewPlaybackSpeed.entries.forEachIndexed { index, playbackSpeed ->
                                        SegmentedButton(
                                            selected = playbackSpeed.factor == playerState.playbackSpeedFactor,
                                            onClick = {
                                                changePlaybackSpeed(playbackSpeed.factor)
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
                            modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(
                                space = 8.dp, alignment = Alignment.CenterHorizontally
                            ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = stop,
                                enabled = playerState.status !is PlayerStatus.Ready.Stopped,
                                modifier = Modifier.alpha(if (playerState.status !is PlayerStatus.Ready.Stopped) 1f else .5f)
                            ) {
                                Icon(Icons.Default.Stop, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            when (playerState.status) {
                                is PlayerStatus.Ready.Playing -> IconButton(onClick = pause) {
                                    Icon(Icons.Default.Pause, null, tint = MaterialTheme.colorScheme.onSurface)
                                }

                                else -> IconButton(onClick = {
                                    when (playerState.status) {
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
                                    playerState.status::class.simpleName.toString(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(
                                        space = 8.dp, alignment = Alignment.CenterHorizontally
                                    ), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        toggleMute(!playerState.isMuted)
                                    }) {
                                        Icon(
                                            when {
                                                playerState.isMuted -> Icons.AutoMirrored.Filled.VolumeMute

                                                playerState.volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff

                                                playerState.volume < .5f -> Icons.AutoMirrored.Filled.VolumeDown

                                                else -> Icons.AutoMirrored.Filled.VolumeUp
                                            }, null, tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Slider(
                                        value = playerState.volume,
                                        onValueChange = {
                                            changeVolume(it)
                                        },
                                        modifier = Modifier.width(128.dp),
                                        enabled = playerState.status is PlayerStatus.Ready
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
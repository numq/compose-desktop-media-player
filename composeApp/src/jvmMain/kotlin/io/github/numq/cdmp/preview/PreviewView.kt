package io.github.numq.cdmp.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import io.github.numq.cdmp.player.PlayerControls
import io.github.numq.cdmp.rendering.RenderTargetType
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.net.URI
import java.nio.file.Paths

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PreviewView(
    feature: PreviewFeature,
    renderTargetType: RenderTargetType,
    onRenderTargetTypeChange: (RenderTargetType) -> Unit,
    isRenderTargetTypeChangeable: Boolean,
    isOverlaySupported: Boolean,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    val error by feature.events.filterIsInstance<PreviewEvent.Error>().collectAsState(null)

    val exceptions = remember { mutableStateListOf<Exception?>() }

    LaunchedEffect(error) {
        error?.exception?.let(exceptions::add)
    }

    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                super.onStarted(event)

                coroutineScope.launch {
                    feature.execute(PreviewCommand.Interaction.SetDragAndDropActive)
                }
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)

                coroutineScope.launch {
                    feature.execute(PreviewCommand.Interaction.SetDragAndDropInactive)
                }
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                (event.dragData() as? DragData.FilesList)?.readFiles()?.lastOrNull()?.let { location ->
                    coroutineScope.launch {
                        feature.execute(
                            PreviewCommand.Playback.Prepare(
                                location = Paths.get(URI.create(location)).toAbsolutePath().toString()
                            )
                        )
                    }
                }

                return true
            }
        }
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize().dragAndDropTarget(shouldStartDragAndDrop = { event ->
                event.dragData() is DragData.FilesList
            }, target = dropTarget).clickable(interactionSource = null, indication = null) {
                coroutineScope.launch {
                    feature.execute(PreviewCommand.Playback.Release)
                }
            }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
        ) {
            PlayerControls(
                playerState = state.playerState,
                isRenderTargetTypeChangeable = isRenderTargetTypeChangeable,
                isOverlaySupported = isOverlaySupported,
                renderTargetType = renderTargetType,
                onRenderTargetTypeChange = onRenderTargetTypeChange,
                changePlaybackSpeed = { factor ->
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.ChangePlaybackSpeed(factor = factor))
                    }
                },
                changeVolume = { value ->
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.ChangeVolume(value = value))
                    }
                },
                toggleMute = { isMuted ->
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.ToggleMute(isMuted = isMuted))
                    }
                },
                play = {
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.Play)
                    }
                },
                pause = {
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.Pause)
                    }
                },
                resume = {
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.Resume)
                    }
                },
                stop = {
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.Stop)
                    }
                },
                seekTo = { timestamp ->
                    coroutineScope.launch {
                        feature.execute(PreviewCommand.Playback.SeekTo(timestamp = timestamp))
                    }
                },
                content = content
            )
        }
    }

    exceptions.firstOrNull()?.let { exception ->
        BasicAlertDialog(onDismissRequest = { exceptions.removeFirstOrNull() }, content = {
            Surface(
                modifier = Modifier.wrapContentWidth().wrapContentHeight(), shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        space = 8.dp, alignment = Alignment.CenterVertically
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp, alignment = Alignment.CenterHorizontally
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("An error occurred", style = MaterialTheme.typography.labelLarge)

                        Icon(Icons.Default.Error, null)
                    }

                    Text(exception.localizedMessage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        })
    }

    if (state.isDragAndDropActive) {
        Box(
            modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Upload,
                null,
                modifier = Modifier.fillMaxSize(.25f),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
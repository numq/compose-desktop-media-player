package io.github.numq.cdmp.navigation

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.numq.cdmp.location.LocationStatus
import io.github.numq.cdmp.navigation.tab.JfxNavigationTab
import io.github.numq.cdmp.navigation.tab.KlarityNavigationTab
import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.navigation.tab.VlcjNavigationTab
import io.github.numq.cdmp.preview.PreviewEvent
import io.github.numq.cdmp.rendering.RenderBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.net.URI
import java.nio.file.Paths

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NavigationView(feature: NavigationFeature = koinInject()) {
    val coroutineScope = rememberCoroutineScope { Dispatchers.Default }

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
                    feature.execute(NavigationCommand.Interaction.SetDragAndDropActive)
                }
            }

            override fun onEnded(event: DragAndDropEvent) {
                super.onEnded(event)

                coroutineScope.launch {
                    feature.execute(NavigationCommand.Interaction.SetDragAndDropInactive)
                }
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                (event.dragData() as? DragData.FilesList)?.readFiles()?.lastOrNull()?.let { location ->
                    coroutineScope.launch {
                        feature.execute(
                            NavigationCommand.Location.StartUploading(
                                location = Paths.get(URI.create(location)).toAbsolutePath().toString()
                            )
                        )
                    }
                }

                return true
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize().dragAndDropTarget(shouldStartDragAndDrop = { event ->
        event.dragData() is DragData.FilesList
    }, target = dropTarget), topBar = {
        TopAppBar(title = {
            Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = null, indication = null, onClick = {
                coroutineScope.launch {
                    feature.execute(NavigationCommand.Location.StartUnloading)
                }
            }, enabled = state.locationStatus is LocationStatus.Uploaded), contentAlignment = Alignment.CenterStart) {
                when (val locationStatus = state.locationStatus) {
                    is LocationStatus.Uploaded -> Text(
                        locationStatus.location,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    else -> Unit
                }
            }
        }, actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp, alignment = Alignment.CenterHorizontally
                ), verticalAlignment = Alignment.CenterVertically
            ) {
                Text(RenderBackend.SKIA.displayName)
                Switch(
                    checked = state.tab != NavigationTab.KLARITY && state.renderBackend == RenderBackend.AWT,
                    onCheckedChange = { isChecked ->
                        coroutineScope.launch {
                            val renderBackend = when {
                                isChecked -> RenderBackend.AWT

                                else -> RenderBackend.SKIA
                            }

                            feature.execute(NavigationCommand.SelectRenderBackend(renderBackend = renderBackend))
                        }
                    },
                    enabled = state.tab != NavigationTab.KLARITY
                )
                Text(RenderBackend.AWT.displayName)
            }
        })
    }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (state.locationStatus) {
                    is LocationStatus.Uploaded -> when (state.tab) {
                        NavigationTab.JFX -> JfxNavigationTab()

                        NavigationTab.VLCJ -> VlcjNavigationTab()

                        NavigationTab.KLARITY -> KlarityNavigationTab()
                    }

                    is LocationStatus.Empty -> Text(
                        "Drag your media file here",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    else -> CircularProgressIndicator()
                }
            }
            TabRow(selectedTabIndex = state.tab.ordinal, modifier = Modifier.fillMaxWidth(), tabs = {
                NavigationTab.entries.forEach { tab ->
                    Tab(selected = state.tab == tab, onClick = {
                        coroutineScope.launch {
                            feature.execute(NavigationCommand.NavigateTo(tab))
                        }
                    }, text = {
                        Text(tab.name, style = MaterialTheme.typography.labelLarge)
                    }, enabled = state.tab != tab)
                }
            })
        }
    }

    exceptions.firstOrNull()?.let { exception ->
        BasicAlertDialog(onDismissRequest = { exceptions.removeFirstOrNull() }, content = {
            Surface(
                modifier = Modifier.wrapContentSize(), shape = MaterialTheme.shapes.large
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
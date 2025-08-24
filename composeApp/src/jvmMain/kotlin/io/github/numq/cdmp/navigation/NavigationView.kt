package io.github.numq.cdmp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.numq.cdmp.navigation.tab.JfxNavigationTab
import io.github.numq.cdmp.navigation.tab.KlarityNavigationTab
import io.github.numq.cdmp.navigation.tab.NavigationTab
import io.github.numq.cdmp.navigation.tab.VlcjNavigationTab
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun NavigationView(feature: NavigationFeature = koinInject()) {
    val coroutineScope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (state.tab) {
                NavigationTab.JFX -> JfxNavigationTab(
                    renderTargetType = state.renderTargetType,
                    onRenderTargetTypeChange = { renderTargetType ->
                        coroutineScope.launch {
                            feature.execute(NavigationCommand.ChangeRenderTargetType(renderTargetType = renderTargetType))
                        }
                    })

                NavigationTab.VLCJ -> VlcjNavigationTab(
                    renderTargetType = state.renderTargetType,
                    onRenderTargetTypeChange = { renderTargetType ->
                        coroutineScope.launch {
                            feature.execute(NavigationCommand.ChangeRenderTargetType(renderTargetType = renderTargetType))
                        }
                    })

                NavigationTab.KLARITY -> KlarityNavigationTab(renderTargetType = state.renderTargetType)
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
                })
            }
        })
    }
}
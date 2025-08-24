package io.github.numq.cdmp.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// todo

private fun getUsageColor(usage: Double) = when {
    usage > 0.9 -> Color.Red

    usage > 0.7 -> Color.Yellow

    else -> Color.Green
}

@Composable
fun PerformanceDashboard(
    modifier: Modifier = Modifier,
    metrics: PerformanceMetrics,
) {
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = .5f)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "CPU: ${"%.1f".format(metrics.cpuUsage.systemWideUsage * 100)}%",
            color = getUsageColor(metrics.cpuUsage.systemWideUsage)
        )

        val ramUsage = metrics.memoryUsage.usedSystemMemory.toDouble() / metrics.memoryUsage.totalSystemMemory

        Text("RAM: ${"%.1f".format(ramUsage * 100)}%", color = getUsageColor(ramUsage))
    }
}
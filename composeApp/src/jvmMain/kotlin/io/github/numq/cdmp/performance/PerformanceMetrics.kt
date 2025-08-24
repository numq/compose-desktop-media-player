package io.github.numq.cdmp.performance

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class PerformanceMetrics(
    val cpuUsage: CpuMetrics,
    val memoryUsage: MemoryMetrics,
    val timestamp: Duration = System.currentTimeMillis().milliseconds,
) {
    data class CpuMetrics(
        val systemWideUsage: Double,
        val processUsage: Double?,
    )

    data class MemoryMetrics(
        val totalSystemMemory: Long,
        val usedSystemMemory: Long,
        val processMemory: ProcessMemory,
    ) {
        data class ProcessMemory(
            val heapUsed: Long,
            val heapMax: Long,
            val nativeUsed: Long? = null,
        )
    }
}
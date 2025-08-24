package io.github.numq.cdmp.performance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.lang.management.ManagementFactory

class SystemPerformanceMonitor : PerformanceMonitor {
    private val mxBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean

    private val memoryBean = ManagementFactory.getMemoryMXBean()

    private fun collectCpuMetrics() = PerformanceMetrics.CpuMetrics(
        systemWideUsage = mxBean.cpuLoad.coerceIn(0.0, 1.0),
        processUsage = mxBean.processCpuLoad.takeIf { it >= 0 }?.coerceIn(0.0, 1.0)
    )

    private fun collectMemoryMetrics(): PerformanceMetrics.MemoryMetrics {
        val heap = memoryBean.heapMemoryUsage

        val nonHeap = memoryBean.nonHeapMemoryUsage

        return PerformanceMetrics.MemoryMetrics(
            totalSystemMemory = mxBean.totalMemorySize,
            usedSystemMemory = mxBean.totalMemorySize - mxBean.freeMemorySize,
            processMemory = PerformanceMetrics.MemoryMetrics.ProcessMemory(
                heapUsed = heap.used,
                heapMax = heap.max,
                nativeUsed = nonHeap.used
            )
        )
    }

    override fun getMetrics() = runCatching {
        flow {
            while (currentCoroutineContext().isActive) {
                emit(
                    PerformanceMetrics(
                        cpuUsage = collectCpuMetrics(),
                        memoryUsage = collectMemoryMetrics()
                    )
                )
                delay(500)
            }
        }.flowOn(Dispatchers.IO)
    }
}
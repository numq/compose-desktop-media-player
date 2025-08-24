package io.github.numq.cdmp.performance

import kotlinx.coroutines.flow.Flow

interface PerformanceMonitor {
    fun getMetrics(): Result<Flow<PerformanceMetrics>>
}
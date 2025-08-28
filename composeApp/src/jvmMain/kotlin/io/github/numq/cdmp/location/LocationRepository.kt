package io.github.numq.cdmp.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface LocationRepository {
    val location: StateFlow<String?>

    suspend fun uploadLocation(location: String): Result<Unit>

    suspend fun unloadLocation(): Result<Unit>

    class Default : LocationRepository {
        private val _location = MutableStateFlow<String?>(null)

        override val location = _location.asStateFlow()

        override suspend fun uploadLocation(location: String) = runCatching { _location.value = location }

        override suspend fun unloadLocation() = runCatching { _location.value = null }
    }
}
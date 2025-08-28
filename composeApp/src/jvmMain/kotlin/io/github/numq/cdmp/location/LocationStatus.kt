package io.github.numq.cdmp.location

sealed interface LocationStatus {
    data object Empty : LocationStatus

    data object Uploading : LocationStatus

    data class Uploaded(val location: String) : LocationStatus

    data object Unloading : LocationStatus
}
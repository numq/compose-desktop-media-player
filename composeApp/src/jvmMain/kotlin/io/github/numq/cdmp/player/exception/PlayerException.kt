package io.github.numq.cdmp.player.exception

// todo

data class PlayerException(override val cause: Throwable) : Exception(cause)
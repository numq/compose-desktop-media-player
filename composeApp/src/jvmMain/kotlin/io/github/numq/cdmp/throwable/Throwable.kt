package io.github.numq.cdmp.throwable

val Throwable.exception: Exception
    get() = when (this) {
        is Exception -> this

        else -> Exception(this)
    }
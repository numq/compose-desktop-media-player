package io.github.numq.cdmp.player

import kotlin.time.Duration

data class PlayerMedia(val id: String, val location: String, val width: Int?, val height: Int?, val duration: Duration)
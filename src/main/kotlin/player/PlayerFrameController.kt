package player

import kotlinx.coroutines.flow.StateFlow

interface PlayerFrameController : PlayerController {
    val frameBytes: StateFlow<ByteArray?>
}
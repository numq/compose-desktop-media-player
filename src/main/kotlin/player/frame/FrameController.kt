package player.frame

import player.PlayerController
import kotlinx.coroutines.flow.StateFlow

interface FrameController : PlayerController {
    val frameBytes: StateFlow<ByteArray?>
}
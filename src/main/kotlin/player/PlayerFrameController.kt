package player

import controller.PlayerController
import kotlinx.coroutines.flow.StateFlow

interface PlayerFrameController : PlayerController {
    val frameBytes: StateFlow<ByteArray?>
}
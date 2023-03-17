package player.component

import player.PlayerController
import java.awt.Component

interface ComponentController : PlayerController {
    val component: Component
}
package player

import controller.PlayerController
import java.awt.Component

interface PlayerComponentController : PlayerController {
    val component: Component
}
package player

import java.awt.Component

interface PlayerComponentController : PlayerController {
    val component: Component
}
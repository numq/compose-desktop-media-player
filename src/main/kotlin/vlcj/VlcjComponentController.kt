package vlcj

import controller.PlayerController
import renderer.ComponentRenderer

class VlcjComponentController constructor(
    private val controller: VlcjController = VlcjController(),
) : ComponentRenderer, PlayerController by controller {
    override val component by lazy { controller.embeddedComponent }
}
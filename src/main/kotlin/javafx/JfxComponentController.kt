package javafx

import controller.PlayerController
import exception.catch
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import renderer.ComponentRenderer
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class JfxComponentController constructor(
    private val controller: JfxController = JfxController(),
) : ComponentRenderer, PlayerController by controller {

    override val component by lazy { JFXPanel() }

    override fun load(url: String) = catch {
        with(controller) {
            onLoad = { view ->
                Platform.runLater {
                    view.mediaPlayer?.run {
                        val width = media.width
                        val height = media.height
                        component.preferredSize = Dimension(width, height)
                        component.addComponentListener(object : ComponentAdapter() {
                            override fun componentResized(e: ComponentEvent?) {
                                view.fitWidth = component.width.toDouble()
                                view.fitHeight = component.height.toDouble()
                            }
                        })
                        val stackPane = StackPane(view)
                        val scene = Scene(stackPane)
                        component.scene = scene
                    }
                }
            }
            load(url)
        }
    }
}
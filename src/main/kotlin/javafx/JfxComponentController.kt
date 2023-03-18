package javafx

import exception.catch
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaView
import player.PlayerController
import player.component.ComponentRenderer
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class JfxComponentController constructor(
    private val controller: JfxController = JfxController(),
) : ComponentRenderer, PlayerController by controller {

    private val view by lazy { MediaView() }

    override val component by lazy { JFXPanel() }

    override fun load(url: String) = catch {
        controller.load(url)
        controller.player?.run {

            view.mediaPlayer?.stop()
            view.mediaPlayer?.dispose()
            view.mediaPlayer = this

            Platform.runLater {
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
}
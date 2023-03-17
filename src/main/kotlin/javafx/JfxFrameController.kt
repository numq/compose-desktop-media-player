package javafx

import com.sun.media.jfxmedia.MediaPlayer
import com.sun.media.jfxmedia.control.VideoDataBuffer
import com.sun.media.jfxmedia.control.VideoFormat
import com.sun.media.jfxmedia.events.NewFrameEvent
import com.sun.media.jfxmedia.events.VideoRendererListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import player.PlayerController
import renderer.FrameRenderer
import java.nio.ByteBuffer

class JfxFrameController constructor(
    private val controller: JfxController = JfxController(),
) : FrameRenderer, PlayerController by controller {

    private var embeddedPlayer: MediaPlayer? = null

    private val rendererListener = object : VideoRendererListener {

        override fun videoFrameUpdated(event: NewFrameEvent?) {
            try {
                event?.frameData?.run {
                    convertToFormat(VideoFormat.BGRA_PRE)?.apply {
                        val buffer = getBufferForPlane(VideoDataBuffer.PACKED_FORMAT_PLANE)
                        val pixelCount = encodedHeight * encodedWidth
                        val byteBuffer = ByteBuffer.allocate(pixelCount * 4)
                        buffer.get(byteBuffer.array(), 0, pixelCount * 4)
                        _size.value = width to height
                        _bytes.value = byteBuffer.array()
                    }
                    releaseFrame()
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            }
        }

        override fun releaseVideoFrames() {
            _size.value = 0 to 0
            _bytes.value = null
        }
    }

    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    override fun load(url: String) {
        controller.load(url)
        controller.player?.run {
            embeddedPlayer = javaClass.getDeclaredMethod("retrieveJfxPlayer").apply {
                isAccessible = true
            }.invoke(this) as? MediaPlayer
            setOnReady {
                embeddedPlayer?.videoRenderControl?.addVideoRendererListener(rendererListener)
            }
            setOnHalted {
                embeddedPlayer?.videoRenderControl?.removeVideoRendererListener(rendererListener)
            }
        }
    }
}
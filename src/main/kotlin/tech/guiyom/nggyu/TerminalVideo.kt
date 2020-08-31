package tech.guiyom.nggyu

import tech.guiyom.anscapes.Anscapes
import tech.guiyom.anscapes.renderer.ImageRenderer
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.io.Closeable
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TerminalVideo(private val renderer: ImageRenderer, mrl: String) : Closeable {
    private val factory = MediaPlayerFactory()
    private val media = factory.media().newMediaRef(mrl)
    private val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val bfc = object : BufferFormatCallbackAdapter() {
        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            return RV32BufferFormat(sourceWidth, sourceHeight)
        }
    }

    init {
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun finished(mediaPlayer: MediaPlayer) {
                lock.withLock { condition.signal() }
            }
        })
    }

    @Throws(InterruptedException::class)
    fun renderTo(out: PrintWriter) {
        val rc = TVPRenderCallback(out, renderer)
        player.videoSurface().set(factory.videoSurfaces().newVideoSurface(bfc, rc, false))
        player.videoSurface().attachVideoSurface()
        //player.media().play("null")
        //player.controls().stop()
        out.write(Anscapes.CLEAR)
        player.media().start(media)

        // Wait for media to finish playing
        lock.withLock { condition.await() }

        media.release()
    }

    private class TVPRenderCallback(private val out: PrintWriter, private val renderer: ImageRenderer) : RenderCallback {

        private var buffer: IntArray? = null
        private val frameCount = AtomicInteger()

        override fun display(mediaPlayer: MediaPlayer, nativeBuffers: Array<out ByteBuffer>, format: BufferFormat) {
            val startTime = System.nanoTime()

            val size = format.width * format.height
            if (buffer == null || buffer?.size != size) {
                buffer = IntArray(size)
                out.println("new size : ${format.width}*${format.height}")
            }
            nativeBuffers[0].asIntBuffer()[buffer, 0, size]
            renderer.render(buffer, format.width, format.height) { buf, len -> out.write(buf, 0, len) }

            // Workaround for the bloated screen
            if (frameCount.incrementAndGet() == 2) {
                out.print(Anscapes.CLEAR)
            }
            val renderTime = System.nanoTime() - startTime
            out.printf("%d fps / %f ms; frame: %d", 1000000000 / renderTime, renderTime / 1000000f, frameCount.get())
            out.print(Anscapes.cursorPos(1, 1))
            out.flush()
        }
    }

    override fun close() {
        player.release()
        factory.release()
    }
}
package tech.guiyom.nggyu

import tech.guiyom.anscapes.Anscapes
import tech.guiyom.anscapes.AnsiRenderer
import tech.guiyom.anscapes.ColorMode
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.io.Closeable
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TerminalVideo(cm: ColorMode, targetWidth: Int, targetHeight: Int, mrl: String) : Closeable {
    private val factory = MediaPlayerFactory()
    private val media = factory.media().newMediaRef(mrl)
    private val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val renderer: AnsiRenderer = AnsiRenderer(cm, targetWidth, targetHeight)
    private val frameCount = AtomicInteger()
    private var rc: TVPRenderCallback? = null
    private var bfc: TVPBufferFormatCallback? = null

    init {
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun finished(mediaPlayer: MediaPlayer) {
                lock.withLock { condition.signal() }
            }
        })
    }

    @Throws(InterruptedException::class)
    fun renderTo(out: PrintWriter) {
        rc = TVPRenderCallback(out)
        bfc = TVPBufferFormatCallback()
        player.videoSurface().set(factory.videoSurfaces().newVideoSurface(bfc, rc, true))
        player.videoSurface().attachVideoSurface()
        player.media().play("null")
        player.controls().stop()
        out.write(Anscapes.CLEAR)
        player.media().start(media)

        // Wait for media to finish playing
        lock.withLock { condition.await() }

        media.release()
    }

    private inner class TVPBufferFormatCallback : BufferFormatCallbackAdapter() {
        override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
            rc!!.setSize(sourceWidth, sourceHeight)
            return RV32BufferFormat(sourceWidth, sourceHeight)
        }
    }

    private inner class TVPRenderCallback(private val out: PrintWriter) : RenderCallbackAdapter() {
        private var bufferWidth = 0
        private var bufferHeight = 0
        public override fun onDisplay(mediaPlayer: MediaPlayer, buffer: IntArray) {
            val startTime = System.nanoTime()
            renderer.renderDirect(buffer, bufferWidth, bufferHeight) { buf: CharArray?, len: Int? -> out.write(buf, 0, len!!) }

            // Workaround for the bloated screen
            if (frameCount.incrementAndGet() == 2) {
                out.print(Anscapes.CLEAR)
            }
            val renderTime = System.nanoTime() - startTime
            out.printf("%d fps / %f ms; frame: %d", 1000000000 / renderTime, renderTime / 1000000f, frameCount.get())
            out.print(Anscapes.cursorPos(1, 1))
            out.flush()
        }

        fun setSize(width: Int, height: Int) {
            bufferWidth = width
            bufferHeight = height
            setBuffer(IntArray(width * height))
        }
    }

    override fun close() {
        player.release()
        factory.release()
    }
}
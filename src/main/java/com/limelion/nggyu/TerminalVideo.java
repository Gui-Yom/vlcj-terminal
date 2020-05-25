package com.limelion.nggyu;

import tech.guiyom.anscapes.Anscapes;
import tech.guiyom.anscapes.AnsiRenderer;
import tech.guiyom.anscapes.ColorMode;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

public class TerminalVideo {

    private final MediaPlayerFactory factory = new MediaPlayerFactory();
    private final MediaRef media;
    private final EmbeddedMediaPlayer player;
    private final Object lock = new Object();
    private final AnsiRenderer renderer;
    private final AtomicInteger frameCount = new AtomicInteger();
    private TVPRenderCallback rc;
    private TVPBufferFormatCallback bfc;

    public TerminalVideo(ColorMode cm, int targetWidth, int targetHeight, String mrl) {
        renderer = new AnsiRenderer(cm, targetWidth, targetHeight);
        media = factory.media().newMediaRef(mrl);
        player = factory.mediaPlayers().newEmbeddedMediaPlayer();
        player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                new Thread(() -> {
                    synchronized (lock) {
                        lock.notify();
                    }
                }).start();
            }
        });
    }

    public void renderTo(PrintWriter out) throws InterruptedException {

        rc = new TVPRenderCallback(out);
        bfc = new TVPBufferFormatCallback();

        player.videoSurface().set(factory.videoSurfaces().newVideoSurface(bfc, rc, true));
        player.videoSurface().attachVideoSurface();

        player.media().play("null");
        player.controls().stop();
        out.write(Anscapes.CLEAR);
        player.media().start(media);

        synchronized (lock) {
            lock.wait();
        }

        media.release();
        player.release();
        factory.release();
    }

    private class TVPBufferFormatCallback extends BufferFormatCallbackAdapter {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            rc.setSize(sourceWidth, sourceHeight);
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }
    }

    private class TVPRenderCallback extends RenderCallbackAdapter {

        private final PrintWriter out;
        private int bufferWidth = 0;
        private int bufferHeight = 0;

        public TVPRenderCallback(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void onDisplay(MediaPlayer mediaPlayer, int[] buffer) {
            long startTime = System.nanoTime();

            renderer.renderDirect(buffer, bufferWidth, bufferHeight, (buf, len) -> out.write(buf, 0, len));

            // Workaround for the bloated screen
            if (frameCount.incrementAndGet() == 2) {
                out.print(Anscapes.CLEAR);
            }

            long renderTime = System.nanoTime() - startTime;
            out.printf("%d fps / %f ms; frame: %d", 1_000_000_000 / renderTime, renderTime / 1_000_000f, frameCount.get());
            out.print(Anscapes.cursorPos(1, 1));
            out.flush();
        }

        void setSize(int width, int height) {
            this.bufferWidth = width;
            this.bufferHeight = height;
            this.setBuffer(new int[width * height]);
        }
    }
}

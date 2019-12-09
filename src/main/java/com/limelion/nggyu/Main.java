package com.limelion.nggyu;

import com.limelion.anscapes.Anscapes;
import com.limelion.anscapes.ColorMode;
import com.limelion.anscapes.ImgConverter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Main {

    private static int bufferWidth;
    private static int bufferHeight;
    private static int[] data;

    public static void main(String[] args) {

        if (args == null)
            System.exit(-2);
        if (args.length < 4)
            System.exit(-2);

        PrintStream out = System.out;
        System.setOut(new PrintStream(new NullOutputStream()));
        System.setErr(new PrintStream(new NullOutputStream()));

        try {
            Terminal term = TerminalBuilder.builder()
                                    .encoding(StandardCharsets.UTF_8)
                                    .jansi(true)
                                    .system(true)
                                    .name("Terminal Video Player")
                                    .nativeSignals(true)
                                    //.type("windows-vtp")
                                    .signalHandler(Main::handleSignal)
                                    .streams(System.in, out)
                                    .build();

            if (term instanceof DumbTerminal) {
                System.err.println("This terminal isn't supported ! Aborting.");
                System.exit(-1);
            }

            ColorMode cmode = ColorMode.valueOf(args[0]);
            int targetWidth = Integer.parseInt(args[1]);
            int targetHeight = Integer.parseInt(args[2]);

            System.out.print(Anscapes.ALTERNATIVE_SCREEN_BUFFER);

            ImgConverter converter = new ImgConverter(cmode, targetWidth, targetHeight);

            MediaPlayerFactory factory = new MediaPlayerFactory();
            EmbeddedMediaPlayer player = factory.mediaPlayers().newEmbeddedMediaPlayer();
            player.videoSurface().set(new TVPCallbackVideoSurface());

            Media media = factory.media().newMedia(args[3]);
            out.println(media.info().type());
            out.println(media.meta().asMetaData());
            player.media().play(factory.media().newMediaRef(media));

            for (; ; ) {
                if (data != null) {
                    out.print(converter.convertToSequence(data, bufferWidth, bufferHeight));
                    out.print(Anscapes.cursorPos(1, 1));
                }

                if (false)
                    break;
            }

            player.controls().stop();
            player.release();
            factory.release();
            System.out.print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF);
            term.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void handleSignal(Terminal.Signal sig) {

        if (sig.equals(Terminal.Signal.WINCH))
            System.out.print(Anscapes.CLEAR);

        if (sig.equals(Terminal.Signal.INT)) {
            System.out.print(Anscapes.CLEAR);
            System.out.print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF);
            System.exit(0);
        }

        if (sig.equals(Terminal.Signal.QUIT)) {
            System.out.print(Anscapes.CLEAR);
            System.out.print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF);
            System.exit(0);
        }
    }

    public static int getArgb(ByteBuffer buf, int x, int y, int scanlineStride) {
        int index = y * scanlineStride + x * 4;
        int b = buf.get(index) & 0xff;
        int g = buf.get(index + 1) & 0xff;
        int r = buf.get(index + 2) & 0xff;
        int a = buf.get(index + 3) & 0xff;
        if (a > 0x00 && a < 0xff) {
            int halfa = a >> 1;
            r = (r >= a) ? 0xff : (r * 0xff + halfa) / a;
            g = (g >= a) ? 0xff : (g * 0xff + halfa) / a;
            b = (b >= a) ? 0xff : (b * 0xff + halfa) / a;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static class TVPBufferFormatCallback implements BufferFormatCallback {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            bufferWidth = sourceWidth;
            bufferHeight = sourceHeight;

            // This does not need to be done here, but you could set the video surface size to match the native video
            // size

            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            // This is the new magic sauce, the native video buffer is used directly for the image buffer - there is no
            // full-frame buffer copy here
            data = new int[bufferWidth * bufferHeight];
            System.out.println("Allocated buffers : " + data.length);
            System.out.println("buffWidth: " + bufferWidth + ", bufferHeight: " + bufferHeight);
        }
    }

    private static class TVPCallbackVideoSurface extends CallbackVideoSurface {
        TVPCallbackVideoSurface() {
            super(new TVPBufferFormatCallback(),
                    new TVPRenderCallback(),
                    true,
                    VideoSurfaceAdapters.getVideoSurfaceAdapter());
        }
    }

    // This is correct as far as it goes, but we need to use one of the timers to get smooth rendering (the timer is
    // handled by the demo sub-classes)
    private static class TVPRenderCallback implements RenderCallback {
        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            // We only need to tell the pixel buffer which pixels were updated (in this case all of them) - the
            // pre-cached value is used
            //System.out.println("nativeBuffers size : " + nativeBuffers[0].array().length);
            for (int x = 0; x < bufferWidth; ++x)
                for (int y = 0; y < bufferHeight; ++x)
                    data[y * bufferWidth + x] = getArgb(nativeBuffers[0], x, y, bufferWidth);
        }
    }
}

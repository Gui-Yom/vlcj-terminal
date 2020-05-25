package com.limelion.nggyu;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import tech.guiyom.anscapes.Anscapes;
import tech.guiyom.anscapes.ColorMode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    private static Terminal term;

    public static void main(String[] args) {

        if (args == null)
            System.exit(-2);
        if (args.length < 4)
            System.exit(-2);

        // Nullify base output stream (prevents dependency gibberish)
        PrintStream out_ = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));

        try {
            term = TerminalBuilder.builder()
                           .encoding(StandardCharsets.UTF_8)
                           .jansi(true)
                           .system(true)
                           .name("Terminal Video Player")
                           .nativeSignals(true)
                           .signalHandler(Main::handleSignal)
                           .streams(System.in, out_)
                           .build();

            if (term instanceof DumbTerminal) {
                System.err.println("This terminal isn't supported ! Aborting.");
                System.exit(-1);
            }

            ColorMode cmode = ColorMode.valueOf(args[0]);
            int targetWidth = Integer.parseInt(args[1]);
            int targetHeight = Integer.parseInt(args[2]);

            // Let's go private since we'll print thousands of lines a second
            // This also prevent scroll since the terminal buffer will match the window perfectly
            term.writer().print(Anscapes.ALTERNATIVE_SCREEN_BUFFER);

            TerminalVideo video = new TerminalVideo(cmode, targetWidth, targetHeight, args[3]);
            video.renderTo(term.writer());

            term.writer().print(Anscapes.CLEAR_BUFFER);
            term.writer().print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF);
            term.close();

        } catch (IOException | InterruptedException ex) {
            term.writer().println(ex.getLocalizedMessage());
        }
    }

    public static void handleSignal(Terminal.Signal sig) {

        if (sig.equals(Terminal.Signal.WINCH))
            term.writer().print(Anscapes.CLEAR);

        if (sig.equals(Terminal.Signal.INT)) {
            term.writer().print(Anscapes.CLEAR);
            term.writer().print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF);
            System.exit(0);
        }

        if (sig.equals(Terminal.Signal.QUIT)) {
            term.writer().print(Anscapes.CLEAR);
            term.writer().print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF);
            System.exit(0);
        }
    }
}

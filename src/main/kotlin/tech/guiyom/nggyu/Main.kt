package tech.guiyom.nggyu

import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.terminal.impl.DumbTerminal
import tech.guiyom.anscapes.Anscapes
import tech.guiyom.anscapes.ColorMode
import tech.guiyom.anscapes.renderer.AnsiImageRenderer
import tech.guiyom.anscapes.renderer.ImageRenderer
import tech.guiyom.anscapes.renderer.RgbImageRenderer
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 5) exitProcess(-2)

    var term: Terminal? = null
    term = TerminalBuilder.builder()
            .name("Terminal Video Player")
            .encoding(StandardCharsets.UTF_8)
            .jansi(true)
            .system(true)
            .nativeSignals(true)
            //.streams(System.`in`, out_)
            .dumb(true)
            .signalHandler(fun(sig: Terminal.Signal) {
                term?.let {
                    if (sig == Terminal.Signal.WINCH) it.writer().print(Anscapes.CLEAR)
                    if (sig == Terminal.Signal.INT || sig == Terminal.Signal.QUIT) {
                        it.finalize()
                        exitProcess(0)
                    }
                }
            })
            .build()

    if (term == null || term is DumbTerminal) {
        System.err.println("This terminal isn't supported ! Aborting.")
        exitProcess(-1)
    }

    // Nullify base output stream (prevents dependency gibberish)
    //val out_ = System.out
    System.setOut(PrintStream(OutputStream.nullOutputStream()))
    System.setErr(PrintStream(OutputStream.nullOutputStream()))

    try {
        val cmode = ColorMode.valueOf(args[0])
        val bias = args[1].toInt()
        val targetWidth = args[2].toInt()
        val targetHeight = args[3].toInt()

        val renderer: ImageRenderer = if (cmode == ColorMode.RGB) {
            RgbImageRenderer(targetWidth, targetHeight, bias)
        } else {
            AnsiImageRenderer(targetWidth, targetHeight, bias)
        }

        // Let's go private since we'll print thousands of lines a second
        // This also prevent scroll since the terminal buffer will match the window perfectly
        term.writer().print(Anscapes.ALTERNATIVE_SCREEN_BUFFER)
        TerminalVideo(renderer, args[4]).use {
            // Blocks until video ends
            it.renderTo(term.writer())
        }

        term.finalize()
    } catch (ex: IOException) {
        term.finalize()
        term.writer().println(ex.localizedMessage)
    } catch (ex: InterruptedException) {
        term.finalize()
        term.writer().println(ex.localizedMessage)
    } finally {
        term.close()
    }
}

internal fun Terminal.finalize() {
    writer().print(Anscapes.CLEAR_BUFFER)
    writer().print(Anscapes.ALTERNATIVE_SCREEN_BUFFER_OFF)
}

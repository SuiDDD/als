package sui.k.als.tty

import com.termux.terminal.*
import java.util.concurrent.*

internal val ttyIO = Executors.newSingleThreadExecutor()
internal var ttySession: TerminalSession? = null
fun cmd(command: String) {
    ttyIO.execute { ttySession?.write("$command\n") }
}
package sui.k.als.tty

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.util.concurrent.atomic.AtomicBoolean

data class TTYIns(
    val session: TerminalSession,
    val view: TerminalView,
    val initialized: AtomicBoolean = AtomicBoolean(false),
    val isFinished: MutableState<Boolean> = mutableStateOf(false)
)
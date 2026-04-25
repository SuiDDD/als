package sui.k.als

import android.app.Activity
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.tty.TTYIME
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.ui.ALSButton

const val alsPath = "/data/local/tmp/als"

@Composable
fun Hub(modifier: Modifier = Modifier, onFin: () -> Unit) = Box(
    modifier
        .fillMaxSize()
        .background(Color.Black), Alignment.Center
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf<TTYInstance?>(null) }
    var showTTY by remember { mutableStateOf(false) }
    var showApp by remember { mutableStateOf(false) }
    val close = { active?.session?.finishIfRunning(); active = null }
    DisposableEffect(Unit) { onDispose(close) }
    BackHandler { showTTY = false; showApp = false }
    if (showApp) App() else if (showTTY) active?.let { TTYScreen(it) { TTYIME() } } else Box(
        Modifier.fillMaxSize(), Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ALSButton(R.drawable.arrow_forward) { showApp = true }
            ALSButton(R.drawable.terminal) {
                if (active == null) active = createTTYInstance(ctx, object : TTYSessionStub() {
                    override fun onSessionFinished(session: TerminalSession) {
                        showTTY = false; active = null
                    }
                }, object : TTYViewStub() {
                    override fun onSingleTapUp(event: MotionEvent) {
                        active?.view?.run {
                            requestFocus(); ctx.getSystemService(
                            InputMethodManager::class.java
                        )?.showSoftInput(this, 0)
                        }
                    }
                }).also { scope.launch { delay(90); cmd(su); delay(90); cmd("cd $alsPath && clear && busybox") } }
                showTTY = true
            }
            ALSButton(R.drawable.power) { close(); onFin(); (ctx as? Activity)?.finishAffinity() }
        }
    }
}
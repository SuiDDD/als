package sui.k.als.boot

import android.app.Activity
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.*
import sui.k.als.R
import sui.k.als.tty.*
import sui.k.als.ui.ALSButton

const val alsPath = "/data/local/tmp/als"

@Composable
fun Hub(modifier: Modifier = Modifier, onFin: () -> Unit) = Box(modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf<TTYInstance?>(null) }
    var sTTY by remember { mutableStateOf(false) }
    var sApp by remember { mutableStateOf(false) }
    val close = { active?.session?.finishIfRunning(); active = null }
    DisposableEffect(Unit) { onDispose(close) }
    BackHandler(sTTY || sApp) { sTTY = false; sApp = false }
    if (sApp) App() else if (sTTY) active?.let { TTYScreen(it) } else Box(Modifier.fillMaxSize(), Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ALSButton(R.drawable.arrow_forward) { sApp = true }
            ALSButton(R.drawable.terminal) {
                if (active == null) active = createTTYInstance(ctx, object : TTYSessionStub() {
                    override fun onSessionFinished(session: TerminalSession) { sTTY = false; active = null }
                }, object : TTYViewStub() {
                    override fun onSingleTapUp(event: MotionEvent) { active?.view?.run { requestFocus(); ctx.getSystemService(InputMethodManager::class.java)?.showSoftInput(this, 0) } }
                }).also { scope.launch { delay(90); cmd(su); delay(90); cmd("cd $alsPath && clear && busybox") } }
                sTTY = true
            }
            ALSButton(R.drawable.power) { close(); onFin(); (ctx as? Activity)?.finishAffinity() }
        }
    }
}

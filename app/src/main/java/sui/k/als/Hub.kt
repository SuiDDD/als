package sui.k.als

import android.app.*
import android.view.*
import android.view.inputmethod.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import com.termux.terminal.*
import kotlinx.coroutines.*
import sui.k.als.tty.*
import sui.k.als.ui.*

const val alsPath = "/data/local/tmp/als"

@Composable
fun Hub(modifier: Modifier = Modifier, onFin: () -> Unit) = Box(
    modifier
        .fillMaxSize()
        .background(Color.Black), Alignment.Center
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf(emptyList<TTYInstance>()) }
    var active by remember { mutableStateOf<TTYInstance?>(null) }
    var showTTY by remember { mutableStateOf(false) }; var showTTYHUB by remember { mutableStateOf(false) }; var showApp by remember { mutableStateOf(false) }
    val close = { sessions.forEach { it.session.finishIfRunning() }; sessions = emptyList(); active = null }
    val create = {
        val instance = createTTYInstance(ctx, object : TTYSessionStub() {
            override fun onSessionFinished(session: TerminalSession) {
                sessions = sessions.filter { it.session != session }
                if (active?.session == session) active = sessions.lastOrNull()
                if (active == null) { showTTY = false; showTTYHUB = sessions.isNotEmpty() }
            }
        }, object : TTYViewStub() {
            override fun onSingleTapUp(event: MotionEvent) {
                active?.view?.run { requestFocus(); ctx.getSystemService(InputMethodManager::class.java)?.showSoftInput(this, 0) }
            }
        }).also { scope.launch { delay(90); cmd(su); delay(90); cmd("cd $alsPath && clear && busybox") } }
        sessions = sessions + instance; active = instance; showTTY = true; showTTYHUB = false
    }
    DisposableEffect(Unit) { onDispose(close) }
    BackHandler { if (showTTY) { showTTY = false; showTTYHUB = true } else { showTTYHUB = false; showApp = false } }
    if (showApp) App() else if (showTTY) active?.let { TTYScreen(it) { TTYIME() } } else if (showTTYHUB) TTYHUB(sessions, { active = it; showTTY = true; showTTYHUB = false }, create) else Box(
        Modifier.fillMaxSize(), Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ALSButton(R.drawable.arrow_forward) { showApp = true }
            ALSButton(R.drawable.terminal) { if (sessions.isEmpty()) create() else showTTYHUB = true }
            ALSButton(R.drawable.power) { close(); onFin(); (ctx as? Activity)?.finishAffinity() }
        }
    }
}
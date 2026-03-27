package sui.k.als.boot

import android.app.Activity
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.localAppFont
import sui.k.als.tty.TTYIns
import sui.k.als.tty.TTYScreen

@Composable
fun Boot(onFinished: () -> Unit) {
    val context = LocalContext.current
    val font = localAppFont.current
    val scope = rememberCoroutineScope()
    val state = remember { BootState(context) }
    val sessions = remember { mutableStateListOf<Pair<String, TTYIns>>() }
    var active by remember { mutableStateOf<TTYIns?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    var lastVolumePress by remember { mutableLongStateOf(0L) }
    var systemMenuBackground by remember { mutableStateOf(Color.Transparent) }
    val imm =
        remember { context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager }
    val labels = listOf(
        stringResource(R.string.menu_system),
        stringResource(R.string.menu_session),
        stringResource(R.string.menu_exit)
    )
    LaunchedEffect(Unit) { BootSU.init(context) }
    LaunchedEffect(BootSU.path, state.refreshTrigger) { state.initialize() }
    if (state.showEdit) SuPathDialog(context) { state.refreshTrigger++ }
    Box(Modifier
        .fillMaxSize()
        .background(Color.Black)
        .onKeyEvent {
            if (showTerminal && it.nativeKeyEvent.keyCode in listOf(
                    KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN
                )
            ) {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    if (now - lastVolumePress < 500) showTerminal = false
                    lastVolumePress = now
                }
                true
            } else false
        }) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.09f)
            ) {
                BootSysInfoText(state.information, font)
            }
            Column(
                Modifier
                    .weight(0.91f)
                    .fillMaxWidth()
            ) {
                BootMenu(labels[0], font, systemMenuBackground) {
                    BootSys.onClick(
                        context,
                        scope,
                        imm,
                        sessions,
                        labels[0],
                        { active = it },
                        { showTerminal = it },
                        { systemMenuBackground = it })
                }
                BootMenu(labels[1], font) {
                    BootSession.onClick(
                        context,
                        scope,
                        imm,
                        sessions,
                        labels[1],
                        { active = it },
                        { showTerminal = it })
                }
                BootSession.SessionBar(sessions, active, font) {
                    active = it
                    showTerminal = true
                }
                BootMenu(labels[2], font) {
                    sessions.forEach { it.second.session.finishIfRunning() }
                    onFinished()
                    (context as? Activity)?.finishAffinity()
                }
            }
        }
        if (showTerminal && active != null) TTYScreen(active!!)
    }
}
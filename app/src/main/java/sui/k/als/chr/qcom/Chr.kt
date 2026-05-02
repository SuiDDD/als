package sui.k.als.chr.qcom

import android.view.*
import android.view.inputmethod.*
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import com.termux.terminal.*
import kotlinx.coroutines.*
import sui.k.als.*
import sui.k.als.R
import sui.k.als.ide.*
import sui.k.als.tty.*
import sui.k.als.ui.*

private var chrInstance: TTYInstance? = null

@Composable
fun Chr(onTTYCreated: (TTYInstance) -> Unit, scope: CoroutineScope) {
    val context = LocalContext.current
    BackHandler(idePath != null) { ideOpen(null) }
    DisposableEffect(Unit) { onDispose { ideOpen(null) } }
    val configs by remember { mutableStateOf(listOf(mapOf("name" to "Debian_Trixie_20260428"))) }
    val chrTerm = {
        if (chrInstance != null) {
            ttySession = chrInstance!!.session
            onTTYCreated(chrInstance!!)
        } else {
            val sessionStub = object : TTYSessionStub() {
                override fun onSessionFinished(session: TerminalSession) {
                    chrInstance = null
                }
            }
            val viewStub = object : TTYViewStub() {
                override fun onSingleTapUp(event: MotionEvent) {
                    chrInstance?.view?.requestFocus()
                    context.getSystemService(InputMethodManager::class.java)
                        ?.showSoftInput(chrInstance?.view, 0)
                }
            }
            chrInstance = createTTYInstance(context, sessionStub, viewStub)
            onTTYCreated(chrInstance!!)
            scope.launch {
                delay(90)
                ttySession = chrInstance!!.session
                cmd(su)
                cmd("cd $alsDir && txc")
                cmd("pkg install x11-repo -y && pkg install pulseaudio termux-x11 -y && { termux-x11 :1 & } && $su -c ./chr ")
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier
                    .weight(1f)
                    .padding(9.dp),
                contentPadding = PaddingValues(top = 9.dp, bottom = 9.dp)
            ) {
                itemsIndexed(configs) { index, configItem ->
                    ALSList(
                        data = configItem["name"].toString(),
                        first = index == 0,
                        last = index == configs.size - 1,
                        onClick = { },
                        iconContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                ALSButton(R.drawable.edit) { ideOpen("$alsDir/chr.sh") }
                                ALSButton(R.drawable.power) { chrTerm() }
                            }
                        }
                    )
                }
            }
        }
        IDE()
    }
}
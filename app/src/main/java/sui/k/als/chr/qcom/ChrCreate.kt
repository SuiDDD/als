package sui.k.als.chr.qcom

import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.R
import sui.k.als.alsPath
import sui.k.als.su
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.ui.ALSButton

@Composable
fun ChrCreate(onBack: () -> Unit, onTTYCreated: (TTYInstance) -> Unit, scope: CoroutineScope) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize(), Alignment.Center) {
        ALSButton(R.drawable.terminal) {
            val instance = createTTYInstance(context, object : TTYSessionStub() {
                override fun onSessionFinished(session: TerminalSession) {
                    onBack()
                }
            }, object : TTYViewStub() {
                override fun onSingleTapUp(event: MotionEvent) {
                    context.getSystemService(InputMethodManager::class.java)?.showSoftInput(null, 0)
                }
            })
            onTTYCreated(instance)
            scope.launch {
                delay(90)
                cmd(su)
                cmd("$alsPath/txc")
            }
        }
    }
}
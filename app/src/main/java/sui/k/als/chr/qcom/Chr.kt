package sui.k.als.chr.qcom
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.R
import sui.k.als.alsPath
import sui.k.als.localFont
import sui.k.als.su
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.tty.ttySession
import sui.k.als.ui.ALSButton
private var chrInstance: TTYInstance? = null
@Composable
fun Chr(onTTYCreated: (TTYInstance) -> Unit, scope: CoroutineScope) {
    val context = LocalContext.current
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
                    context.getSystemService(InputMethodManager::class.java)?.showSoftInput(chrInstance?.view, 0)
                }
            }
            chrInstance = createTTYInstance(context, sessionStub, viewStub)
            onTTYCreated(chrInstance!!)
            scope.launch {
                delay(90)
                ttySession = chrInstance!!.session
                cmd(su)
                cmd("cd $alsPath && txc")
                cmd("pkg install x11-repo -y && pkg install pulseaudio termux-x11 -y && { termux-x11 :1 & } && $su -c ./chr ")
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
            contentPadding = PaddingValues(top = 9.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(configs) { configItem ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Color(0xFF111111)).padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = configItem["name"].toString(),
                        modifier = Modifier.weight(1f).padding(start = 3.dp),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = localFont.current
                    )
                    ALSButton(R.drawable.power) { chrTerm() }
                }
            }
        }
    }
}
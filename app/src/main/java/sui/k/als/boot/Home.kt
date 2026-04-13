package sui.k.als.boot
import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sui.k.als.R
import sui.k.als.localFont
import sui.k.als.tty.*
import sui.k.als.vm.VM
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
const val alsPath = "/data/local/tmp/als/"
const val markFile = "${alsPath}als260406"
@Composable
fun Home(onFinished: () -> Unit) {
    val ctx = LocalContext.current
    val font = localFont.current
    val scope = rememberCoroutineScope()
    val sessions = remember { mutableStateListOf<Pair<String, TTYInstance>>() }
    var activeSession by remember { mutableStateOf<TTYInstance?>(null) }
    var showTTY by remember { mutableStateOf(false) }
    var showVM by remember { mutableStateOf(false) }
    val ubuntuOrange = Color(0xFFE95420)
    val menuSys = stringResource(R.string.system)
    val menuSession = stringResource(R.string.session)
    val menuExit = stringResource(R.string.exit)
    val closeAll = { sessions.onEach { it.second.session.finishIfRunning() }.clear() }
    DisposableEffect(Unit) { onDispose { closeAll() } }
    BackHandler(showTTY || showVM) {
        if (showTTY) showTTY = false
        else if (showVM) showVM = false
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val check = ProcessBuilder(su, "-c", "[ -f $markFile ] && echo 1 || echo 0").start().inputStream.bufferedReader().readText().trim()
            if (check != "1") {
                val bbTmp = File(ctx.cacheDir, "busybox")
                ctx.assets.open("busybox").use { i -> FileOutputStream(bbTmp).use { o -> i.copyTo(o) } }
                val tarTmp = File(ctx.cacheDir, "01.tar.xz")
                val conn = URL("https://github.com/SuiDDD/als/releases/download/26.4.13R2/01.tar.xz").openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.inputStream.use { i -> FileOutputStream(tarTmp).use { o -> i.copyTo(o) } }
                val script = "mkdir -p $alsPath && cp ${bbTmp.absolutePath} ${alsPath}busybox && cp ${tarTmp.absolutePath} ${alsPath}01.tar.xz && chmod 755 ${alsPath}busybox && cd $alsPath && ./busybox tar -xJf 01.tar.xz && touch $markFile && rm 01.tar.xz && rm ${bbTmp.absolutePath} && rm ${tarTmp.absolutePath}"
                ProcessBuilder(su, "-c", script).start().waitFor()
            }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Splash {
            Column(Modifier.fillMaxSize()) {
                MenuLine(menuSys, font, Color.White) { showVM = true; showTTY = false }
                Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    val interaction = remember { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    Text(menuSession, color = if (isPressed) ubuntuOrange else Color.White, fontSize = 9.sp, fontFamily = font, modifier = Modifier.clickable(interaction, null) {
                        val instance = createTTYInstance(ctx, object : TTYSessionStub() {
                            override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                sessions.removeAll { it.second.session == session }
                                if (activeSession?.session == session) { showTTY = false; activeSession = null }
                            }
                        }, object : TTYViewStub() {
                            override fun onSingleTapUp(event: MotionEvent) {
                                activeSession?.view?.requestFocus()
                                (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(activeSession?.view, 0)
                            }
                        })
                        sessions.add("$menuSession ${sessions.count { it.first.contains(menuSession) } + 1}" to instance)
                        activeSession = instance; showTTY = true; showVM = false
                        scope.launch { delay(100); cmd(su); delay(100); cmd("cd $alsPath && clear && ./busybox") }
                    })
                    Spacer(Modifier.width(9.dp))
                    LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        items(sessions) { item ->
                            val sInt = remember { MutableInteractionSource() }
                            val sPre by sInt.collectIsPressedAsState()
                            Text(item.first, fontSize = 9.sp, fontFamily = font, color = if (sPre || activeSession == item.second) ubuntuOrange else Color.Gray, modifier = Modifier.clickable(sInt, null) { activeSession = item.second; showTTY = true; showVM = false })
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                MenuLine(menuExit, font, Color.White) { closeAll(); onFinished(); (ctx as? Activity)?.finishAffinity() }
            }
        }
        if (showVM) VM(onExit = { showVM = false })
        if (showTTY) activeSession?.let { TTYScreen(it) }
    }
}
@Composable
fun MenuLine(text: String, font: androidx.compose.ui.text.font.FontFamily, baseColor: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Box(Modifier.fillMaxWidth().height(18.dp).clickable(interaction, null) { onClick() }, Alignment.CenterStart) {
        Text(text, color = if (isPressed) Color(0xFFE95420) else baseColor, fontSize = 9.sp, fontFamily = font)
    }
}
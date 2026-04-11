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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import sui.k.als.localAppFont
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.vm.VM
import java.io.File
import java.io.FileOutputStream

const val alsPath = "/data/local/tmp/als/"
const val markFile = "${alsPath}als260406"

@Composable
fun Home(onFinished: () -> Unit) {
    val ctx = LocalContext.current
    val font = localAppFont.current
    val scope = rememberCoroutineScope()
    var systemInfo by remember { mutableStateOf("") }
    val sessions = remember { mutableStateListOf<Pair<String, TTYInstance>>() }
    var activeSession by remember { mutableStateOf<TTYInstance?>(null) }

    var showTTY by remember { mutableStateOf(false) }
    var showVM by remember { mutableStateOf(false) }

    val ubuntuOrange = Color(0xFFE95420)
    val menuSys = stringResource(R.string.menu_system)
    val menuSession = stringResource(R.string.menu_session)
    val menuExit = stringResource(R.string.menu_exit)

    DisposableEffect(Unit) {
        onDispose {
            sessions.forEach { it.second.session.finishIfRunning() }
            sessions.clear()
        }
    }

    BackHandler(showTTY || showVM) {
        showTTY = false
        showVM = false
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val infoCmd = "echo \"$(uname -m)\\n$(/system/bin/getenforce)\\n$(df /data | awk 'NR==2 {printf \"%.2f GB\", $4/1024/1024}') Free\\n$(cat /sys/class/power_supply/battery/capacity)%\\n$(uname -r)\""
            systemInfo = try {
                ProcessBuilder(su, "-c", infoCmd).start().inputStream.bufferedReader().readText()
            } catch (_: Exception) { "" }

            val hasMark = ProcessBuilder(su, "-c", "[ -f $markFile ] && echo 1 || echo 0").start().inputStream.bufferedReader().readText().trim() == "1"
            if (!hasMark) {
                val assetNames = listOf("busybox", "01.tar.xz")
                val tmpPaths = assetNames.map { name ->
                    val tmp = File(ctx.cacheDir, name)
                    ctx.assets.open(name).use { input -> FileOutputStream(tmp).use { output -> input.copyTo(output) } }
                    tmp.absolutePath
                }
                val script = "mkdir -p $alsPath && cp ${tmpPaths[0]} ${alsPath}busybox && cp ${tmpPaths[1]} ${alsPath}01.tar.xz && chmod 755 ${alsPath}busybox && cd $alsPath && ./busybox tar -xJf 01.tar.xz && touch $markFile && rm ${tmpPaths.joinToString(" ")}"
                ProcessBuilder(su, "-c", script).start().waitFor()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Splash(header = {
            Text(
                text = systemInfo,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                color = Color.Black,
                fontSize = 9.sp,
                fontFamily = font,
                lineHeight = 11.sp
            )
        }, content = {
            Column(modifier = Modifier.fillMaxSize()) {
                MenuLine(menuSys, font, Color.Black) {
                    showVM = true
                    showTTY = false
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    Text(
                        text = menuSession,
                        color = if (isPressed) ubuntuOrange else Color.Black,
                        fontSize = 9.sp,
                        fontFamily = font,
                        modifier = Modifier.clickable(interactionSource = interactionSource, indication = null) {
                            val count = sessions.count { it.first.contains(menuSession) } + 1
                            val instance = createTTYInstance(ctx, object : TTYSessionStub() {
                                override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                    sessions.removeAll { it.second.session == session }
                                    if (activeSession?.session == session) {
                                        showTTY = false
                                        activeSession = null
                                    }
                                }
                            }, object : TTYViewStub() {
                                override fun onSingleTapUp(event: MotionEvent) {
                                    activeSession?.view?.requestFocus()
                                    (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(activeSession?.view, 0)
                                }
                            })
                            sessions.add("$menuSession $count" to instance)
                            activeSession = instance
                            showTTY = true
                            showVM = false
                            scope.launch {
                                delay(100)
                                cmd(su)
                                delay(100)
                                cmd("cd $alsPath && clear && ./busybox")
                            }
                        })

                    Spacer(modifier = Modifier.width(9.dp))

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(sessions) { item ->
                            val sSource = remember { MutableInteractionSource() }
                            val sPressed by sSource.collectIsPressedAsState()
                            Text(
                                text = item.first,
                                color = when {
                                    sPressed -> ubuntuOrange
                                    activeSession == item.second -> ubuntuOrange
                                    else -> Color.Gray
                                },
                                fontSize = 9.sp,
                                fontFamily = font,
                                modifier = Modifier.clickable(interactionSource = sSource, indication = null) {
                                    activeSession = item.second
                                    showTTY = true
                                    showVM = false
                                })
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                MenuLine(menuExit, font, Color.Black) {
                    sessions.forEach { it.second.session.finishIfRunning() }
                    onFinished()
                    (ctx as? Activity)?.finishAffinity()
                }
            }
        })

        if (showVM) VM()
        if (showTTY && activeSession != null) TTYScreen(activeSession!!)
    }
}

@Composable
fun MenuLine(text: String, font: androidx.compose.ui.text.font.FontFamily, baseColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier.fillMaxWidth().height(18.dp).clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, color = if (isPressed) Color(0xFFE95420) else baseColor, fontSize = 9.sp, fontFamily = font)
    }
}
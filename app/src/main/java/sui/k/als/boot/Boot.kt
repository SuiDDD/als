package sui.k.als.boot

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.onKeyEvent
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
import java.io.File
import java.io.FileOutputStream

const val alsPath = "/data/local/tmp/als/"

@Composable
fun BootScreen(onFinished: () -> Unit) {
    val ctx = LocalContext.current
    val font = localAppFont.current
    val scope = rememberCoroutineScope()
    var systemInfo by remember { mutableStateOf("") }
    val sessions = remember { mutableStateListOf<Pair<String, TTYInstance>>() }
    var activeSession by remember { mutableStateOf<TTYInstance?>(null) }
    var showTTY by remember { mutableStateOf(false) }
    var lastVolClick by remember { mutableLongStateOf(0L) }
    var systemMenuBg by remember { mutableStateOf(Color.Transparent) }
    val ubuntuOrange = Color(0xFFE95420)
    val menuSys = stringResource(R.string.menu_system)
    val menuSession = stringResource(R.string.menu_session)
    val menuExit = stringResource(R.string.menu_exit)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val infoCmd =
                "echo \"$(uname -m)\\n$(/system/bin/getenforce)\\n$(df /data | awk 'NR==2 {printf \"%.2f GB\", $4/1024/1024}') Free\\n$(cat /sys/class/power_supply/battery/capacity)%\\n$(uname -r)\""
            systemInfo = try {
                ProcessBuilder("su", "-c", infoCmd).start().inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                "ROOT_ERR"
            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .onKeyEvent {
            if (showTTY && (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_UP || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    if (now - lastVolClick < 500) showTTY = false
                    lastVolClick = now
                }
                true
            } else false
        }) {
        Splash(header = {
            Text(
                text = systemInfo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                color = ubuntuOrange,
                fontSize = 9.sp,
                fontFamily = font,
                lineHeight = 11.sp
            )
        }, content = {
            Column(modifier = Modifier.fillMaxSize()) {
                MenuLine(menuSys, font, systemMenuBg) {
                    scope.launch(Dispatchers.IO) {
                        val hasGunyah = ProcessBuilder(
                            "su", "-c", "[ -e /dev/gunyah ] && echo 1 || echo 0"
                        ).start().inputStream.bufferedReader().readText().trim() == "1"
                        if (!hasGunyah) {
                            systemMenuBg = Color.Red; return@launch
                        }
                        val exists = ProcessBuilder(
                            "su", "-c", "[ -f ${alsPath}i ] && echo 1 || echo 0"
                        ).start().inputStream.bufferedReader().readText().trim() == "1"
                        if (!exists) {
                            ProcessBuilder(
                                "su", "-c", "mkdir -p $alsPath && chmod 777 $alsPath"
                            ).start().waitFor()
                            listOf("busybox", "01.tar.xz").forEach { name ->
                                val tmp = File(ctx.cacheDir, name)
                                ctx.assets.open(name).use { input ->
                                    FileOutputStream(tmp).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                ProcessBuilder(
                                    "su",
                                    "-c",
                                    "cp ${tmp.absolutePath} $alsPath$name && chmod 755 $alsPath$name"
                                ).start().waitFor()
                                tmp.delete()
                            }
                            ProcessBuilder(
                                "su", "-c", "cd $alsPath && ./busybox tar -xJf 01.tar.xz"
                            ).start().waitFor()
                        }
                        withContext(Dispatchers.Main) {
                            val instance = createTTYInstance(
                                ctx,
                                object : TTYSessionStub() {},
                                object : TTYViewStub() {
                                    override fun onSingleTapUp(e: MotionEvent) {
                                        activeSession?.view?.requestFocus()
                                        (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                                            activeSession?.view, 0
                                        )
                                    }
                                })
                            sessions.add(menuSys to instance)
                            activeSession = instance
                            showTTY = true
                            scope.launch { delay(180); cmd("clear && cd $alsPath && ./i") }
                        }
                    }
                }
                MenuLine(menuSession, font, Color.Transparent) {
                    val instance = createTTYInstance(
                        ctx,
                        object : TTYSessionStub() {},
                        object : TTYViewStub() {
                            override fun onSingleTapUp(e: MotionEvent) {
                                activeSession?.view?.requestFocus()
                                (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                                    activeSession?.view, 0
                                )
                            }
                        })
                    sessions.add("$menuSession#${sessions.size}" to instance)
                    activeSession = instance
                    showTTY = true
                }
                if (sessions.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(sessions) { _, item ->
                            val (name, instance) = item
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(18.dp)
                                    .background(
                                        if (activeSession == instance) ubuntuOrange else Color(
                                            0xFF222222
                                        )
                                    )
                                    .clickable { activeSession = instance; showTTY = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name,
                                    color = if (activeSession == instance) Color.Black else Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = font
                                )
                            }
                        }
                    }
                }
                MenuLine(menuExit, font, Color.Transparent) {
                    sessions.forEach { it.second.session.finishIfRunning() }
                    onFinished()
                    (ctx as? Activity)?.finishAffinity()
                }
            }
        })
        if (showTTY && activeSession != null) TTYScreen(activeSession!!)
    }
}

@Composable
fun MenuLine(
    text: String,
    font: androidx.compose.ui.text.font.FontFamily,
    customBg: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .background(if (isPressed) Color(0xFFE95420) else customBg, RectangleShape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (isPressed || customBg == Color.Red) Color.Black else Color.White,
            fontSize = 9.sp,
            fontFamily = font
        )
    }
}
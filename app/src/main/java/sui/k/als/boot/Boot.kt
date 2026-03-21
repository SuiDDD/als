package sui.k.als.boot
import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import sui.k.als.tty.*
import java.io.File
import java.io.FileOutputStream
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
    val ubuntuOrange = Color(0xFFE95420)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dir = File("/data/als")
            if (!dir.exists()) ProcessBuilder("su", "-c", "mkdir -p /data/als && chmod 777 /data/als").start().waitFor()
            val check = ProcessBuilder("su", "-c", "[ -f /data/als/busybox ] && echo 1 || echo 0").start().inputStream.bufferedReader().readText().trim() == "1"
            if (!check) {
                listOf("busybox", "01.tar.xz").forEach { name ->
                    val tmp = File(ctx.cacheDir, name)
                    ctx.assets.open(name).use { input -> FileOutputStream(tmp).use { output -> input.copyTo(output) } }
                    ProcessBuilder("su", "-c", "cp ${tmp.absolutePath} /data/als/$name && chmod 755 /data/als/$name").start().waitFor()
                    tmp.delete()
                }
            }
            val infoCmd = "echo \"$(uname -m)\\n$(/system/bin/getenforce)\\n$(df /data | awk 'NR==2 {printf \"%.2f GB\", $4/1024/1024}') Free\\n$(cat /sys/class/power_supply/battery/capacity)%\\n$(uname -r)\""
            systemInfo = try { ProcessBuilder("su", "-c", infoCmd).start().inputStream.bufferedReader().readText() } catch (_: Exception) { "ROOT_ERR" }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).onKeyEvent {
        if (showTTY && (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_UP || it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastVolClick < 500) showTTY = false
                lastVolClick = now
            }
            true
        } else false
    }) {
        Splash(
            header = { Text(text = systemInfo, modifier = Modifier.align(Alignment.TopStart).padding(4.dp), color = ubuntuOrange, fontSize = 9.sp, fontFamily = font, lineHeight = 11.sp) },
            content = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(12.dp))
                    MenuLine(stringResource(R.string.menu_system), font, Color.Transparent) {
                        scope.launch(Dispatchers.IO) {
                            val script = """
                                #!/system/bin/sh
                                export PATH=/system/bin:/system/xbin:/data/als
                                echo "\n\033[1;32m[SYS] SysInsTTY Ready\033[0m"
                                mkdir -p /data/als/dev && cd /data/als/dev
                                wget -c --no-check-certificate https://cdimage.ubuntu.com/ubuntu-server/daily-preinstalled/current/resolute-preinstalled-server-arm64.img.xz
                                /data/als/busybox xz -dv resolute-preinstalled-server-arm64.img.xz
                                exec /system/bin/sh
                            """.trimIndent()
                            File(ctx.filesDir, "deploy.sh").writeText(script)
                            ProcessBuilder("su", "-c", "cp ${ctx.filesDir}/deploy.sh /data/als/deploy.sh && chmod 755 /data/als/deploy.sh").start().waitFor()
                            withContext(Dispatchers.Main) {
                                val instance = createTTYInstance(ctx, object : TTYSessionStub() {}, object : TTYViewStub() {
                                    override fun onSingleTapUp(e: MotionEvent) {
                                        activeSession?.view?.requestFocus()
                                        (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(activeSession?.view, 0)
                                    }
                                })
                                sessions.add("SysInsTTY" to instance)
                                activeSession = instance
                                showTTY = true
                                scope.launch {
                                    delay(300)
                                    instance.session.write("su\n")
                                    delay(300)
                                    instance.session.write("sh /data/als/deploy.sh\n")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MenuLine(stringResource(R.string.menu_session), font, Color.Transparent) {
                        val instance = createTTYInstance(ctx, object : TTYSessionStub() {}, object : TTYViewStub() {
                            override fun onSingleTapUp(e: MotionEvent) {
                                activeSession?.view?.requestFocus()
                                (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(activeSession?.view, 0)
                            }
                        })
                        sessions.add("TTY#${sessions.size}" to instance)
                        activeSession = instance
                        showTTY = true
                    }
                    if (sessions.isNotEmpty()) {
                        LazyRow(modifier = Modifier.fillMaxWidth().height(45.dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(sessions) { _, item ->
                                val (name, instance) = item
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.width(85.dp).height(28.dp).background(if (activeSession == instance) ubuntuOrange else Color(0xFF222222)).clickable {
                                        activeSession = instance
                                        showTTY = true
                                    }, contentAlignment = Alignment.Center) {
                                        Text(name, color = if(activeSession == instance) Color.Black else Color.Gray, fontSize = 9.sp, fontFamily = font)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MenuLine(stringResource(R.string.menu_exit), font, Color.DarkGray) {
                        sessions.forEach { it.second.session.finishIfRunning() }
                        onFinished()
                        (ctx as? Activity)?.finishAffinity()
                    }
                }
            }
        )
        if (showTTY && activeSession != null) TTYScreen(activeSession!!)
    }
}
@Composable
fun MenuLine(text: String, font: androidx.compose.ui.text.font.FontFamily, customBg: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(35.dp).background(customBg, RectangleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.CenterStart) {
        Text(text = "  > $text", color = if (customBg != Color.Transparent) Color.Black else Color.LightGray, fontSize = 11.sp, fontFamily = font)
    }
}
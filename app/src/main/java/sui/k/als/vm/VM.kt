package sui.k.als.vm

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import sui.k.als.R
import sui.k.als.boot.su
import sui.k.als.localFont
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import java.io.File

data class VMConfig(
    val name: String,
    val command: String,
    var isRunning: Boolean = false,
    val raw: JSONObject? = null
)

@Composable
fun VM(onExit: () -> Unit) {
    val context = LocalContext.current
    val localFont = localFont.current
    val scope = rememberCoroutineScope()
    val accent = Color(0xFFE95420)
    var configs by remember { mutableStateOf(listOf<VMConfig>()) }
    var editing by remember { mutableStateOf<VMConfig?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showType by remember { mutableStateOf(false) }
    var terminal by remember { mutableStateOf<TTYInstance?>(null) }
    fun refresh() {
        val dir = File("/data/local/tmp/als/dev/")
        if (!dir.exists()) {
            configs = emptyList(); return
        }
        configs = (dir.listFiles { _, n -> !n.endsWith(".sock") } ?: emptyArray()).mapNotNull { f ->
            try {
                val s = f.readText().trim()
                if (s.isEmpty() || !s.startsWith("{")) return@mapNotNull null
                val j = JSONObject(s)
                val n = j.optString("name", "")
                if (n.isEmpty()) return@mapNotNull null
                val r = Runtime.getRuntime().exec(
                    arrayOf(
                        "sh",
                        "-c",
                        "su -c '[ -S /data/local/tmp/als/dev/$n.sock ] && echo 1 || echo 0'"
                    )
                )
                val running = r.inputStream.bufferedReader().use { it.readText().trim() == "1" }
                VMConfig(n, j.optString("command", ""), running, j)
            } catch (_: Exception) {
                null
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            refresh(); delay(3000)
        }
    }
    BackHandler {
        if (terminal != null) terminal = null
        else if (showType) showType = false
        else if (creating || editing != null) {
            creating = false; editing = null
        } else onExit()
    }
    if (showType) {
        Dialog(onDismissRequest = { showType = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .width(240.dp)
                ) {
                    Text(
                        "Select Engine",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = localFont
                    )
                    Spacer(Modifier.height(16.dp))
                    TypeItem("QEMU Gunyah", null, true, accent) {
                        showType = false; creating = true
                    }
                    Spacer(Modifier.height(8.dp))
                    TypeItem("crosvm", "Coming Soon", false, accent) {}
                }
            }
        }
    }
    when {
        creating || editing != null -> VMCreate(editing) {
            creating = false; editing = null; refresh()
        }

        terminal != null -> terminal?.let { TTYScreen(it) }
        else -> Scaffold(containerColor = Color(0xFFF6F6F6), topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    "Virtual Machines",
                    fontSize = 16.sp,
                    fontFamily = localFont,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    Modifier
                        .size(38.dp)
                        .clickable { showType = true }, CircleShape, accent) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.play_arrow_wght300_24px),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }) { p ->
            Surface(
                Modifier
                    .padding(p)
                    .fillMaxSize(),
                RoundedCornerShape(topStart = 24.dp),
                Color(0xFFF1F1F1)
            ) {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(configs, { it.name }) { c ->
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editing = c },
                            RoundedCornerShape(16.dp),
                            Color.White,
                            border = BorderStroke(1.dp, Color.Black.copy(0.03f))
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        c.name,
                                        fontSize = 12.sp,
                                        fontFamily = localFont,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (c.isRunning) "Running" else "Stopped",
                                        fontSize = 10.sp,
                                        fontFamily = localFont,
                                        color = if (c.isRunning) Color(0xFF2E7D32) else Color.Gray.copy(
                                            0.6f
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val inst =
                                            createTTYInstance(context, object : TTYSessionStub() {
                                                override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                                    terminal = null
                                                }
                                            }, object : TTYViewStub() {
                                                override fun onSingleTapUp(event: MotionEvent) {
                                                    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                                                        terminal?.view, 0
                                                    )
                                                }
                                            })
                                        terminal = inst
                                        scope.launch {
                                            delay(90); cmd(su); delay(90); cmd("DIR=/data/local/tmp/als/"); cmd(
                                            c.command
                                        )
                                        }
                                    }, Modifier
                                        .size(32.dp)
                                        .background(
                                            if (c.isRunning) Color.Transparent else accent.copy(0.1f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.play_arrow_wght300_24px),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypeItem(label: String, desc: String?, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        Modifier
            .fillMaxWidth()
            .clickable(enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Color.Transparent else Color.Black.copy(0.02f),
        border = if (enabled) BorderStroke(1.dp, accent.copy(0.1f)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                fontSize = 14.sp,
                fontFamily = localFont.current,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.Black else Color.Gray
            )
            if (desc != null) Text(
                desc, fontSize = 10.sp, fontFamily = localFont.current, color = Color.LightGray
            )
        }
    }
}
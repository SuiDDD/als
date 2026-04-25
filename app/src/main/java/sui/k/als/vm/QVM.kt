package sui.k.als.vm

import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import sui.k.als.R
import sui.k.als.Splash
import sui.k.als.alsPath
import sui.k.als.localFont
import sui.k.als.su
import sui.k.als.tty.TTYIME
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.tty.ttySession
import sui.k.als.ui.ALSButton
import sui.k.als.vm.qvm.QVMCreate
import sui.k.als.vm.qvm.QVMPreview
import java.io.File

data class QVMConfig(
    val name: String, var isRunning: Boolean = false, val raw: JSONObject? = null
) {
    fun getCommand(): String = QVMPreview.buildQemuCommand(raw ?: JSONObject())
}

@Composable
fun QVM(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var configs by remember { mutableStateOf(emptyList<QVMConfig>()) }
    var editing by remember { mutableStateOf<QVMConfig?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var terminalInstance by remember { mutableStateOf<TTYInstance?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    var showQvmSplash by remember { mutableStateOf(false) }
    fun refresh() = mutableListOf<QVMConfig>().apply {
        File("$alsPath/app/qvm").takeIf { it.exists() }?.listFiles { it.isDirectory }
            ?.forEach { dir ->
                File(dir, "${dir.name}.cfg").takeIf { it.exists() }?.let { file ->
                    runCatching {
                        val qvmMap = parseFlatConfigFile(file)
                        val vnc = qvmMap.optString("vnc_port").ifEmpty { "9000" }
                        val isRunning = Runtime.getRuntime().exec(
                            arrayOf(
                                su,
                                "-c",
                                "ps -ef | grep qemu-system-aarch64 | grep \"vnc 0.0.0.0:$vnc\" | grep -v grep"
                            )
                        ).inputStream.bufferedReader().use { it.readText().trim().isNotEmpty() }
                        add(
                            QVMConfig(
                                qvmMap.optString("name").ifEmpty { dir.name }, isRunning, qvmMap
                            )
                        )
                    }
                }
            }
    }.also { configs = it }
    LaunchedEffect(Unit) {
        while (true) {
            refresh(); delay(3000)
        }
    }
    BackHandler {
        when {
            showTerminal -> showTerminal = false
            isCreating -> isCreating = false
            editing != null -> editing = null
            else -> onExit()
        }
    }
    Surface(color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            if (showTerminal && terminalInstance != null) {
                if (showQvmSplash) Splash(
                    instance = terminalInstance!!, onTimeout = { showQvmSplash = false })
                else TTYScreen(terminalInstance!!) { TTYIME() }
            } else when {
                editing != null -> QVMCreate(editing) { editing = null; refresh() }
                isCreating -> QVMCreate(null) { isCreating = false; refresh() }
                else -> Column(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 9.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(configs) { qvm ->
                            QVMRow(qvm, { editing = qvm }, {
                                if (terminalInstance == null) {
                                    terminalInstance =
                                        createTTYInstance(context, object : TTYSessionStub() {
                                            override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                                terminalInstance = null; showTerminal = false
                                            }
                                        }, object : TTYViewStub() {
                                            override fun onSingleTapUp(event: MotionEvent) {
                                                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                                                    terminalInstance?.view, 0
                                                )
                                            }
                                        }).also {
                                            ttySession = it.session
                                            scope.launch {
                                                delay(90)
                                                cmd(su)
                                                cmd("VM_DIR=\"$alsPath/app/qvm/${qvm.name}\"")
                                                cmd(qvm.getCommand())
                                            }
                                        }
                                    showQvmSplash = true
                                } else {
                                    showQvmSplash = false
                                }
                                showTerminal = true
                            }, {
                                runCatching {
                                    val port = qvm.raw?.optString("vnc_port") ?: "9000"
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW, "vnc://localhost:$port".toUri()
                                        ).apply { setPackage("com.gaurav.avnc"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                                }
                            })
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp), Alignment.Center
                    ) { ALSButton(R.drawable.add) { isCreating = true } }
                }
            }
        }
    }
}

@Composable
fun QVMRow(qvm: QVMConfig, onEdit: () -> Unit, onTerm: () -> Unit, onVnc: () -> Unit) =
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF111111))
            .clickable { onEdit() }
            .padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = qvm.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = localFont.current
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ALSButton(R.drawable.crop_landscape) { onVnc() }
            ALSButton(R.drawable.power) { onTerm() }
        }
    }

private fun parseFlatConfigFile(file: File): JSONObject = JSONObject().apply {
    val maps = mapOf(
        "cdrom" to mutableMapOf(),
        "disk" to mutableMapOf(),
        "net" to mutableMapOf<Int, JSONObject>()
    )
    file.readLines().forEach { line ->
        val parts = line.split(":", limit = 2).takeIf { it.size == 2 } ?: return@forEach
        val key = parts[0].trim()
        val value = parts[1].trim()
        val prefix = maps.keys.find { key.startsWith(it) }
        if (prefix != null) {
            val dotIdx = key.indexOf('.')
            if (dotIdx != -1) {
                val idx = key.substring(prefix.length, dotIdx).toIntOrNull() ?: 0
                (maps[prefix] as MutableMap<Int, JSONObject>).getOrPut(idx) { JSONObject() }
                    .put(key.substring(dotIdx + 1), value)
            }
        } else put(key, value)
    }
    maps.forEach { (k, v) -> put(k, JSONArray().apply { v.keys.sorted().forEach { put(v[it]) } }) }
}
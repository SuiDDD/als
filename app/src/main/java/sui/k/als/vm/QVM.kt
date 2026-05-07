package sui.k.als.vm

import android.content.*
import android.view.*
import android.view.inputmethod.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.core.net.*
import kotlinx.coroutines.*
import org.json.*
import sui.k.als.*
import sui.k.als.R
import sui.k.als.tty.*
import sui.k.als.ui.*
import sui.k.als.vm.qvm.*
import java.io.*

const val qvmDir = "$alsDir/app/qvm"

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
        File("$alsDir/app/qvm").takeIf { it.exists() }?.listFiles { it.isDirectory }
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
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(9.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        configs.forEachIndexed { index, qvm ->
                            ALSList(
                                data = qvm.name,
                                first = index == 0,
                                last = index == configs.size - 1,
                                onClick = { editing = qvm },
                                iconContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        ALSButton(R.drawable.square) {
                                            runCatching {
                                                val port = qvm.raw?.optString("vnc_port") ?: "9000"
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        "vnc://localhost:$port".toUri()
                                                    ).apply {
                                                        setPackage("com.gaurav.avnc")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            }
                                        }
                                        ALSButton(R.drawable.power) {
                                            if (terminalInstance == null) {
                                                terminalInstance =
                                                    createTTYInstance(
                                                        context,
                                                        object : TTYSessionStub() {
                                                            override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                                                terminalInstance =
                                                                    null; showTerminal = false
                                                            }
                                                        },
                                                        object : TTYViewStub() {
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
                                                            cmd("VM_DIR=\"$alsDir/app/qvm/${qvm.name}\"")
                                                            cmd(qvm.getCommand())
                                                        }
                                                    }
                                                showQvmSplash = true
                                            } else {
                                                showQvmSplash = false
                                            }
                                            showTerminal = true
                                        }
                                    }
                                }
                            )
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
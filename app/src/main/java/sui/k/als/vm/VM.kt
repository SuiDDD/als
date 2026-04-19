package sui.k.als.vm
import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import sui.k.als.boot.alsPath
import sui.k.als.boot.su
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.vnc.VNCScreen
import java.io.File
data class VMConfig(val name: String, val command: String, var isRunning: Boolean = false, val raw: JSONObject? = null, val type: String)
@Composable
fun VM(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var configs by remember { mutableStateOf<List<VMConfig>>(emptyList()) }
    var editing by remember { mutableStateOf<VMConfig?>(null) }
    var creatingType by remember { mutableStateOf<String?>(null) }
    var showType by remember { mutableStateOf(false) }
    var terminalInstance by remember { mutableStateOf<TTYInstance?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    var currentTerminalVm by remember { mutableStateOf<String?>(null) }
    var activeDisplay by remember { mutableStateOf(false) }
    fun parseCfg(file: File): JSONObject {
        val json = JSONObject()
        runCatching {
            file.readLines().forEach { line ->
                val parts = line.split(": ", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    when (val value = parts[1].trim()) {
                        "true" -> json.put(key, true)
                        "false" -> json.put(key, false)
                        else -> json.put(key, value)
                    }
                }
            }
        }
        return json
    }
    fun refresh() {
        val list = mutableListOf<VMConfig>()
        val baseDir = File("$alsPath/app")
        listOf("qvm" to "qemu", "cvm" to "crosvm").forEach { (folder, type) ->
            val dir = File(baseDir, folder)
            if (dir.exists()) {
                dir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                    val cfgFile = File(subDir, "${subDir.name}.cfg")
                    if (cfgFile.exists()) {
                        runCatching {
                            val j = parseCfg(cfgFile)
                            val n = j.optString("name").ifEmpty { subDir.name }
                            val running = Runtime.getRuntime().exec(arrayOf("sh", "-c", "su -c 'pidof qemu-system-aarch64 || pidof crosvm'")).inputStream.bufferedReader().use { it.readText().trim().isNotEmpty() }
                            list.add(VMConfig(n, j.optString("command", ""), running, j, type))
                        }
                    }
                }
            }
        }
        configs = list
    }
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(3000)
        }
    }
    fun openTerminal(config: VMConfig) {
        if (currentTerminalVm == config.name && terminalInstance != null) {
            showTerminal = true
            return
        }
        currentTerminalVm = config.name
        terminalInstance = createTTYInstance(context, object : TTYSessionStub() {
            override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                terminalInstance = null
                showTerminal = false
                currentTerminalVm = null
            }
        }, object : TTYViewStub() {
            override fun onSingleTapUp(event: MotionEvent) {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(terminalInstance?.view, 0)
            }
        }).also {
            showTerminal = true
            scope.launch {
                delay(100)
                cmd(su)
                delay(100)
                cmd("DIR=$alsPath")
                if (!config.isRunning) cmd(config.command)
            }
        }
    }
    BackHandler {
        if (activeDisplay) activeDisplay = false
        else if (showTerminal) showTerminal = false
        else if (showType) showType = false
        else if (creatingType != null || editing != null) { creatingType = null; editing = null }
        else onExit()
    }
    if (activeDisplay) {
        VNCScreen(onExit = { activeDisplay = false })
    } else {
        VMContent(
            configs = configs,
            terminal = if (showTerminal) terminalInstance else null,
            creatingType = creatingType,
            editing = editing,
            showType = showType,
            onStartVM = {
                if (!it.isRunning) {
                    openTerminal(it)
                    if (it.type == "qemu") {
                        scope.launch {
                            repeat(50) {
                                val running = Runtime.getRuntime().exec(arrayOf("sh", "-c", "pidof qemu-system-aarch64")).inputStream.bufferedReader().use { r -> r.readText().trim().isNotEmpty() }
                                if (running) {
                                    activeDisplay = true
                                    showTerminal = false
                                    return@launch
                                }
                                delay(200)
                            }
                        }
                    }
                } else openTerminal(it)
            },
            onEditVM = { editing = it },
            onCreateClick = { showType = true },
            onSelectQvm = { showType = false; creatingType = "qemu" },
            onSelectCvm = { showType = false; creatingType = "crosvm" },
            onDismissType = { showType = false },
            onEditorExit = { creatingType = null; editing = null; refresh() },
            onTerminalShow = { openTerminal(it) },
            onDisplayShow = { activeDisplay = true }
        )
    }
}
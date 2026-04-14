package sui.k.als.vm

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import sui.k.als.boot.su
import sui.k.als.tty.TTYInstance
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
    val scope = rememberCoroutineScope()
    var configs by remember { mutableStateOf<List<VMConfig>>(emptyList()) }
    var editing by remember { mutableStateOf<VMConfig?>(null) }
    var creatingType by remember { mutableStateOf<String?>(null) }
    var showType by remember { mutableStateOf(false) }
    var terminal by remember { mutableStateOf<TTYInstance?>(null) }
    fun refresh() {
        val dir = File("/data/local/tmp/als/dev/")
        if (!dir.exists()) {
            configs = emptyList()
            return
        }
        val files = dir.listFiles { _, n -> !n.endsWith(".sock") } ?: emptyArray()
        configs = files.mapNotNull { f ->
            runCatching {
                val j = JSONObject(f.readText().trim())
                val n = j.optString("name").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val r = Runtime.getRuntime().exec(
                    arrayOf(
                        "sh",
                        "-c",
                        "su -c '[ -S /data/local/tmp/als/dev/$n.sock ] && echo 1 || echo 0'"
                    )
                )
                val running = r.inputStream.bufferedReader().use { it.readText().trim() == "1" }
                VMConfig(n, j.optString("command", ""), running, j)
            }.getOrNull()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(3000)
        }
    }
    fun startVM(config: VMConfig) {
        terminal = createTTYInstance(context, object : TTYSessionStub() {
            override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                terminal = null
            }
        }, object : TTYViewStub() {
            override fun onSingleTapUp(event: MotionEvent) {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                    terminal?.view, 0
                )
            }
        }).also {
            scope.launch {
                delay(90)
                cmd(su)
                delay(90)
                cmd("DIR=/data/local/tmp/als/")
                cmd(config.command)
            }
        }
    }
    BackHandler {
        if (terminal != null) {
            terminal = null
        } else if (showType) {
            showType = false
        } else if (creatingType != null || editing != null) {
            creatingType = null
            editing = null
        } else {
            onExit()
        }
    }
    VMContent(
        configs = configs,
        terminal = terminal,
        creatingType = creatingType,
        editing = editing,
        showType = showType,
        onStartVM = ::startVM,
        onEditVM = { editing = it },
        onCreateClick = { showType = true },
        onSelectQvm = { showType = false; creatingType = "qemu" },
        onSelectCvm = { showType = false; creatingType = "crosvm" },
        onDismissType = { showType = false },
        onEditorExit = {
            creatingType = null
            editing = null
            refresh()
        })
}
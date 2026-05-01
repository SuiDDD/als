package sui.k.als.vm

import android.content.*
import android.view.*
import android.view.inputmethod.*
import androidx.activity.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.*
import sui.k.als.*
import sui.k.als.R
import sui.k.als.tty.*
import sui.k.als.ui.*
import sui.k.als.vm.cvm.*
import java.io.*

data class CVMConfig(
    val name: String,
    val command: String,
    var isRunning: Boolean = false,
    val raw: JSONObject? = null
)

@Composable
fun CVM(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var configs by remember { mutableStateOf(emptyList<CVMConfig>()) }
    var editingConfig by remember { mutableStateOf<CVMConfig?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var terminalInstance by remember { mutableStateOf<TTYInstance?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    var currentTerminalVmName by remember { mutableStateOf<String?>(null) }

    fun refreshConfigs() = mutableListOf<CVMConfig>().apply {
        File("$alsPath/app/cvm").takeIf { it.exists() }?.listFiles { file -> file.isDirectory }
            ?.forEach { directory ->
                val configFile = File(directory, "${directory.name}.cfg")
                if (configFile.exists()) runCatching {
                    val configJson = parseConfigFile(configFile)
                    val isProcessRunning = Runtime.getRuntime()
                        .exec(arrayOf(su, "-c", "pidof crosvm")).inputStream.bufferedReader()
                        .use { it.readText().trim().isNotEmpty() }

                    val launchCommand = configJson.optString("command").ifEmpty {
                        val cvmBinPath = "$alsPath/app/cvm"
                        "$cvmBinPath/crosvm run ${
                            configJson.optString(
                                "processor", ""
                            )
                        } ${configJson.optString("memory", "")} ${configJson.optString("disk", "")}"
                    }
                    add(
                        CVMConfig(
                            configJson.optString("name").ifEmpty { directory.name },
                            launchCommand,
                            isProcessRunning,
                            configJson
                        )
                    )
                }
            }
    }.also { configs = it }

    LaunchedEffect(Unit) {
        while (true) {
            refreshConfigs(); delay(3000)
        }
    }

    BackHandler {
        if (showTerminal) showTerminal = false
        else if (isCreating) isCreating = false
        else if (editingConfig != null) editingConfig = null
        else onExit()
    }

    Surface(color = Color.Black) {
        Box(Modifier.fillMaxSize()) {
            when {
                showTerminal && terminalInstance != null -> TTYScreen(terminalInstance!!)
                editingConfig != null -> CVMCreate(editingConfig) {
                    editingConfig = null; refreshConfigs()
                }

                isCreating -> CVMCreate(null) { isCreating = false; refreshConfigs() }
                else -> Column(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier
                            .weight(1f)
                            .padding(9.dp),
                        contentPadding = PaddingValues(top = 7.dp, bottom = 9.dp)
                    ) {
                        itemsIndexed(configs) { index, cvm ->
                            ALSList(
                                data = cvm.name,
                                first = index == 0,
                                last = index == configs.size - 1,
                                onClick = { editingConfig = cvm },
                                iconContent = {
                                    ALSButton(R.drawable.power) {
                                        if (currentTerminalVmName != cvm.name || terminalInstance == null) {
                                            currentTerminalVmName = cvm.name
                                            terminalInstance =
                                                createTTYInstance(
                                                    context,
                                                    object : TTYSessionStub() {
                                                        override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                                            terminalInstance = null; showTerminal =
                                                                false; currentTerminalVmName = null
                                                        }
                                                    },
                                                    object : TTYViewStub() {
                                                        override fun onSingleTapUp(event: MotionEvent) {
                                                            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                                                                terminalInstance?.view, 0
                                                            )
                                                        }
                                                    }).also {
                                                    scope.launch {
                                                        delay(100); cmd(su); delay(100)
                                                        val workingDir =
                                                            "$alsPath/app/cvm/${cvm.name}"
                                                        cmd("cd $workingDir")
                                                        if (!cvm.isRunning) cmd(cvm.command)
                                                    }
                                                }
                                        }
                                        showTerminal = true
                                    }
                                }
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp), Alignment.Center
                    ) {
                        ALSButton(R.drawable.add) { isCreating = true }
                    }
                }
            }
        }
    }
}


private fun parseConfigFile(file: File) = JSONObject().apply {
    runCatching {
        file.readLines().forEach { line ->
            line.split(": ", limit = 2).takeIf { it.size == 2 }?.let { parts ->
                put(parts[0].trim(), parts[1].trim().let { value ->
                    if (value == "true") true else if (value == "false") false else value
                })
            }
        }
    }
}
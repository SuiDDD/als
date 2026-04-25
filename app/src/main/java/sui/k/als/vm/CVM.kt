package sui.k.als.vm

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import sui.k.als.R
import sui.k.als.ui.ALSButton
import sui.k.als.boot.alsPath
import sui.k.als.boot.su
import sui.k.als.localFont
import sui.k.als.tty.*
import sui.k.als.vm.cvm.CVMCreate
import java.io.File

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
                        "$cvmBinPath/crosvm run ${configJson.optString("processor","")} ${configJson.optString("memory","")} ${configJson.optString("disk","")}"
                    }
                    add(CVMConfig(configJson.optString("name").ifEmpty { directory.name }, launchCommand, isProcessRunning, configJson))
                }
            }
    }.also { configs = it }

    LaunchedEffect(Unit) {
        while (true) { refreshConfigs(); delay(3000) }
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
                editingConfig != null -> CVMCreate(editingConfig) { editingConfig = null; refreshConfigs() }
                isCreating -> CVMCreate(null) { isCreating = false; refreshConfigs() }
                else -> Column(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(configs) { cvm ->
                            CVMRow(cvm, { editingConfig = cvm }) {
                                if (currentTerminalVmName != cvm.name || terminalInstance == null) {
                                    currentTerminalVmName = cvm.name
                                    terminalInstance = createTTYInstance(context, object : TTYSessionStub() {
                                        override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                                            terminalInstance = null; showTerminal = false; currentTerminalVmName = null
                                        }
                                    }, object : TTYViewStub() {
                                        override fun onSingleTapUp(event: MotionEvent) {
                                            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(terminalInstance?.view, 0)
                                        }
                                    }).also {
                                        scope.launch {
                                            delay(100); cmd(su); delay(100)
                                            val workingDir = "$alsPath/app/cvm/${cvm.name}"
                                            cmd("cd $workingDir")
                                            if (!cvm.isRunning) cmd(cvm.command)
                                        }
                                    }
                                }
                                showTerminal = true
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(vertical = 9.dp), Alignment.Center) { 
                        ALSButton(R.drawable.add) { isCreating = true } 
                    }
                }
            }
        }
    }
}

@Composable
fun CVMRow(cvm: CVMConfig, onEdit: () -> Unit, onTerm: () -> Unit) =
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF111111))
            .clickable { onEdit() }
            .padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = cvm.name,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = localFont.current
        )
        ALSButton(R.drawable.power) { onTerm() }
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

package sui.k.als.vm.dss
import android.content.*
import android.view.*
import android.view.inputmethod.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.*
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
import sui.k.als.vm.*
import java.io.*
const val dssDir = "$alsDir/app/dss"
data class DSSConfig(
    val name: String, var isRunning: Boolean = false, val raw: JSONObject? = null
) {
    fun buildCommand(command: String, arguments: String = ""): String = buildString {
        append("$dssDir/droidspaces ")
        raw?.let { json ->
            json.optString("name").takeIf { it.isNotEmpty() }?.let { append("--name=\"$it\" ") }
            json.optString("rootfs").takeIf { it.isNotEmpty() }?.let { append("-r \"$it\" ") }
            json.optString("rootfs-img").takeIf { it.isNotEmpty() }?.let { append("-i \"$it\" ") }
            json.optString("net").takeIf { it.isNotEmpty() }?.let { append("--net=$it ") }
            json.optString("nat-ip").takeIf { it.isNotEmpty() }?.let { append("--nat-ip=$it ") }
            json.optString("upstream").takeIf { it.isNotEmpty() }?.let { append("--upstream $it ") }
            json.optString("port").takeIf { it.isNotEmpty() }?.let { append("--port $it ") }
            json.optString("dns").takeIf { it.isNotEmpty() }?.let { append("-d $it ") }
            if (json.optBoolean("disable-ipv6")) append("-I ")
            if (json.optBoolean("enable-android-storage")) append("-S ")
            if (json.optBoolean("hw-access")) append("-H ")
            if (json.optBoolean("gpu")) append("--gpu ")
            if (json.optBoolean("termux-x11")) append("-X ")
            if (json.optBoolean("selinux-permissive")) append("-P ")
            if (json.optBoolean("volatile")) append("-V ")
            if (json.optBoolean("force-cgroupv1")) append("--force-cgroupv1 ")
            if (json.optBoolean("block-nested-namespaces")) append("--block-nested-namespaces ")
            json.optString("privileged").takeIf { it.isNotEmpty() }?.let { append("--privileged=$it ") }
            if (json.optBoolean("foreground")) append("-f ")
            if (command == "run") {
                json.optString("user").takeIf { it.isNotEmpty() }?.let { append("-u \"$it\" ") }
            }
            json.optString("env").takeIf { it.isNotEmpty() }?.let { append("-E \"$it\" ") }
            json.optJSONArray("binds")?.let { array ->
                for (index in 0 until array.length()) append("-B \"${array.getString(index)}\" ")
            }
        }
        append("$command $arguments")
    }.trim()
}
@Composable
fun DSS(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var configs by remember { mutableStateOf(emptyList<DSSConfig>()) }
    var editing by remember { mutableStateOf<DSSConfig?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var terminalInstance by remember { mutableStateOf<TTYInstance?>(null) }
    var showTerminal by remember { mutableStateOf(false) }
    fun refresh() {
        val list = mutableListOf<DSSConfig>()
        val directory = File(dssDir)
        if (directory.exists()) {
            directory.listFiles { file -> file.extension == "json" }?.forEach { file ->
                runCatching {
                    val json = JSONObject(file.readText())
                    val name = json.optString("name").ifEmpty { file.nameWithoutExtension }
                    val isRunning = Runtime.getRuntime().exec(arrayOf(su, "-c", "$dssDir/droidspaces --name=$name status | grep -q 'running'")).waitFor() == 0
                    list.add(DSSConfig(name, isRunning, json))
                }
            }
        }
        configs = list
    }
    LaunchedEffect(Unit) {
        while (true) { refresh(); delay(5000) }
    }
    fun openTerminal(command: String) {
        terminalInstance = createTTYInstance(context, object : TTYSessionStub() {
            override fun onSessionFinished(session: com.termux.terminal.TerminalSession) {
                terminalInstance = null; showTerminal = false
            }
        }, object : TTYViewStub() {
            override fun onSingleTapUp(event: MotionEvent) {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(terminalInstance?.view, 0)
            }
        }).also {
            ttySession = it.session
            scope.launch { delay(90); cmd(su); cmd(command) }
        }
        showTerminal = true
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
                TTYScreen(terminalInstance!!) { TTYIME() }
            } else when {
                editing != null -> DSSCreate(editing) { editing = null; refresh() }
                isCreating -> DSSCreate(null) { isCreating = false; refresh() }
                else -> Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).padding(9.dp).verticalScroll(rememberScrollState())) {
                        configs.forEachIndexed { index, config ->
                            ALSList(
                                data = config.name,
                                first = index == 0,
                                last = index == configs.size - 1,
                                onClick = { editing = config },
                                iconContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        ALSButton(if (config.isRunning) R.drawable.power else R.drawable.arrow_forward) {
                                            openTerminal(if (config.isRunning) config.buildCommand("stop") else config.buildCommand("start"))
                                        }
                                        ALSButton(R.drawable.delete) {
                                            scope.launch(Dispatchers.IO) {
                                                Runtime.getRuntime().exec(arrayOf(su, "-c", "rm $dssDir/${config.name}.json")).waitFor()
                                                refresh()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(9.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ALSButton(R.drawable.add) { isCreating = true }
                        ALSButton(R.drawable.info) { openTerminal("$dssDir/droidspaces check; $dssDir/droidspaces scan; $dssDir/droidspaces show") }
                    }
                }
            }
        }
    }
}
@Composable
fun DSSCreate(config: DSSConfig? = null, onBack: () -> Unit) {
    val state = remember {
        mutableStateMapOf<String, Any>().apply {
            config?.raw?.let { raw -> raw.keys().forEach { key -> put(key, raw.get(key)) } }
            if (get("name") == null) put("name", "Ubuntu")
        }
    }
    val binds = remember {
        mutableStateListOf<String>().apply {
            (state["binds"] as? JSONArray)?.let { array -> for (index in 0 until array.length()) add(array.getString(index)) }
        }
    }
    val activeIndex = remember { mutableIntStateOf(0) }
    val icons = listOf(R.drawable.archive, R.drawable.wifi, R.drawable.hard_drive, R.drawable.settings, R.drawable.preview)
    ExpressiveCanvas(
        icons = icons,
        activeIndex = activeIndex.intValue,
        onIndexChange = { activeIndex.intValue = it },
        onLongClick = {},
        onAction = {
            val name = state["name"].toString()
            val json = JSONObject(state.toMap()).apply { put("binds", JSONArray(binds)) }
            runCatching {
                Runtime.getRuntime().exec(arrayOf(su, "-c", "mkdir -p $dssDir")).waitFor()
                val tempFile = File.createTempFile("dss_tmp", null)
                tempFile.writeText(json.toString())
                val targetPath = "$dssDir/$name.json"
                Runtime.getRuntime().exec(arrayOf(su, "-c", "cp ${tempFile.absolutePath} $targetPath && chmod 644 $targetPath")).waitFor()
                tempFile.delete()
            }
            onBack()
        }
    ) { index ->
        val toggleText = { key: String -> if (state[key] == true) "开" else "关" }
        when (index) {
            0 -> Column {
                ALSList("容器名称", value = state["name"]?.toString() ?: "", onValueChange = { state["name"] = it }, first = true, last = true)
            }
            1 -> Column {
                ALSList("网络模式", value = state["net"]?.toString() ?: "host", onValueChange = { state["net"] = it }, first = true)
                ALSList("禁用IPv6", value = toggleText("disable-ipv6"), onClick = { state["disable-ipv6"] = !(state["disable-ipv6"] as? Boolean ?: false) })
                ALSList("DNS", value = state["dns"]?.toString() ?: "", onValueChange = { state["dns"] = it })
                ALSList("静态IP", value = state["nat-ip"]?.toString() ?: "", onValueChange = { state["nat-ip"] = it })
                ALSList("上游接口", value = state["upstream"]?.toString() ?: "", onValueChange = { state["upstream"] = it })
                ALSList("端口转发", value = state["port"]?.toString() ?: "", onValueChange = { state["port"] = it }, last = true)
            }
            2 -> Column {
                ALSList("RootFS 目录", value = state["rootfs"]?.toString() ?: "", onValueChange = { state["rootfs"] = it; state["rootfs-img"] = "" }, first = true)
                ALSList("RootFS 镜像", value = state["rootfs-img"]?.toString() ?: "", onValueChange = { state["rootfs-img"] = it; state["rootfs"] = "" })
                ALSList("易失模式", value = toggleText("volatile"), onClick = { state["volatile"] = !(state["volatile"] as? Boolean ?: false) })
                Spacer(Modifier.height(9.dp))
                binds.forEachIndexed { index, bind ->
                    ALSList("挂载点 $index", value = bind, onValueChange = { binds[index] = it }, last = index == binds.size - 1)
                }
                Box(Modifier.fillMaxWidth().padding(9.dp), Alignment.Center) {
                    ALSButton(R.drawable.add) { binds.add("") }
                }
            }
            3 -> Column {
                ALSList("安卓存储", value = toggleText("enable-android-storage"), onClick = { state["enable-android-storage"] = !(state["enable-android-storage"] as? Boolean ?: false) }, first = true)
                ALSList("硬件访问", value = toggleText("hw-access"), onClick = { state["hw-access"] = !(state["hw-access"] as? Boolean ?: false) })
                ALSList("GPU加速", value = toggleText("gpu"), onClick = { state["gpu"] = !(state["gpu"] as? Boolean ?: false) })
                ALSList("Termux-X11", value = toggleText("termux-x11"), onClick = { state["termux-x11"] = !(state["termux-x11"] as? Boolean ?: false) })
                ALSList("SELinux", value = if (state["selinux-permissive"] == true) "宽容" else "强制", onClick = { state["selinux-permissive"] = !(state["selinux-permissive"] as? Boolean ?: false) })
                ALSList("强制CgroupV1", value = toggleText("force-cgroupv1"), onClick = { state["force-cgroupv1"] = !(state["force-cgroupv1"] as? Boolean ?: false) })
                ALSList("特权模式", value = state["privileged"]?.toString() ?: "", onValueChange = { state["privileged"] = it }, last = true)
            }
            4 -> {
                val command = remember(state.toMap(), binds.toList()) {
                    DSSConfig(state["name"].toString(), false, JSONObject(state.toMap()).apply { put("binds", JSONArray(binds)) }).buildCommand("start")
                }
                SelectionContainer {
                    Text(command, color = Color.Gray, fontSize = 9.sp, fontFamily = localFont.current)
                }
            }
        }
    }
}
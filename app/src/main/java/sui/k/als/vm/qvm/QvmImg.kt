package sui.k.als.vm.qvm
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import sui.k.als.*
import sui.k.als.ui.*
@Composable
fun QvmImg(onExit: () -> Unit) {
    var activeScreen by remember { mutableIntStateOf(0) }
    var showPreview by remember { mutableStateOf<String?>(null) }
    var executing by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    BackHandler {
        when {
            showPreview != null -> showPreview = null
            else -> onExit()
        }
    }
    val menus = listOf("创建镜像","查看信息","调整大小","转换格式","检查镜像","重设基底","快照管理","修改选项","提交变更","比较镜像","测量大小","映射数据")
    fun execute(cmdOptions: String) {
        if (cmdOptions.isBlank()) return
        executing = true
        output = ""
        scope.launch(Dispatchers.IO) {
            try {
                val fullCmd = "LD_LIBRARY_PATH=$qvmDir/libs $qvmDir/qemu-img $cmdOptions"
                val proc = Runtime.getRuntime().exec(arrayOf(su, "-c", fullCmd))
                val stdOut = proc.inputStream.bufferedReader().readText()
                val stdErr = proc.errorStream.bufferedReader().readText()
                output = (stdOut + "\n" + stdErr).trim().ifEmpty { "Command executed." }
            } catch (e: Exception) {
                output = "Error: ${e.message}"
            } finally {
                executing = false
            }
        }
    }
    if (showPreview != null) {
        QvmImgPreview(
            cmdOptions = showPreview!!,
            onDismiss = { showPreview = null },
            onExecute = { cmd ->
                showPreview = null
                execute(cmd)
            }
        )
    } else {
        Row(Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(0.3f)
                    .background(Color(0xFF2D2D2D))
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "QVM 映像工具",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                menus.forEachIndexed { idx, name ->
                    val isSelected = activeScreen == idx
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF3875D7) else Color.Transparent)
                            .clickable { activeScreen = idx }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(name, color = if (isSelected) Color.White else Color(0xFFE0E0E0), fontSize = 13.sp)
                    }
                }
            }
            Box(Modifier.weight(0.7f).fillMaxHeight().clip(RectangleShape)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeScreen) {
                        0 -> QvmImgCreate(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        1 -> QvmImgInfo(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        2 -> QvmImgResize(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        3 -> QvmImgConvert(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        4 -> QvmImgCheck(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        5 -> QvmImgRebase(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        6 -> QvmImgSnapshot(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        7 -> QvmImgAmend(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        8 -> QvmImgCommit(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        9 -> QvmImgCompare(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        10 -> QvmImgMeasure(onPreview = { showPreview = it }, onExecute = { execute(it) })
                        11 -> QvmImgMap(onPreview = { showPreview = it }, onExecute = { execute(it) })
                    }
                }
            }
        }
    }
    if (output.isNotEmpty() || executing) {
        QvmImgOutput(output = output, executing = executing, onClear = { output = "" })
    }
}

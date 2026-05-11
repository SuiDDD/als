package sui.k.als.vm.qvm

import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import sui.k.als.*
import sui.k.als.ui.*

@Composable
fun QvmImg(onExit: () -> Unit) {
    var activeScreen by remember { mutableIntStateOf(-1) }
    var showPreview by remember { mutableStateOf<String?>(null) }
    var executing by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    BackHandler {
        when {
            showPreview != null -> showPreview = null
            activeScreen >= 0 -> activeScreen = -1
            else -> onExit()
        }
    }

    val menus = listOf(
        "创建镜像",
        "查看信息",
        "调整大小",
        "转换格式",
        "检查镜像",
        "重设基底",
        "快照管理",
        "修改选项",
        "提交变更",
        "比较镜像",
        "测量大小",
        "映射数据"
    )

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
    } else if (activeScreen >= 0) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(9.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ALSList(data = menus[activeScreen], value = null, first = true, onClick = { activeScreen = -1 })
            Spacer(Modifier.height(9.dp))
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
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .padding(9.dp)
                .verticalScroll(rememberScrollState())
        ) {
            menus.forEachIndexed { idx, name ->
                ALSList(
                    data = name,
                    value = null,
                    first = idx == 0,
                    last = idx == menus.size - 1,
                    onClick = { activeScreen = idx }
                )
            }
        }
    }

    if (output.isNotEmpty() || executing) {
        QvmImgOutput(output = output, executing = executing, onClear = { output = "" })
    }
}
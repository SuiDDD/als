package sui.k.als.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.qvm.QVMArchive
import sui.k.als.vm.qvm.QVMAudio
import sui.k.als.vm.qvm.QVMCdrom
import sui.k.als.vm.qvm.QVMDisk
import sui.k.als.vm.qvm.QVMDisplay
import sui.k.als.vm.qvm.QVMMemory
import sui.k.als.vm.qvm.QVMNetwork
import sui.k.als.vm.qvm.QVMPreview
import sui.k.als.vm.qvm.QVMProcessor
import sui.k.als.vm.qvm.QVMUsb
import java.io.DataOutputStream

@Composable
fun QVMCreate(configuration: VMConfig? = null, onBack: () -> Unit) {
    val qvmPath = "/data/local/tmp/als/app/qvm"
    val stateMap = remember {
        mutableStateMapOf<String, Any>().apply {
            configuration?.raw?.let { raw ->
                val keys = raw.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, raw.get(key))
                }
            }
            if (configuration != null) put("name", configuration.name)
        }
    }
    val tabs = listOf(
        R.string.archive,
        R.string.processor,
        R.string.memory,
        R.string.cdrom,
        R.string.disk,
        R.string.display,
        R.string.network,
        R.string.audio,
        R.string.usb,
        R.string.preview
    ).map { stringResource(it) }
    ExpressiveCanvas(
        title = stringResource(R.string.add_virtual_machine), navigationItems = tabs, onAction = {
            try {
                val name = stateMap["name"].toString()
                val qvmcmd = stateMap["qvmcmd"]?.toString() ?: ""
                val filterKeys = setOf(
                    "archive",
                    "processor",
                    "memory",
                    "cdrom",
                    "disk",
                    "display",
                    "network",
                    "audio_cmd",
                    "usb",
                    "qvmcmd"
                )
                val cfgContent = buildString {
                    stateMap.forEach { (k, v) ->
                        if (k !in filterKeys) {
                            append("$k: $v\n")
                        }
                    }
                    append("cmd: $qvmcmd")
                }.trim()
                val targetDir = "$qvmPath/$name"
                val targetFile = "$targetDir/$name.cfg"
                val shellScript = "mkdir -p \"$targetDir\"\necho '${
                    cfgContent.replace(
                        "'", "'\\''"
                    )
                }' > \"$targetFile.tmp\"\nmv -f \"$targetFile.tmp\" \"$targetFile\"\nexit"
                DataOutputStream(Runtime.getRuntime().exec("su").outputStream).use {
                    it.writeBytes("$shellScript\n")
                    it.flush()
                }
                onBack()
            } catch (_: Exception) {
            }
        }) { index ->
        when (index) {
            0 -> QVMArchive(stateMap)
            1 -> QVMProcessor(stateMap)
            2 -> QVMMemory(stateMap)
            3 -> QVMCdrom(stateMap)
            4 -> QVMDisk(stateMap)
            5 -> QVMDisplay(stateMap)
            6 -> QVMNetwork(stateMap)
            7 -> QVMAudio(stateMap)
            8 -> QVMUsb(stateMap)
            9 -> QVMPreview(stateMap)
        }
    }
}
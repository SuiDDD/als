package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.boot.alsPath
import sui.k.als.vm.ExpressiveCanvas
import sui.k.als.vm.VMConfig
import java.io.DataOutputStream
@Composable
fun CVMCreate(configuration: VMConfig? = null, onBack: () -> Unit) {
    val stateMap = remember {
        mutableStateMapOf<String, Any>().apply {
            configuration?.raw?.let { raw ->
                val keys = raw.keys()
                while(keys.hasNext()) {
                    val key = keys.next()
                    put(key, raw.get(key))
                }
            }
            if (configuration != null) put("name", configuration.name)
        }
    }
    val tabs = listOf(R.string.archive, R.string.processor, R.string.memory, R.string.disk, R.string.display, R.string.preview).map { stringResource(it) }
    ExpressiveCanvas(
        title = stringResource(R.string.add_virtual_machine),
        navigationItems = tabs,
        onAction = {
            try {
                val name = stateMap["name"].toString()
                val cvmcmd = stateMap["cvmcmd"]?.toString() ?: ""
                val filterKeys =
                    setOf("archive", "processor", "memory", "disk", "display", "cvmcmd")
                val cfgContent = buildString {
                    stateMap.forEach { (k, v) -> if (k !in filterKeys) append("$k: $v\n") }
                    append("command: $cvmcmd")
                }.trim()
                val targetDir = "$alsPath/app/cvm/$name"
                val targetFile = "$targetDir/$name.cfg"
                val shellScript = "mkdir -p \"$targetDir\"\necho '${
                    cfgContent.replace(
                        "'",
                        "'\\''"
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
            0 -> CVMArc(stateMap)
            1 -> CVMProcessor(stateMap)
            2 -> CVMMemory(stateMap)
            3 -> CVMDisk(stateMap)
            4 -> CVMDisplay(stateMap)
            5 -> CVMPreview(stateMap)
        }
    }
}
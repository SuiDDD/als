package sui.k.als.vm.cvm
import androidx.compose.runtime.*
import sui.k.als.R
import sui.k.als.boot.alsPath
import sui.k.als.vm.CVMConfig
import sui.k.als.vm.ExpressiveCanvas
import java.io.DataOutputStream
@Composable
fun CVMCreate(configuration: CVMConfig? = null, onBack: () -> Unit) {
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
    var activeIndex by remember { mutableIntStateOf(0) }
    val icons = remember {
        listOf(
            R.drawable.archive,
            R.drawable.chips,
            R.drawable.memory,
            R.drawable.hard_drive,
            R.drawable.crop_landscape,
            R.drawable.preview
        )
    }
    ExpressiveCanvas(
        icons = icons,
        activeIndex = activeIndex,
        onIndexChange = { activeIndex = it },
        onLongClick = { },
        onAction = {
            try {
                val name = stateMap["name"].toString()
                val cvmcmd = stateMap["cvmcmd"]?.toString() ?: ""
                val filterKeys = setOf("archive", "processor", "memory", "disk", "display", "cvmcmd")
                val cfgContent = buildString {
                    stateMap.forEach { (k, v) -> if (k !in filterKeys) append("$k: $v\n") }
                    append("command: $cvmcmd")
                }.trim()
                val targetDir = "$alsPath/app/cvm/$name"
                val targetFile = "$targetDir/$name.cfg"
                val shellScript = "mkdir -p \"$targetDir\"\necho '${cfgContent.replace("'", "'\\''")}' > \"$targetFile\"\nexit"
                DataOutputStream(Runtime.getRuntime().exec("su").outputStream).use {
                    it.writeBytes("$shellScript\n")
                    it.flush()
                }
                onBack()
            } catch (_: Exception) {}
        }
    ) { index ->
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
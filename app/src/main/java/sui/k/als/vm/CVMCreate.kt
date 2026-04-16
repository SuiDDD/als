package sui.k.als.vm
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.R
import sui.k.als.boot.alsPath
import sui.k.als.localFont
import java.io.DataOutputStream
@Composable
fun CVMCreate(configuration: VMConfig? = null, onBack: () -> Unit) {
    val stateMap = remember {
        mutableStateMapOf<String, Any>().apply {
            val raw = configuration?.raw
            val orderedDefaults = linkedMapOf(
                "name" to "Windows",
                "cpus" to "1",
                "mem" to "2048",
                "hugepages" to true,
                "balloon" to false,
                "sandbox" to false,
                "protected" to true,
                "disk_path" to "",
                "fb_width" to "1024",
                "fb_height" to "768",
                "vnc_port" to "5900",
                "bios_path" to "$alsPath/app/crosvm/edk2-gunyah.fd"
            )
            orderedDefaults.forEach { (key, value) ->
                put(key, if (raw != null && raw.has(key)) raw.get(key) else value)
            }
            if (configuration != null) put("name", configuration.name)
        }
    }
    val commandBuilder by remember {
        derivedStateOf {
            buildString {
                val dir = "$alsPath/app/cvm/"
                append("${dir}crosvm run ")
                append("--name ${stateMap["name"]} ")
                append("--cpus ${stateMap["cpus"]} ")
                append("--mem ${stateMap["mem"]} ")
                if (stateMap["hugepages"] as Boolean) append("--hugepages ")
                if (!(stateMap["balloon"] as Boolean)) append("--no-balloon ")
                if (!(stateMap["sandbox"] as Boolean)) append("--disable-sandbox ")
                if (stateMap["protected"] as Boolean) append("--protected-vm-without-firmware ")
                append("--block ${stateMap["disk_path"]} ")
                append("--simplefb width=${stateMap["fb_width"]},height=${stateMap["fb_height"]} ")
                append("--vnc-server host=127.0.0.1,port=${stateMap["vnc_port"]} ")
                append("${stateMap["bios_path"]}")
            }
        }
    }
    val tabs = listOf(R.string.archive, R.string.processor, R.string.memory, R.string.disk, R.string.display, R.string.preview).map { stringResource(it) }
    ExpressiveCanvas(title = stringResource(R.string.add_virtual_machine), navigationItems = tabs, onAction = {
        try {
            val name = stateMap["name"].toString()
            val cfgKeys = listOf("name", "cpus", "mem", "hugepages", "balloon", "sandbox", "protected", "disk_path", "fb_width", "fb_height", "vnc_port", "bios_path")
            val cfgContent = buildString {
                cfgKeys.forEach { key -> stateMap[key]?.let { append("$key: $it\n") } }
                append("command: $commandBuilder")
            }.trim()
            val escapedCfg = cfgContent.replace("'", "'\\''")
            val targetDir = "$alsPath/app/cvm/$name"
            val targetFile = "$targetDir/$name.cfg"
            val shellScript = "mkdir -p \"$targetDir\"\necho '$escapedCfg' > \"$targetFile.tmp\"\nmv -f \"$targetFile.tmp\" \"\"$targetFile\"\nexit"
            DataOutputStream(Runtime.getRuntime().exec("su").outputStream).use { outputStream ->
                outputStream.writeBytes(shellScript + "\n")
                outputStream.flush()
            }
            onBack()
        } catch (_: Exception) {}
    }) { index ->
        val currentLocalFont = localFont.current
        when (index) {
            0 -> InputCell(stringResource(R.string.configuration_name), stateMap["name"].toString()) { stateMap["name"] = it }
            1 -> {
                InputCell(stringResource(R.string.cpu_cores), stateMap["cpus"].toString()) { stateMap["cpus"] = it }
                ToggleCell(stringResource(R.string.disable_sandbox), !(stateMap["sandbox"] as Boolean)) { stateMap["sandbox"] = !it }
            }
            2 -> {
                InputCell(stringResource(R.string.memory_size), stateMap["mem"].toString()) { stateMap["mem"] = it }
                ToggleCell(stringResource(R.string.enable_hugepages), stateMap["hugepages"] as Boolean) { stateMap["hugepages"] = it }
                ToggleCell(stringResource(R.string.protected_vm), stateMap["protected"] as Boolean) { stateMap["protected"] = it }
            }
            3 -> {
                InputCell(stringResource(R.string.disk_path), stateMap["disk_path"].toString()) { stateMap["disk_path"] = it }
                InputCell(stringResource(R.string.firmware_path), stateMap["bios_path"].toString()) { stateMap["bios_path"] = it }
                ToggleCell(stringResource(R.string.enable_balloon), stateMap["balloon"] as Boolean) { stateMap["balloon"] = it }
            }
            4 -> {
                InputCell(stringResource(R.string.fb_width), stateMap["fb_width"].toString()) { stateMap["fb_width"] = it }
                InputCell(stringResource(R.string.fb_height), stateMap["fb_height"].toString()) { stateMap["fb_height"] = it }
                InputCell(stringResource(R.string.vnc_port), stateMap["vnc_port"].toString()) { stateMap["vnc_port"] = it }
            }
            5 -> SelectionContainer {
                Text(commandBuilder, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), fontFamily = currentLocalFont)
            }
        }
    }
}
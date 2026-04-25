package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.ui.ALSList
import sui.k.als.boot.alsPath
@Composable
fun CVMDisk(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["disk_path"], stateMap["bios_path"], stateMap["balloon"]) {
        val disk = stateMap["disk_path"]?.toString() ?: ""
        val bios = stateMap["bios_path"] ?: "$alsPath/app/crosvm/edk2-gunyah.fd"
        val balloon = if (stateMap["balloon"] == false) "--no-balloon " else ""
        stateMap["disk"] = "--block $disk $balloon$bios"
    }
    ALSList(stringResource(R.string.disk_path), value = stateMap["disk_path"]?.toString() ?: "", first = true, onValueChange = { stateMap["disk_path"] = it })
    ALSList(stringResource(R.string.firmware_path), value = stateMap["bios_path"]?.toString() ?: "$alsPath/app/crosvm/edk2-gunyah.fd", onValueChange = { stateMap["bios_path"] = it })
    ALSList(stringResource(R.string.enable_balloon), checked = stateMap["balloon"] == true, last = true) { stateMap["balloon"] = stateMap["balloon"] != true }
}

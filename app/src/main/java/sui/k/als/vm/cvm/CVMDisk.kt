package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.boot.alsPath
import sui.k.als.vm.InputCell
import sui.k.als.vm.ToggleCell

@Composable
fun CVMDisk(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["disk_path"], stateMap["bios_path"], stateMap["balloon"]) {
        val disk = stateMap["disk_path"]?.toString() ?: ""
        val bios = stateMap["bios_path"] ?: "$alsPath/app/crosvm/edk2-gunyah.fd"
        val balloon = if (stateMap["balloon"] == false) "--no-balloon " else ""
        stateMap["disk"] = "--block $disk $balloon$bios"
    }
    InputCell(stringResource(R.string.disk_path), stateMap["disk_path"]?.toString() ?: "") { stateMap["disk_path"] = it }
    InputCell(stringResource(R.string.firmware_path), stateMap["bios_path"]?.toString() ?: "$alsPath/app/crosvm/edk2-gunyah.fd") { stateMap["bios_path"] = it }
    ToggleCell(stringResource(R.string.enable_balloon), stateMap["balloon"] == true) { stateMap["balloon"] = it }
}
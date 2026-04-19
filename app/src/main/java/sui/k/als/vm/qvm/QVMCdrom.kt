package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.ToggleCell

@Composable
fun QVMCdrom(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(Unit) {
        if (stateMap["cd_enabled"] == null) stateMap["cd_enabled"] = false
        if (stateMap["cd_path"] == null) stateMap["cd_path"] = ""
        if (stateMap["cd_boot"] == null) stateMap["cd_boot"] = "1"
    }
    LaunchedEffect(stateMap["cd_enabled"], stateMap["cd_path"], stateMap["cd_boot"]) {
        val enabled = stateMap["cd_enabled"] == true
        val path = stateMap["cd_path"]?.toString() ?: ""
        val boot = stateMap["cd_boot"] ?: "1"
        stateMap["cdrom"] =
            if (enabled && path.isNotEmpty()) "-drive file=\"$path\",if=none,id=dr1,format=raw,aio=threads,media=cdrom -device virtio-blk-pci,drive=dr1,bootindex=$boot " else ""
    }
    ToggleCell(
        stringResource(R.string.enable_cdrom), stateMap["cd_enabled"] == true
    ) { stateMap["cd_enabled"] = it }
    if (stateMap["cd_enabled"] == true) {
        QVMList(
            listOf(R.string.cdrom_path to "cd_path", R.string.boot_index to "cd_boot"), stateMap
        )
    }
}
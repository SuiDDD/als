package sui.k.als.vm.qvm
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import sui.k.als.R
import sui.k.als.ui.ALSButton
@Suppress("UNCHECKED_CAST")
@Composable
fun QVMAddDev(stateMap: MutableMap<String, Any>, onAdded: (Int) -> Unit) {
    val cdList = remember { stateMap["cdrom_list"] as? MutableList<MutableMap<String, Any>> ?: mutableStateListOf<MutableMap<String, Any>>().also { stateMap["cdrom_list"] = it } }
    val dList = remember { stateMap["disk_list"] as? MutableList<MutableMap<String, Any>> ?: mutableStateListOf<MutableMap<String, Any>>().also { stateMap["disk_list"] = it } }
    val nList = remember { stateMap["net_list"] as? MutableList<MutableMap<String, Any>> ?: mutableStateListOf<MutableMap<String, Any>>().also { stateMap["net_list"] = it } }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ALSButton(R.drawable.hard_drive) {
            dList.add(mutableStateMapOf("path" to "", "cache" to "unsafe", "aio" to "threads", "discard" to "unmap", "queues" to "$(nproc)"))
            onAdded(3 + dList.size - 1)
        }
        ALSButton(R.drawable.album) {
            cdList.add(mutableStateMapOf("path" to "", "boot" to (cdList.size + 2).toString()))
            onAdded(3 + dList.size + cdList.size - 1)
        }
        ALSButton(R.drawable.wifi) {
            nList.add(mutableStateMapOf("enabled" to true, "device" to "virtio-net-pci", "proto" to "tcp", "ports" to "2222-:22"))
            onAdded(3 + dList.size + cdList.size + nList.size - 1)
        }
        if (stateMap["display_enabled"] != true) ALSButton(R.drawable.square) {
            stateMap["display_enabled"] = true
            onAdded(3 + dList.size + cdList.size + nList.size)
        }
        if (stateMap["audio_enabled"] != true) ALSButton(R.drawable.volume_up) {
            stateMap["audio_enabled"] = true
            onAdded(3 + dList.size + cdList.size + nList.size + (if(stateMap["display_enabled"] == true) 1 else 0))
        }
    }
}

package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.ToggleCell

@Composable
fun QVMNetwork(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(Unit) {
        if (stateMap["network_enabled"] == null) stateMap["network_enabled"] = false
        if (stateMap["network_protocol"] == null) stateMap["network_protocol"] = "tcp"
        if (stateMap["network_port"] == null) stateMap["network_port"] = "2222-:22"
    }
    LaunchedEffect(
        stateMap["network_enabled"], stateMap["network_protocol"], stateMap["network_port"]
    ) {
        val enabled = stateMap["network_enabled"] == true
        val proto = stateMap["network_protocol"]?.toString() ?: "tcp"
        val ports = stateMap["network_port"]?.toString()?.split(",") ?: listOf("2222-:22")
        val hostfwdCmd = ports.joinToString(" ") { ",hostfwd=$proto::$it" }
        stateMap["network"] =
            if (enabled) "-netdev user,id=net0$hostfwdCmd -device virtio-net-pci,netdev=net0,disable-legacy=on,disable-modern=off " else ""
    }
    ToggleCell(
        stringResource(R.string.network_device), stateMap["network_enabled"] == true
    ) { stateMap["network_enabled"] = it }
    if (stateMap["network_enabled"] == true) {
        QVMList(
            listOf(
                R.string.network_protocol to "network_protocol",
                R.string.port_forwarding to "network_port"
            ), stateMap
        )
    }
}
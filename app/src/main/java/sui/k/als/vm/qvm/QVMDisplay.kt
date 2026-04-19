package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.ToggleCell

@Composable
fun QVMDisplay(stateMap: MutableMap<String, Any>) {
    val gpu = stateMap["gpu"] as? Boolean ?: false
    val port = stateMap["vnc_port"]?.toString() ?: "5900"
    val vnc = stateMap["vnc_enable"] as? Boolean ?: false
    val displayCmd by remember(gpu, port, vnc) {
        derivedStateOf {
            val displayNum = try {
                (port.toInt() - 5900).coerceAtLeast(0)
            } catch (_: Exception) {
                0
            }
            val resCmd =
                """$(wm size | sed -n 's/Physical size: \([0-9]*\)x\([0-9]*\)/xres=\2,yres=\1/p')"""
            buildString {
                if (gpu) {
                    append(" -device virtio-gpu-pci,$resCmd,disable-legacy=on,disable-modern=off")
                    if (vnc) append(" -vnc :$displayNum")
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (stateMap["gpu"] == null) stateMap["gpu"] = false
        if (stateMap["vnc_enable"] == null) stateMap["vnc_enable"] = false
        if (stateMap["vnc_port"] == null) stateMap["vnc_port"] = "5900"
    }
    SideEffect {
        stateMap["display"] = displayCmd
    }
    QVMList(listOf(R.string.vnc_port to "vnc_port"), stateMap)
    ToggleCell(stringResource(R.string.display_output), gpu) { stateMap["gpu"] = it }
}
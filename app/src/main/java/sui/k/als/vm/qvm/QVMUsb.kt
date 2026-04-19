package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import sui.k.als.R

@Composable
fun QVMUsb(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(Unit) {
        if (stateMap["p2"] == null) stateMap["p2"] = "9"
        if (stateMap["p3"] == null) stateMap["p3"] = "9"
    }
    LaunchedEffect(stateMap["p2"], stateMap["p3"]) {
        val p2 = stateMap["p2"] ?: "9"
        val p3 = stateMap["p3"] ?: "9"
        stateMap["usb"] =
            "-device qemu-xhci,id=usb-bus,p2=$p2,p3=$p3 -device usb-tablet,bus=usb-bus.0 -device usb-kbd,bus=usb-bus.0 "
    }
    QVMList(listOf(R.string.usb2_port to "p2", R.string.usb3_port to "p3"), stateMap)
}
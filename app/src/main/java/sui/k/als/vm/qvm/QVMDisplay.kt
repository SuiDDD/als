package sui.k.als.vm.qvm

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import sui.k.als.*
import sui.k.als.R
import sui.k.als.ui.*
import java.util.*

@Composable
fun QVMDisplay(state: MutableMap<String, Any>) {
    var showDeviceDiscovery by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(state["vnc_port"], state["display_device"], state["xres"], state["yres"]) {
        val device = state["display_device"] ?: "virtio-gpu-pci"
        val vnc = state.getOrPut("vnc_port") { ":0" }
        val x = state["xres"] ?: ""
        val y = state["yres"] ?: ""
        state["display"] = "-device $device,xres=$x,yres=$y -vnc $vnc "
    }
    Column {
        ALSList(
            stringResource(R.string.display_device),
            value = state["display_device"]?.toString() ?: "virtio-gpu-pci",
            first = true
        ) {
            val qvmPath = "$alsDir/app/qvm"
            val discoveryCommand =
                """LD_LIBRARY_PATH=$qvmPath/libs $qvmPath/qemu-system-aarch64 -M virt -device help 2>&1 | sed -n '/Display devices:/,/^$/p' | sed '1d' | awk -F'[" ,]' '{print $3}'"""
            runCatching {
                val discoveryProcess =
                    Runtime.getRuntime().exec(arrayOf(su, "-c", discoveryCommand))
                deviceList = Scanner(discoveryProcess.inputStream).useDelimiter("\n").asSequence()
                    .filter { it.isNotBlank() }.toList()
                if (deviceList.isNotEmpty()) showDeviceDiscovery = true
            }
        }
        ALSList(
            stringResource(R.string.xres),
            value = state["xres"]?.toString() ?: "",
            backgrounds = Color.Red.takeIf {
                (state["xres"]?.toString() ?: "").isEmpty()
            }) { state["xres"] = it }
        ALSList(
            stringResource(R.string.yres),
            value = state["yres"]?.toString() ?: "",
            backgrounds = Color.Red.takeIf {
                (state["yres"]?.toString() ?: "").isEmpty()
            }) { state["yres"] = it }
        ALSList(
            stringResource(R.string.vnc_port),
            value = state.getOrPut("vnc_port") { ":0" }.toString(),
            last = true,
            backgrounds = Color.Red.takeIf {
                state["vnc_port"]?.toString().isNullOrEmpty()
            }) { state["vnc_port"] = it }
    }
    ALSList(
        data = deviceList,
        show = showDeviceDiscovery,
        onDismiss = { showDeviceDiscovery = false }) {
        state["display_device"] = it
    }
}
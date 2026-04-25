package sui.k.als.vm.qvm

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.alsPath
import sui.k.als.su
import sui.k.als.ui.ALSList
import java.util.Scanner

@Composable
fun QVMDisplay(state: MutableMap<String, Any>) {
    var showDeviceDiscovery by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf(listOf<String>()) }
    val device = state["display_device"]?.toString() ?: "virtio-gpu-pci"

    if (state["vnc_port"] == null) state["vnc_port"] = ":0"
    val vncPort = state["vnc_port"]?.toString() ?: ""

    val xres = state["xres"]?.toString() ?: ""
    val yres = state["yres"]?.toString() ?: ""

    LaunchedEffect(vncPort, device, xres, yres) {
        state["display"] = "-device $device,xres=$xres,yres=$yres -vnc $vncPort "
    }

    Column {
        ALSList(data = stringResource(R.string.display_device), value = device, first = true) {
            val qvmPath = "$alsPath/app/qvm"
            val discoveryCommand =
                $$"LD_LIBRARY_PATH=$$qvmPath/libs $$qvmPath/qemu-system-aarch64 -M virt -device help 2>&1 | sed -n '/Display devices:/,/^$/p' | sed '1d' | awk -F'[\\\" ,]' '{print $3}'"
            try {
                val discoveryProcess =
                    Runtime.getRuntime().exec(arrayOf(su, "-c", discoveryCommand))
                deviceList = Scanner(discoveryProcess.inputStream).useDelimiter("\n").asSequence()
                    .filter { it.isNotBlank() }.toList()
                showDeviceDiscovery = true
            } catch (_: Exception) {
            }
        }
        ALSList(
            data = stringResource(R.string.xres),
            value = xres,
            backgrounds = if (xres.isEmpty()) Color.Red else null,
            onValueChange = { state["xres"] = it })
        ALSList(
            data = stringResource(R.string.yres),
            value = yres,
            backgrounds = if (yres.isEmpty()) Color.Red else null,
            onValueChange = { state["yres"] = it })
        ALSList(
            data = stringResource(R.string.vnc_port),
            value = vncPort,
            last = true,
            backgrounds = if (vncPort.isEmpty()) Color.Red else null,
            onValueChange = { state["vnc_port"] = it })
    }
    ALSList(
        data = deviceList, show = showDeviceDiscovery, onDismiss = { }) {
        state["display_device"] = it
    }
}
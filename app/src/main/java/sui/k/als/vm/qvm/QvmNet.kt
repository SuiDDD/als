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
fun QvmNetwork(state: MutableMap<String, Any>) {
    var showDeviceDiscovery by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf(listOf<String>()) }
    var showBackendSelection by remember { mutableStateOf(false) }
    val backendList = listOf("user", "tap")
    var showProtocolSelection by remember { mutableStateOf(false) }
    val protocolList = listOf("tcp", "udp")

    val dev = state["device"]?.toString() ?: "virtio-net-pci"
    val backend = state["backend"]?.toString() ?: "user"
    val protocol = state["protocol"]?.toString() ?: "tcp"
    val ports = state["ports"]?.toString() ?: "2222-:22"

    Column {
        ALSList(stringResource(R.string.network_device), value = dev, first = true) {
            val qvmDir = "$alsDir/app/qvm"
            val discoveryCommand =
                $$"LD_LIBRARY_PATH=$$qvmDir/libs $$qvmDir/qemu-system-aarch64 -M virt -device help 2>&1 | sed -n '/Network devices:/,/^$/p' | sed '1d' | awk -F'[\\\\\" ,]' '{print $3}'"
            try {
                val discoveryProcess =
                    Runtime.getRuntime().exec(arrayOf(su, "-c", discoveryCommand))
                deviceList = Scanner(discoveryProcess.inputStream).useDelimiter("\n").asSequence()
                    .filter { it.isNotBlank() }.toList()
                showDeviceDiscovery = true
            } catch (_: Exception) {
            }
        }
        ALSList(stringResource(R.string.network_backend), value = backend) {
            showBackendSelection = true
        }
        if (backend == "user") {
            ALSList(
                stringResource(R.string.network_protocol), value = protocol
            ) { showProtocolSelection = true }
            ALSList(
                stringResource(R.string.port_forwarding),
                value = ports,
                last = true,
                background = if (ports.isEmpty()) Color.Red else null
            ) { state["ports"] = it }
        } else {
            ALSList(
                stringResource(R.string.network_backend), value = backend, last = true
            ) { showBackendSelection = true }
        }
    }

    ALSList(
        data = deviceList, show = showDeviceDiscovery, onDismiss = { }) {
        state["device"] = it
    }
    ALSList(
        data = backendList, show = showBackendSelection, onDismiss = { }) {
        state["backend"] = it
    }
    ALSList(
        data = protocolList, show = showProtocolSelection, onDismiss = { }) {
        state["protocol"] = it
    }
}
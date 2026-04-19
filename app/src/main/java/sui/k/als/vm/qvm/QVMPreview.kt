package sui.k.als.vm.qvm

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import sui.k.als.localFont

@Composable
fun QVMPreview(stateMap: MutableMap<String, Any>) {
    val qvmPath = "/data/local/tmp/als/app/qvm"
    val command by remember {
        derivedStateOf {
            buildString {
                append(stateMap["archive"]?.toString() ?: "")
                append("env LD_LIBRARY_PATH=$qvmPath/libs ")
                append("$qvmPath/qemu-system-aarch64 ")
                append("-L $qvmPath/pc-bios ")
                append("-M virt,confidential-guest-support=prot0 ")
                append("-accel gunyah ")
                append(stateMap["audio_cmd"]?.toString() ?: "")
                append("-bios $qvmPath/QEMU_EFI.fd ")
                append(stateMap["cdrom"]?.toString() ?: "")
                append("-cpu host ")
                append(stateMap["disk"]?.toString() ?: "")
                append(stateMap["display"]?.toString() ?: "")
                append(stateMap["memory"]?.toString() ?: "")
                append(stateMap["network"]?.toString() ?: "")
                append(stateMap["processor"]?.toString() ?: "")
                append("-serial mon:stdio ")
                append(stateMap["usb"]?.toString() ?: "")
            }.trim().also { stateMap["qvmcmd"] = it }
        }
    }
    SelectionContainer {
        Text(command, fontSize = 9.sp, color = Color.Gray, fontFamily = localFont.current)
    }
}
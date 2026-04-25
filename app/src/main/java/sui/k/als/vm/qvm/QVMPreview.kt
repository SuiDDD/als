package sui.k.als.vm.qvm

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import sui.k.als.localFont
import sui.k.als.vm.qvmPath

object QVMPreview {
    fun buildQemuCommand(data: Any): String {
        val get =
            { k: String -> if (data is JSONObject) data.opt(k) else (data as? Map<*, *>)?.get(k) }
        val isTrue = { k: String -> val v = get(k); v == true || v == "true" || v == 1 || v == "1" }
        return buildString {
            append("LD_LIBRARY_PATH=$qvmPath/libs ")
            append("$qvmPath/qemu-system-aarch64 ")
            append("-M virt,confidential-guest-support=prot0 ")
            append("-accel gunyah ")
            append("-cpu host ")
            append("-smp ${get("smp") ?: get("cores") ?: "$(nproc)"} ")
            append("-m ${get("mem") ?: "6G"} ")
            append("-object arm-confidential-guest,id=prot0,swiotlb-size=${get("swiotlb") ?: "64M"} ")
            if (isTrue("prealloc")) {
                val sz = get("prealloc_size")?.toString() ?: "6G"
                val mb = if (sz.endsWith("G")) (sz.removeSuffix("G")
                    .toInt() * 1024).toString() + "M" else sz
                append("-object memory-backend-ram,id=mem,size=$mb,prealloc=on ")
                if (isTrue("force")) append("-mem-prealloc ")
                if (isTrue("lock")) append("-overcommit mem-lock=on ")
            }
            append("-bios $qvmPath/QEMU_EFI.fd ")
            append("-L $qvmPath/pc-bios ")
            val getList = { k: String ->
                when (val v = get(k)) {
                    is JSONArray -> List(v.length()) { v.get(it) }
                    is List<*> -> v
                    else -> emptyList<Any>()
                }
            }
            getList("cdrom").forEachIndexed { i, cd ->
                val m = if (cd is JSONObject) null else cd as? Map<*, *>
                val p = (cd as? JSONObject)?.optString("path") ?: m?.get("path")?.toString() ?: ""
                if (p.isNotEmpty()) {
                    val idx = (cd as? JSONObject)?.optString("index") ?: m?.get("index")?.toString()
                    ?: (i + 1).toString()
                    append("-drive file=\"$p\",if=none,id=dr_cd$i,format=raw,media=cdrom -device virtio-blk-pci,drive=dr_cd$i,bootindex=$idx ")
                }
            }
            getList("disk").forEachIndexed { i, d ->
                val m = if (d is JSONObject) null else d as? Map<*, *>
                val p = (d as? JSONObject)?.optString("path") ?: m?.get("path")?.toString() ?: ""
                if (p.isNotEmpty()) {
                    val cache =
                        (d as? JSONObject)?.optString("cache") ?: m?.get("cache")?.toString()
                        ?: "unsafe"
                    val idx = (d as? JSONObject)?.optString("index") ?: m?.get("index")?.toString()
                    ?: (i + 2).toString()
                    append("-drive file=\"$p\",if=none,id=dr_d$i,cache=$cache -device virtio-blk-pci,drive=dr_d$i,bootindex=$idx ")
                }
            }
            getList("net").forEachIndexed { i, n ->
                val m = if (n is JSONObject) null else n as? Map<*, *>
                val backend =
                    (n as? JSONObject)?.optString("backend") ?: m?.get("backend")?.toString()
                    ?: "user"
                val proto = (n as? JSONObject)?.optString("protocol")
                    ?: (n as? JSONObject)?.optString("proto") ?: m?.get("protocol")
                    ?: m?.get("proto") ?: "tcp"
                val ports = (n as? JSONObject)?.optString("ports") ?: m?.get("ports")?.toString()
                ?: "2222-:22"
                val dev = (n as? JSONObject)?.optString("device") ?: m?.get("device")?.toString()
                ?: "virtio-net-pci"
                append("-netdev $backend,id=net$i,hostfwd=$proto::$ports -device $dev,netdev=net$i ")
            }
            val x = get("xres")?.toString() ?: ""
            val y = get("yres")?.toString() ?: ""
            val vnc = get("vnc_port")?.toString() ?: ":0"
            if (x.isNotEmpty() && y.isNotEmpty()) append("-device virtio-gpu-pci,xres=$x,yres=$y -vnc $vnc ")
            else append("-device virtio-gpu-pci -vnc $vnc ")
            if (isTrue("audio") || isTrue("audio_enabled")) append("-audiodev aaudio,id=snd0 -device virtio-sound-pci,audiodev=snd0 ")
            if (isTrue("usb") || isTrue("usb_enabled")) append("-device qemu-xhci,id=usb,bus=pcie.0,addr=0x4 -device usb-tablet,bus=usb.0,port=1 -device usb-kbd,bus=usb.0,port=2 ")
            append("-serial mon:stdio ")
        }.replace("\\s+".toRegex(), " ").trim()
    }
}

@Composable
fun QVMPreview(stateMap: MutableMap<String, Any>) {
    val command = QVMPreview.buildQemuCommand(stateMap)
    stateMap["qvmcmd"] = command
    SelectionContainer {
        Text(
            command, fontSize = 9.sp, color = Color.Gray, fontFamily = localFont.current
        )
    }
}
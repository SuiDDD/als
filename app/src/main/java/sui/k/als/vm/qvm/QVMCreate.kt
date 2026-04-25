package sui.k.als.vm.qvm

import androidx.compose.runtime.*
import org.json.JSONArray
import sui.k.als.R
import sui.k.als.su
import sui.k.als.vm.ExpressiveCanvas
import sui.k.als.vm.QVMConfig

@Composable
fun QVMCreate(config: QVMConfig? = null, onBack: () -> Unit) {
    val qvmMap = remember { mutableStateMapOf<String, Any>().apply { 
        config?.raw?.let { raw -> raw.keys().forEach { key -> put(key, raw.get(key)) } }
        if (config != null) put("name", config.name)
        if (get("cores") == null) put("cores", "$(nproc)")
        if (get("threads") == null) put("threads", "1")
        if (get("sockets") == null) put("sockets", "1")
        if (get("smp") == null) put("smp", "$(nproc)")
    } }

    LaunchedEffect(Unit) {
        if (qvmMap["xres"] == null || qvmMap["yres"] == null) {
            try {
                val resX = Runtime.getRuntime().exec(arrayOf(su, "-c", "wm size | cut -d' ' -f3 | cut -d'x' -f2")).inputStream.bufferedReader().readText().trim()
                val resY = Runtime.getRuntime().exec(arrayOf(su, "-c", "wm size | cut -d' ' -f3 | cut -d'x' -f1")).inputStream.bufferedReader().readText().trim()
                if (resX.isNotEmpty()) qvmMap["xres"] = resX
                if (resY.isNotEmpty()) qvmMap["yres"] = resY
            } catch (_: Exception) {}
        }
    }

    val cdroms = remember { 
        val list = mutableStateListOf<MutableMap<String, Any>>()
        val array = qvmMap["cdrom"] as? JSONArray
        if (array != null) {
            for (idx in 0 until array.length()) {
                val obj = array.getJSONObject(idx)
                val map = mutableStateMapOf<String, Any>()
                obj.keys().forEach { key -> map[key] = obj.get(key) }
                list.add(map)
            }
        } else if (config == null) {
            list.add(mutableStateMapOf("path" to "", "index" to "1"))
        }
        list
    }

    val disks = remember {
        val list = mutableStateListOf<MutableMap<String, Any>>()
        val array = qvmMap["disk"] as? JSONArray
        if (array != null) {
            for (idx in 0 until array.length()) {
                val obj = array.getJSONObject(idx)
                val map = mutableStateMapOf<String, Any>()
                obj.keys().forEach { key -> map[key] = obj.get(key) }
                list.add(map)
            }
        } else if (config == null) {
            list.add(mutableStateMapOf("path" to "", "cache" to "unsafe", "index" to "2"))
        }
        list
    }

    val networks = remember {
        val list = mutableStateListOf<MutableMap<String, Any>>()
        val array = qvmMap["net"] as? JSONArray
        if (array != null) {
            for (idx in 0 until array.length()) {
                val obj = array.getJSONObject(idx)
                val map = mutableStateMapOf<String, Any>()
                obj.keys().forEach { key -> map[key] = obj.get(key) }
                list.add(map)
            }
        } else if (config == null) {
            list.add(mutableStateMapOf("device" to "virtio-net-pci", "backend" to "user", "protocol" to "tcp", "ports" to "2222-:22"))
        }
        list
    }
    
    val activeIndex = remember { mutableIntStateOf(0) }

    LaunchedEffect(cdroms.map { it["path"].toString() + it["index"].toString() }, disks.map { it["path"].toString() + it["index"].toString() }, networks.map { it["device"].toString() + it["backend"].toString() + it["protocol"].toString() + it["ports"].toString() }, qvmMap["audio"], qvmMap["xres"], qvmMap["yres"], qvmMap["vnc_port"]) {
        qvmMap["cdrom_cmd"] = buildString { cdroms.forEachIndexed { i, cd -> val path = cd["path"]?.toString() ?: ""; if (path.isNotEmpty()) append("-drive file=\"$path\",if=none,id=dr_cd$i,format=raw,media=cdrom -device virtio-blk-pci,drive=dr_cd$i,bootindex=${cd["index"] ?: (i+1)} ") } }
        qvmMap["disk_cmd"] = buildString { disks.forEachIndexed { i, disk -> val path = disk["path"]?.toString() ?: ""; if (path.isNotEmpty()) append("-drive file=\"$path\",if=none,id=dr_d$i,cache=${disk["cache"]?: "unsafe"} -device virtio-blk-pci,drive=dr_d$i,bootindex=${disk["index"] ?: (i+2)} ") } }
        qvmMap["network_cmd"] = buildString { networks.forEachIndexed { i, net -> append("-netdev ${net["backend"] ?: "user"},id=net$i,hostfwd=${net["protocol"]?: "tcp"}::${net["ports"]?: "2222-:22"} -device ${net["device"] ?: "virtio-net-pci"},netdev=net$i ") } }
        val xres = qvmMap["xres"] ?: ""; val yres = qvmMap["yres"] ?: ""
        val vncPort = qvmMap["vnc_port"]?.toString() ?: "9000"
        qvmMap["display_cmd"] = if (xres != "" && yres != "") "-device virtio-gpu-pci,xres=$xres,yres=$yres,disable-legacy=on,disable-modern=off -vnc 0.0.0.0:$vncPort " else "-device virtio-gpu-pci -vnc 0.0.0.0:$vncPort "
        qvmMap["audio_cmd"] = if (qvmMap["audio"] == 1) "-audiodev aaudio,id=snd0 -device virtio-sound-pci,audiodev=snd0,disable-legacy=on,disable-modern=off " else ""
    }

    val icons = remember(cdroms.size, disks.size, networks.size, qvmMap["audio"]) {
        val list = mutableListOf(R.drawable.archive, R.drawable.chips, R.drawable.memory)
        repeat(cdroms.size) { list.add(R.drawable.album) }
        repeat(disks.size) { list.add(R.drawable.hard_drive) }
        repeat(networks.size) { list.add(R.drawable.wifi) }
        list.add(R.drawable.crop_landscape)
        if (qvmMap["audio"] == 1) list.add(R.drawable.volume_up)
        list.add(R.drawable.add)
        list.add(R.drawable.preview)
        list
    }

    ExpressiveCanvas(icons = icons, activeIndex = activeIndex.intValue, onIndexChange = { activeIndex.intValue = it }, onLongClick = { index ->
        val startCdrom = 3; val startDisk = startCdrom + cdroms.size; val startNetwork = startDisk + disks.size; val startDisplay = startNetwork + networks.size
        when (index) {
            in startCdrom until startDisk -> { cdroms.removeAt(index - startCdrom); activeIndex.intValue = 0 }
            in startDisk until startNetwork -> { disks.removeAt(index - startDisk); activeIndex.intValue = 0 }
            in startNetwork until startDisplay -> { networks.removeAt(index - startNetwork); activeIndex.intValue = 0 }
            startDisplay + 1 -> if (qvmMap["audio"] == 1) { qvmMap["audio"] = 0; activeIndex.intValue = 0 } else { activeIndex.intValue = 0 }
        }
    }, onAction = {
        if (qvmMap["name"]?.toString()?.isBlank() != false) { activeIndex.intValue = 0; return@ExpressiveCanvas }

        try {
            val content = buildString {
                val base = listOf("name", "mem", "swiotlb", "cores", "threads", "sockets", "smp", "xres", "yres", "vnc_port")
                base.forEach { k -> qvmMap[k]?.let { v -> append("$k:$v\n") } }
                append("audio:${if(qvmMap["audio"]==1) 1 else 0}\n")
                cdroms.forEachIndexed { i, m -> m.forEach { (k, v) -> append("cdrom${i+1}.$k:$v\n") } }
                disks.forEachIndexed { i, m -> m.forEach { (k, v) -> append("disk${i+1}.$k:$v\n") } }
                networks.forEachIndexed { i, m -> m.forEach { (k, v) -> append("net${i+1}.$k:$v\n") } }
            }.trim()

            val dir = "/data/local/tmp/als/app/qvm/${qvmMap["name"]}"
            val cmd = "mkdir -p \"$dir\"\necho '${content.replace("'", "'\\''")}' > \"$dir/${qvmMap["name"]}.cfg\"\nexit\n"
            val proc = Runtime.getRuntime().exec(su)
            proc.outputStream.use { it.write(cmd.toByteArray(Charsets.UTF_8)); it.flush() }
            proc.waitFor()
            onBack()
        } catch (_: Exception) {}
    }) { index ->
        val startCdrom = 3; val startDisk = startCdrom + cdroms.size; val startNetwork = startDisk + disks.size; val startDisplay = startNetwork + networks.size
        val startAudio = startDisplay + 1
        val startAdd = startAudio + (if(qvmMap["audio"] == 1) 1 else 0)
        when (index) {
            in 0 until startCdrom -> when (index) { 0 -> QVMArchive(qvmMap); 1 -> QVMProcessor(qvmMap); else -> QVMMemory(qvmMap) }
            in startCdrom until startDisk -> QVMCdrom(cdroms[index - startCdrom])
            in startDisk until startNetwork -> QVMDisk(disks[index - startDisk])
            in startNetwork until startDisplay -> QVMNetwork(networks[index - startNetwork])
            startDisplay -> QVMDisplay(qvmMap)
            in startAudio until startAdd -> QVMAudio(qvmMap)
            startAdd -> QVMAddDev(qvmMap) { activeIndex.intValue = it }
            else -> QVMPreview(qvmMap)
        }
    }
}

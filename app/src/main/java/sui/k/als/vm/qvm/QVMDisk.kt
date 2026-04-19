package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.ToggleCell

@Composable
fun QVMDisk(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(Unit) {
        if (stateMap["path"] == null) stateMap["path"] = ""
        if (stateMap["cache"] == null) stateMap["cache"] = "unsafe"
        if (stateMap["aio"] == null) stateMap["aio"] = "threads"
        if (stateMap["discard"] == null) stateMap["discard"] = "unmap"
        if (stateMap["queues"] == null) stateMap["queues"] = "$(nproc)"
    }
    LaunchedEffect(
        stateMap["path"],
        stateMap["cache"],
        stateMap["aio"],
        stateMap["discard"],
        stateMap["queues"],
        stateMap["io_optimization"],
        stateMap["cd_enabled"]
    ) {
        val path = stateMap["path"]?.toString()?.takeIf { it.isNotEmpty() } ?: ""
        val cache = stateMap["cache"] ?: "unsafe"
        val aio = stateMap["aio"] ?: "threads"
        val discard = stateMap["discard"] ?: "unmap"
        val queues = stateMap["queues"] ?: "$(nproc)"
        val iothread = if (stateMap["io_optimization"] == true) ",iothread=io0" else ""
        val bootIndex = if (stateMap["cd_enabled"] == true) "2" else "1"
        val ioObj = if (stateMap["io_optimization"] == true) "-object iothread,id=io0 " else ""
        stateMap["disk"] =
            "${ioObj}-drive file=\"$path\",if=none,id=dr0,cache=$cache,aio=$aio,discard=$discard -device virtio-blk-pci,drive=dr0,num-queues=$queues$iothread,disable-legacy=on,disable-modern=off,bootindex=$bootIndex "
    }
    QVMList(
        listOf(
            R.string.disk_path to "path",
            R.string.cache_mode to "cache",
            R.string.aio_mode to "aio",
            R.string.discard_mode to "discard",
            R.string.queue_count to "queues"
        ), stateMap
    )
    ToggleCell(
        stringResource(R.string.io_thread_optimization), stateMap["io_optimization"] == true
    ) { stateMap["io_optimization"] = it }
}
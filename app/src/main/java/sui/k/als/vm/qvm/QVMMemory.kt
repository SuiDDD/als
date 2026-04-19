package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.ToggleCell

@Composable
fun QVMMemory(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(Unit) {
        if (stateMap["mem"] == null) stateMap["mem"] = "6G"
        if (stateMap["swiotlb"] == null) stateMap["swiotlb"] = "64M"
        if (stateMap["prealloc_size"] == null) stateMap["prealloc_size"] = "6G"
    }
    LaunchedEffect(
        stateMap["mem"],
        stateMap["swiotlb"],
        stateMap["prealloc"],
        stateMap["prealloc_size"],
        stateMap["force_prealloc"],
        stateMap["mem_lock"]
    ) {
        val mem = stateMap["mem"] ?: "6G"
        val swiotlb = stateMap["swiotlb"] ?: "64M"
        var cmd = " -m $mem -object arm-confidential-guest,id=prot0,swiotlb-size=$swiotlb "
        if (stateMap["prealloc"] == true) {
            val size = stateMap["prealloc_size"]?.toString() ?: "6G"
            val mbSize = if (size.endsWith("G")) (size.removeSuffix("G")
                .toInt() * 1024).toString() + "M" else size
            cmd += "-object memory-backend-ram,id=mem,size=$mbSize,prealloc=on "
            if (stateMap["force_prealloc"] == true) cmd += "-mem-prealloc "
            if (stateMap["mem_lock"] == true) cmd += "-overcommit mem-lock=on "
        }
        stateMap["memory"] = cmd
    }
    QVMList(
        listOf(R.string.memory_size to "mem", R.string.swiotlb_buffer_size to "swiotlb"), stateMap
    )
    ToggleCell(
        stringResource(R.string.prealloc), stateMap["prealloc"] == true
    ) { stateMap["prealloc"] = it }
    if (stateMap["prealloc"] == true) {
        QVMList(listOf(R.string.alloc_size to "prealloc_size"), stateMap)
        ToggleCell(
            stringResource(R.string.force_alloc), stateMap["force_prealloc"] == true
        ) { stateMap["force_prealloc"] = it }
        ToggleCell(
            stringResource(R.string.mem_lock), stateMap["mem_lock"] == true
        ) { stateMap["mem_lock"] = it }
    }
}
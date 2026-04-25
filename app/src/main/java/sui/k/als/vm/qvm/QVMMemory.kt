package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.ui.ALSList

@Composable
fun QVMMemory(state: MutableMap<String, Any>) {
    if (state["mem"] == null) state["mem"] = "6G"
    if (state["swiotlb"] == null) state["swiotlb"] = "64M"
    LaunchedEffect(
        state["mem"],
        state["swiotlb"],
        state["prealloc_size"],
        state["prealloc"],
        state["force"],
        state["lock"]
    ) {
        val mem = state["mem"].toString()
        val sw = state["swiotlb"].toString()
        var memCmd = "-m $mem "
        var objCmd = "-object arm-confidential-guest,id=prot0,swiotlb-size=$sw "
        if (state["prealloc"] == true) {
            val sz = state["prealloc_size"]?.toString() ?: "6G"
            val mb =
                if (sz.endsWith("G")) (sz.removeSuffix("G").toInt() * 1024).toString() + "M" else sz
            objCmd += "-object memory-backend-ram,id=mem,size=$mb,prealloc=on "
            if (state["force"] == true) memCmd += "-mem-prealloc "
            if (state["lock"] == true) memCmd += "-overcommit mem-lock=on "
        }
        state["memory"] = memCmd
        state["objects"] = objCmd
    }
    val pre = state["prealloc"] == true
    val mem = state["mem"]?.toString() ?: ""
    val sw = state["swiotlb"]?.toString() ?: ""
    val ps = state["prealloc_size"]?.toString() ?: ""

    ALSList(
        stringResource(R.string.memory_size),
        value = mem,
        first = true,
        backgrounds = if (mem.isEmpty()) Color.Red else null,
        onValueChange = { state["mem"] = it })
    ALSList(
        stringResource(R.string.swiotlb_buffer_size),
        value = sw,
        backgrounds = if (sw.isEmpty()) Color.Red else null,
        onValueChange = { state["swiotlb"] = it })

    ALSList(stringResource(R.string.prealloc), checked = pre, last = !pre) {
        state["prealloc"] = state["prealloc"] != true
    }
    if (pre) {
        ALSList(
            stringResource(R.string.alloc_size),
            value = ps,
            backgrounds = if (ps.isEmpty()) Color.Red else null,
            onValueChange = { state["prealloc_size"] = it })
        ALSList(
            stringResource(R.string.force_alloc), checked = state["force"] == true
        ) { state["force"] = state["force"] != true }
        ALSList(
            stringResource(R.string.mem_lock), checked = state["lock"] == true, last = true
        ) { state["lock"] = state["lock"] != true }
    }
}
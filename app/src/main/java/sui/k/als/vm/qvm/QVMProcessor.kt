package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import sui.k.als.R

@Composable
fun QVMProcessor(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(Unit) {
        if (stateMap["smp"] == null) stateMap["smp"] = "$(nproc)"
        if (stateMap["sockets"] == null) stateMap["sockets"] = "1"
        if (stateMap["cores"] == null) stateMap["cores"] = "$(nproc)"
        if (stateMap["threads"] == null) stateMap["threads"] = "1"
    }
    LaunchedEffect(stateMap["smp"], stateMap["sockets"], stateMap["cores"], stateMap["threads"]) {
        val smp = stateMap["smp"] ?: "$(nproc)"
        val sockets = stateMap["sockets"] ?: "1"
        val cores = stateMap["cores"] ?: "$(nproc)"
        val threads = stateMap["threads"] ?: "1"
        stateMap["processor"] = "-smp $smp,sockets=$sockets,cores=$cores,threads=$threads "
    }
    QVMList(
        listOf(
            R.string.smp to "smp",
            R.string.cpu_cores to "cores",
            R.string.cpu_sockets to "sockets",
            R.string.cpu_threads to "threads"
        ), stateMap
    )
}
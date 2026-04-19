package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import sui.k.als.R

@Composable
fun QVMArchive(stateMap: MutableMap<String, Any>) {
    val name = stateMap["name"]?.toString() ?: ""
    val niceValue = stateMap["nice"]?.toString()?.takeIf { it.isNotEmpty() } ?: "-20"
    LaunchedEffect(Unit) {
        if (stateMap["name"] == null) stateMap["name"] = ""
        if (stateMap["nice"] == null) stateMap["nice"] = "-20"
    }
    LaunchedEffect(name, niceValue) {
        stateMap["archive"] =
            "nice -n $niceValue taskset $(printf '%x' $(( (1 << $(nproc)) - 1 ))) "
    }
    QVMList(listOf(R.string.cfg_name to "name", R.string.nice_value to "nice"), stateMap)
}
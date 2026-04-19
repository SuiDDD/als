package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.InputCell
import sui.k.als.vm.ToggleCell

@Composable
fun CVMMemory(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["mem"], stateMap["hugepages"], stateMap["protected"]) {
        val mem = stateMap["mem"] ?: "2048"
        val hp = if (stateMap["hugepages"] == true) "--hugepages " else ""
        val prot = if (stateMap["protected"] == true) "--protected-vm-without-firmware " else ""
        stateMap["memory"] = "--mem $mem $hp$prot"
    }
    InputCell(stringResource(R.string.memory_size), stateMap["mem"]?.toString() ?: "2048") { stateMap["mem"] = it }
    ToggleCell(stringResource(R.string.enable_hugepages), stateMap["hugepages"] == true) { stateMap["hugepages"] = it }
    ToggleCell(stringResource(R.string.protected_vm), stateMap["protected"] == true) { stateMap["protected"] = it }
}
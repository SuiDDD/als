package sui.k.als.vm.cvm
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.ui.ALSList
@Composable
fun CVMMemory(state: MutableMap<String, Any>) {
    LaunchedEffect(state["mem"], state["hugepages"], state["protected"]) {
        val mem = state["mem"] ?: "2048"
        val hp = if (state["hugepages"] == true) "--hugepages " else ""
        val prot = if (state["protected"] == true) "--protected-vm-without-firmware " else ""
        state["memory"] = "--mem $mem $hp$prot"
    }
    ALSList(stringResource(R.string.memory_size), value = state["mem"]?.toString() ?: "2048", first = true, onValueChange = { state["mem"] = it })
    ALSList(stringResource(R.string.enable_hugepages), checked = state["hugepages"] == true) { state["hugepages"] = state["hugepages"] != true }
    ALSList(stringResource(R.string.protected_vm), checked = state["protected"] == true, last = true) { state["protected"] = state["protected"] != true }
}

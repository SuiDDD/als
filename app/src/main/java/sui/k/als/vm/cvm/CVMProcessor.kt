package sui.k.als.vm.cvm

import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import sui.k.als.R
import sui.k.als.ui.*

@Composable
fun CVMProcessor(state: MutableMap<String, Any>) {
    LaunchedEffect(state["cpus"], state["sandbox"]) {
        val cpus = state["cpus"] ?: "1"
        val sandbox = if (state["sandbox"] == false) "--disable-sandbox " else ""
        state["processor"] = "--cpus $cpus $sandbox"
    }
    ALSList(
        stringResource(R.string.cpu_cores),
        value = state["cpus"]?.toString() ?: "1",
        first = true,
        onValueChange = { state["cpus"] = it })
    ALSList(
        stringResource(R.string.disable_sandbox), checked = state["sandbox"] == false, last = true
    ) { state["sandbox"] = state["sandbox"] == true }
}
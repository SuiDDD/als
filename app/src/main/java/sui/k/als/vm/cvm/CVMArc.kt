package sui.k.als.vm.cvm

import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import sui.k.als.R
import sui.k.als.ui.*

@Composable
fun CVMArc(state: MutableMap<String, Any>) {
    LaunchedEffect(state["name"]) { state["archive"] = "--name ${state["name"] ?: "Windows"} " }
    ALSList(
        data = stringResource(R.string.cfg_name),
        value = state["name"]?.toString() ?: "Windows",
        first = true,
        last = true,
        onValueChange = { state["name"] = it })
}
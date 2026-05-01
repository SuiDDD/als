package sui.k.als.vm.qvm

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import sui.k.als.R
import sui.k.als.ui.*

@Composable
fun QVMArchive(state: MutableMap<String, Any>) {
    val name = state["name"]?.toString() ?: ""
    ALSList(
        stringResource(R.string.cfg_name),
        value = name,
        first = true,
        last = true,
        backgrounds = if (name.isEmpty()) Color.Red else null,
        onValueChange = { state["name"] = it })
}
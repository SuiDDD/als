package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import sui.k.als.vm.InputCell

@Composable
fun QVMList(items: List<Pair<Int, String>>, stateMap: MutableMap<String, Any>) {
    items.forEach { p ->
        InputCell(stringResource(p.first), stateMap[p.second].toString()) {
            stateMap[p.second] = it
        }
    }
}
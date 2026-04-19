package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.InputCell

@Composable
fun CVMArc(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["name"]) {
        val name = stateMap["name"] ?: "Windows"
        stateMap["archive"] = "--name $name "
    }
    InputCell(stringResource(R.string.cfg_name), stateMap["name"]?.toString() ?: "Windows") { stateMap["name"] = it }
}
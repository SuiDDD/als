package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.InputCell
import sui.k.als.vm.ToggleCell

@Composable
fun CVMProcessor(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["cpus"], stateMap["sandbox"]) {
        val cpus = stateMap["cpus"] ?: "1"
        val sandbox = if (stateMap["sandbox"] == false) "--disable-sandbox " else ""
        stateMap["processor"] = "--cpus $cpus $sandbox"
    }
    InputCell(stringResource(R.string.cpu_cores), stateMap["cpus"]?.toString() ?: "1") { stateMap["cpus"] = it }
    ToggleCell(stringResource(R.string.disable_sandbox), stateMap["sandbox"] == false) { stateMap["sandbox"] = !it }
}
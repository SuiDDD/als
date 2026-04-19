package sui.k.als.vm.cvm
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.InputCell

@Composable
fun CVMDisplay(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["fb_width"], stateMap["fb_height"], stateMap["vnc_port"]) {
        val w = stateMap["fb_width"] ?: "1024"
        val h = stateMap["fb_height"] ?: "768"
        val port = stateMap["vnc_port"] ?: "5900"
        stateMap["display"] = "--simplefb width=$w,height=$h --vnc-server host=127.0.0.1,port=$port "
    }
    InputCell(stringResource(R.string.fb_width), stateMap["fb_width"]?.toString() ?: "1024") { stateMap["fb_width"] = it }
    InputCell(stringResource(R.string.fb_height), stateMap["fb_height"]?.toString() ?: "768") { stateMap["fb_height"] = it }
    InputCell(stringResource(R.string.vnc_port), stateMap["vnc_port"]?.toString() ?: "5900") { stateMap["vnc_port"] = it }
}
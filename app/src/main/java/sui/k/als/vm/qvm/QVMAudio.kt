package sui.k.als.vm.qvm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import sui.k.als.R
import sui.k.als.vm.ToggleCell

@Composable
fun QVMAudio(stateMap: MutableMap<String, Any>) {
    LaunchedEffect(stateMap["audio"]) {
        stateMap["audio_cmd"] =
            if (stateMap["audio"] == true) "-audiodev aaudio,id=snd0 -device virtio-sound-pci,audiodev=snd0,disable-legacy=on,disable-modern=off " else ""
    }
    ToggleCell(
        stringResource(R.string.audio_output), stateMap["audio"] == true
    ) { stateMap["audio"] = it }
}
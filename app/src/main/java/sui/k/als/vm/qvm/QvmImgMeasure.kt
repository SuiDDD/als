package sui.k.als.vm.qvm

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import sui.k.als.*
import sui.k.als.R
import sui.k.als.ui.*

@Composable
fun QvmImgMeasure(onPreview: (String) -> Unit, onExecute: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var targetFormat by remember { mutableStateOf("qcow2") }
    val formats = listOf("qcow2", "raw", "vmdk", "vdi", "qcow", "qed")
    var showTargetFormatSelector by remember { mutableStateOf(false) }

    Column {
        ALSList(data = stringResource(R.string.disk_path), value = path, first = true, background = if (path.isEmpty()) Color.Red.copy(alpha = 0.1f) else null, onValueChange = { path = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.image_size), value = size, onValueChange = { size = it })
        ALSList(data = stringResource(R.string.target_format), value = targetFormat, onClick = { showTargetFormatSelector = true }, last = true)

        Spacer(Modifier.height(9.dp))

        ALSList(data = stringResource(R.string.preview), value = null, onClick = {
            onPreview("measure ${if(path.isNotEmpty()) "\"$path\"" else "--size $size"} -O $targetFormat")
        })
        ALSList(data = stringResource(R.string.execute_action), value = null, onClick = {
            onExecute("measure ${if(path.isNotEmpty()) "\"$path\"" else "--size $size"} -O $targetFormat")
        })
    }

    if (showTargetFormatSelector) {
        ALSList(data = formats, show = true, onDismiss = { showTargetFormatSelector = false }, onClick = { targetFormat = it; showTargetFormatSelector = false })
    }
}

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
fun QvmImgConvert(onPreview: (String) -> Unit, onExecute: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    var targetPath by remember { mutableStateOf("") }
    var targetFormat by remember { mutableStateOf("qcow2") }
    var compress by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf("") }
    val formats = listOf("qcow2", "raw", "vmdk", "vdi", "qcow", "qed")
    var showTargetFormatSelector by remember { mutableStateOf(false) }

    Column {
        ALSList(data = stringResource(R.string.disk_path), value = path, first = true, background = if (path.isEmpty()) Color.Red.copy(alpha = 0.1f) else null, onValueChange = { path = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.target_path), value = targetPath, onValueChange = { targetPath = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.target_format), value = targetFormat, onClick = { showTargetFormatSelector = true })
        ALSList(data = stringResource(R.string.compress), value = if (compress) stringResource(R.string.on) else stringResource(R.string.off), onClick = { compress = !compress })
        ALSList(data = stringResource(R.string.options), value = options, onValueChange = { options = it }, last = true)

        Spacer(Modifier.height(9.dp))

        ALSList(data = stringResource(R.string.preview), value = null, onClick = {
            if (path.isNotEmpty() && targetPath.isNotEmpty()) {
                onPreview(buildString {
                    append("convert ")
                    if (compress) append("-c ")
                    if (options.isNotEmpty()) append("-o $options ")
                    append("-O $targetFormat \"$path\" \"$targetPath\"")
                })
            }
        })
        ALSList(data = stringResource(R.string.execute_action), value = null, onClick = {
            if (path.isNotEmpty() && targetPath.isNotEmpty()) {
                onExecute(buildString {
                    append("convert ")
                    if (compress) append("-c ")
                    if (options.isNotEmpty()) append("-o $options ")
                    append("-O $targetFormat \"$path\" \"$targetPath\"")
                })
            }
        })
    }

    if (showTargetFormatSelector) {
        ALSList(data = formats, show = true, onDismiss = { showTargetFormatSelector = false }, onClick = { targetFormat = it; showTargetFormatSelector = false })
    }
}

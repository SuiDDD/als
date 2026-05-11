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
fun QvmImgCreate(onPreview: (String) -> Unit, onExecute: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("20G") }
    var format by remember { mutableStateOf("qcow2") }
    var backingFile by remember { mutableStateOf("") }
    var options by remember { mutableStateOf("") }
    val formats = listOf("qcow2", "raw", "vmdk", "vdi", "qcow", "qed")
    var showFormatSelector by remember { mutableStateOf(false) }

    Column {
        ALSList(data = stringResource(R.string.disk_path), value = path, first = true, background = if (path.isEmpty()) Color.Red.copy(alpha = 0.1f) else null, onValueChange = { path = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.image_size), value = size, onValueChange = { size = it })
        ALSList(data = stringResource(R.string.image_format), value = format, onClick = { showFormatSelector = true })
        ALSList(data = stringResource(R.string.backing_file), value = backingFile, onValueChange = { backingFile = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.options), value = options, onValueChange = { options = it }, last = true)

        Spacer(Modifier.height(9.dp))

        ALSList(data = stringResource(R.string.preview), value = null, onClick = {
            onPreview(buildCommand(path, size, format, backingFile, options))
        })
        ALSList(data = stringResource(R.string.execute_action), value = null, onClick = {
            onExecute(buildCommand(path, size, format, backingFile, options))
        })
    }

    if (showFormatSelector) {
        ALSList(data = formats, show = true, onDismiss = { showFormatSelector = false }, onClick = { format = it; showFormatSelector = false })
    }
}

fun buildCommand(path: String, size: String, format: String, backingFile: String, options: String): String {
    return buildString {
        append("create -f $format ")
        if (backingFile.isNotEmpty()) append("-b \"$backingFile\" ")
        if (options.isNotEmpty()) append("-o $options ")
        append("\"$path\" $size")
    }
}

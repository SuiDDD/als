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
fun QvmImgRebase(onPreview: (String) -> Unit, onExecute: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    var backingFile by remember { mutableStateOf("") }
    var backingFormat by remember { mutableStateOf("qcow2") }
    val formats = listOf("qcow2", "raw", "vmdk", "vdi", "qcow", "qed")
    var showBackingFormatSelector by remember { mutableStateOf(false) }

    Column {
        ALSList(data = stringResource(R.string.disk_path), value = path, first = true, background = if (path.isEmpty()) Color.Red.copy(alpha = 0.1f) else null, onValueChange = { path = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.backing_file), value = backingFile, onValueChange = { backingFile = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.image_format), value = backingFormat, onClick = { showBackingFormatSelector = true }, last = true)

        Spacer(Modifier.height(9.dp))

        ALSList(data = stringResource(R.string.preview), value = null, onClick = {
            if (path.isNotEmpty()) {
                onPreview("rebase ${if (backingFile.isEmpty()) "-p" else "-b \"$backingFile\" -F $backingFormat"} \"$path\"")
            }
        })
        ALSList(data = stringResource(R.string.execute_action), value = null, onClick = {
            if (path.isNotEmpty()) {
                onExecute("rebase ${if (backingFile.isEmpty()) "-p" else "-b \"$backingFile\" -F $backingFormat"} \"$path\"")
            }
        })
    }

    if (showBackingFormatSelector) {
        ALSList(data = formats, show = true, onDismiss = { showBackingFormatSelector = false }, onClick = { backingFormat = it; showBackingFormatSelector = false })
    }
}

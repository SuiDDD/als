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
fun QvmImgCompare(onPreview: (String) -> Unit, onExecute: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    var targetPath by remember { mutableStateOf("") }

    Column {
        ALSList(data = stringResource(R.string.disk_path), value = path, first = true, background = if (path.isEmpty()) Color.Red.copy(alpha = 0.1f) else null, onValueChange = { path = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.target_path), value = targetPath, onValueChange = { targetPath = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })

        Spacer(Modifier.height(9.dp))

        ALSList(data = stringResource(R.string.preview), value = null, onClick = {
            if (path.isNotEmpty() && targetPath.isNotEmpty()) onPreview("compare \"$path\" \"$targetPath\"")
        })
        ALSList(data = stringResource(R.string.execute_action), value = null, onClick = {
            if (path.isNotEmpty() && targetPath.isNotEmpty()) onExecute("compare \"$path\" \"$targetPath\"")
        })
    }
}

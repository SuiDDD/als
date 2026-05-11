package sui.k.als.vm.qvm

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sui.k.als.R
import sui.k.als.su
import sui.k.als.ui.ALSButton
import sui.k.als.ui.ALSList

@Composable
fun QvmImg() {
    var path by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("20G") }
    var format by remember { mutableStateOf("qcow2") }
    val formats = listOf("qcow2", "raw")
    var showFormatSelector by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        ALSList(
            data = stringResource(R.string.disk_path),
            value = path,
            first = true,
            onValueChange = { path = it }
        )
        ALSList(
            data = stringResource(R.string.image_size),
            value = size,
            onValueChange = { size = it }
        )
        ALSList(
            data = stringResource(R.string.image_format),
            value = format,
            last = true,
            onClick = { showFormatSelector = true }
        )

        Spacer(Modifier.height(18.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ALSButton(R.drawable.add) {
                if (path.isNotEmpty() && size.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val cmd = "LD_LIBRARY_PATH=$qvmDir/libs $qvmDir/qemu-img create -f $format \"$path\" $size"
                            Runtime.getRuntime().exec(arrayOf(su, "-c", cmd)).waitFor()
                        } catch (_: Exception) { }
                    }
                }
            }
        }
    }

    if (showFormatSelector) {
        ALSList(
            data = formats,
            show = true,
            onDismiss = { showFormatSelector = false },
            onClick = {
                format = it
                showFormatSelector = false
            }
        )
    }
}
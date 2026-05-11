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
fun QvmImgSnapshot(onPreview: (String) -> Unit, onExecute: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    var snapshotAction by remember { mutableStateOf("list") }
    var snapshotName by remember { mutableStateOf("") }
    val snapshotActions = listOf(
        "list" to R.string.preview,
        "create" to R.string.create,
        "apply" to R.string.apply,
        "delete" to R.string.delete
    )
    var showSnapshotActionSelector by remember { mutableStateOf(false) }

    Column {
        val actionName = stringResource(snapshotActions.find { it.first == snapshotAction }?.second ?: R.string.preview)
        ALSList(data = stringResource(R.string.disk_path), value = path, first = true, background = if (path.isEmpty()) Color.Red.copy(alpha = 0.1f) else null, onValueChange = { path = it }, iconContent = {
            ALSButton("...", size = 18.dp, iconSize = 12.dp) {
            }
        })
        ALSList(data = stringResource(R.string.snapshot), value = actionName, onClick = { showSnapshotActionSelector = true })
        if (snapshotAction != "list") {
            ALSList(data = stringResource(R.string.snapshot_name), value = snapshotName, onValueChange = { snapshotName = it }, last = true)
        } else {
            ALSList(data = stringResource(R.string.snapshot_name), value = "", last = true, checked = false)
        }

        Spacer(Modifier.height(9.dp))

        ALSList(data = stringResource(R.string.preview), value = null, onClick = {
            if (path.isNotEmpty()) {
                onPreview(buildSnapshotCommand(path, snapshotAction, snapshotName))
            }
        })
        ALSList(data = stringResource(R.string.execute_action), value = null, onClick = {
            if (path.isNotEmpty()) {
                onExecute(buildSnapshotCommand(path, snapshotAction, snapshotName))
            }
        })
    }

    if (showSnapshotActionSelector) {
        val labels = mutableListOf<String>()
        snapshotActions.forEach { labels.add(stringResource(it.second)) }
        ALSList(
            data = labels,
            show = true,
            onDismiss = { showSnapshotActionSelector = false },
            onClick = { label ->
                val index = labels.indexOf(label)
                if (index != -1) {
                    snapshotAction = snapshotActions[index].first
                }
                showSnapshotActionSelector = false
            }
        )
    }
}

fun buildSnapshotCommand(path: String, action: String, name: String): String {
    return when(action) {
        "list" -> "snapshot -l \"$path\""
        "create" -> "snapshot -c \"$name\" \"$path\""
        "apply" -> "snapshot -a \"$name\" \"$path\""
        "delete" -> "snapshot -d \"$name\" \"$path\""
        else -> ""
    }
}

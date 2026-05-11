package sui.k.als.vm.qvm

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import sui.k.als.R
import sui.k.als.ui.*

@Composable
fun QvmImgPreview(cmdOptions: String, onDismiss: () -> Unit, onExecute: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF222222))
                .padding(9.dp)
        ) {
            Column {
                ALSList(
                    data = stringResource(R.string.command_preview),
                    value = "qemu-img $cmdOptions",
                    first = true,
                    last = true
                )
                Spacer(Modifier.height(9.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.End)
                ) {
                    ALSButton(R.drawable.arrow_forward, size = 24.dp, iconSize = 12.dp, click = onDismiss)
                    ALSButton(R.drawable.arrow_forward, size = 24.dp, iconSize = 12.dp, click = { onExecute(cmdOptions) })
                }
            }
        }
    }
}

@Composable
fun QvmImgOutput(output: String, executing: Boolean, onClear: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(9.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (executing) {
                    ALSList(data = "Executing...", value = null, first = true)
                } else {
                    ALSList(data = stringResource(R.string.execute_action), value = "Done", first = true)
                }
                ALSButton("X", size = 24.dp, iconSize = 12.dp, click = onClear)
            }
            if (output.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                SelectionContainer {
                    androidx.compose.material3.Text(
                        text = output,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = sui.k.als.localFont.current,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

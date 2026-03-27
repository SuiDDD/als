package sui.k.als.boot

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File

object BootSU {
    private var _path by mutableStateOf(BootConfig.suPath)
    val path: String get() = _path
    private fun getFile(ctx: Context): File = File(ctx.filesDir, "cfg")
    fun init(ctx: Context) {
        val file = getFile(ctx)
        if (file.exists()) {
            val content = file.readText().trim()
            if (content.isNotEmpty()) _path = content
        }
    }

    fun update(ctx: Context, newPath: String) {
        _path = newPath
        getFile(ctx).writeText(newPath)
    }
}

@Composable
fun SuPathDialog(ctx: Context, onDismiss: () -> Unit) {
    var tmp by remember { mutableStateOf(BootSU.path) }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .background(BootConfig.dialogBg, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(9.dp)
        ) {
            BasicTextField(
                value = tmp,
                onValueChange = { tmp = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    BootSU.update(ctx, tmp)
                    onDismiss()
                })
            )
            if (tmp.isEmpty()) {
                androidx.compose.foundation.text.BasicText(
                    "su路径", style = TextStyle(color = Color.DarkGray, fontSize = 14.sp)
                )
            }
        }
    }
}
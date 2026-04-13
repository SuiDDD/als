package sui.k.als.boot

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sui.k.als.localFont

var suPath by mutableStateOf("su")
val su get() = suPath

@Composable
fun Splash(
    modifier: Modifier = Modifier,
    onTimeout: (() -> Unit)? = null,
    header: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("su", 0) }
    var tempPath by remember { mutableStateOf(suPath) }
    var checkTrigger by remember { mutableIntStateOf(0) }
    var needsEdit by remember { mutableStateOf(false) }
    val appIcon = remember {
        ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap().asImageBitmap()
    }
    LaunchedEffect(Unit) {
        prefs.getString("su", "su")?.let { suPath = it; tempPath = it }
    }
    onTimeout?.let { timeout ->
        LaunchedEffect(checkTrigger, suPath) {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ProcessBuilder(suPath, "-c", "exit").start().waitFor() == 0
                }.getOrDefault(false)
            }
            if (ok) timeout() else needsEdit = true
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(0.09f)
                    .fillMaxWidth(), Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(appIcon, null, Modifier.size(27.dp))
                    header()
                }
            }
            Column(
                Modifier
                    .weight(0.81f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onTimeout == null) content()
            }
            Spacer(
                Modifier
                    .weight(0.1f)
                    .fillMaxWidth()
            )
        }
        if (needsEdit) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                BasicTextField(
                    value = tempPath,
                    onValueChange = { tempPath = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White, fontSize = 9.sp, fontFamily = localFont.current
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        prefs.edit { putString("su", tempPath) }
                        suPath = tempPath
                        checkTrigger++
                        needsEdit = false
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}
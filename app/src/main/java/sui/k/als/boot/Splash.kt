package sui.k.als.boot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sui.k.als.localAppFont

var suPath by mutableStateOf("su")
val su: String get() = suPath

@Composable
fun Splash(
    modifier: Modifier = Modifier,
    onTimeout: (() -> Unit)? = null,
    header: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val font: FontFamily = localAppFont.current
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("als_cfg", 0) }
    var tempPath by remember { mutableStateOf(su) }
    var checkTrigger by remember { mutableIntStateOf(0) }
    var needsEdit by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        prefs.getString("su_p", "su")?.let { suPath = it; tempPath = it }
    }
    if (onTimeout != null) {
        LaunchedEffect(checkTrigger, suPath) {
            val isOk = withContext(Dispatchers.IO) {
                runCatching {
                    ProcessBuilder(su, "-c", "exit 0").start().waitFor() == 0
                }.getOrDefault(false)
            }
            if (isOk) onTimeout() else needsEdit = true
        }
    }
    if (needsEdit) {
        AlertDialog(onDismissRequest = {}, text = {
            OutlinedTextField(
                value = tempPath,
                onValueChange = { tempPath = it },
                label = { Text("SU路径", color = Color.Red) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    prefs.edit { putString("su_p", tempPath) }
                    suPath = tempPath
                    checkTrigger++
                })
            )
        }, confirmButton = {}, dismissButton = {})
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            Modifier
                .weight(0.236f)
                .fillMaxWidth(), Alignment.BottomCenter
        ) {
            header()
            Text("AndLinSys", color = Color.Black, fontSize = 9.sp, fontFamily = font)
        }
        Column(
            Modifier
                .weight(0.618f)
                .fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally
        ) {
            if (onTimeout == null) content()
        }
        Column(
            Modifier
                .weight(0.146f)
                .fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally
        ) {
            Text("Powered by", color = Color.Gray, fontSize = 6.sp, fontFamily = font)
            Text("Gunyah", color = Color.Black, fontSize = 9.sp, fontFamily = font)
        }
    }
}
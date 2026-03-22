package sui.k.als.boot

import android.content.ClipData
import android.graphics.BitmapFactory
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sui.k.als.localAppFont
import java.util.concurrent.TimeUnit

var suPath by mutableStateOf("su")
val su: String get() = suPath

@Composable
fun Splash(
    modifier: Modifier = Modifier,
    onTimeout: (() -> Unit)? = null,
    header: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val (font, ctx) = localAppFont.current to LocalContext.current
    val (clip, scope) = LocalClipboard.current to rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var tempPath by remember { mutableStateOf(su) }
    var refresh by remember { mutableIntStateOf(0) }
    val prefs = remember { ctx.getSharedPreferences("als_cfg", 0) }
    LaunchedEffect(Unit) { suPath = prefs.getString("su_p", "su") ?: "su"; tempPath = suPath }
    if (showEdit) {
        AlertDialog(onDismissRequest = { showEdit = false }, text = {
            OutlinedTextField(
                value = tempPath,
                onValueChange = { tempPath = it },
                label = { Text("SU路径") },
                placeholder = { Text("SU路径") },
                singleLine = true
            )
        }, confirmButton = {
            Button(onClick = {
                suPath = tempPath; prefs.edit {
                putString(
                    "su_p", tempPath
                )
            }; showEdit = false; refresh++
            }) { Text("保存") }
        }, dismissButton = {
            Button(onClick = { showEdit = false }) { Text("取消") }
        })
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            Modifier
                .weight(0.236f)
                .fillMaxWidth(), Alignment.BottomCenter
        ) { header(); Text("AndLinSys", color = Color.White, fontSize = 9.sp, fontFamily = font) }
        Column(
            Modifier
                .weight(0.618f)
                .fillMaxWidth()
        ) {
            if (onTimeout != null) {
                val lines = remember { mutableStateListOf<AnnotatedString>() }
                val state = rememberLazyListState()
                var auto by remember { mutableStateOf(true) }
                LaunchedEffect(refresh, suPath) {
                    lines.clear()
                    withContext(Dispatchers.IO) {
                        val r = runCatching {
                            ProcessBuilder(
                                su, "-c", "logcat --uid ${Process.myUid()} -v tag"
                            ).start()
                        }
                        val p = r.getOrNull() ?: return@withContext run {
                            withContext(Dispatchers.Main) {
                                lines.add(
                                    AnnotatedString(
                                        r.exceptionOrNull()?.message ?: "SU Failed",
                                        SpanStyle(Color.Red, 6.sp)
                                    )
                                )
                            }
                        }
                        val err = async { p.errorStream.bufferedReader().readText() }
                        launch {
                            p.inputStream.bufferedReader().useLines {
                                it.forEach { l ->
                                    val s = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = when (l.getOrNull(0)) {
                                                    'V' -> Color(0xFFD6D6D6); 'D' -> Color(
                                                        0xFFCFE7FF
                                                    ); 'I' -> Color(0xFFE9F5E6); 'W' -> Color(
                                                        0xFFF5EAC1
                                                    ); 'E' -> Color(0xFFCF5B56); 'A' -> Color(
                                                        0xFF7F0000
                                                    ); else -> Color.White
                                                }, fontSize = 6.sp
                                            )
                                        ) { append(l) }
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (lines.size > 2000) lines.removeAt(
                                            0
                                        ); lines.add(s); if (auto) state.scrollToItem(lines.size - 1)
                                    }
                                }
                            }
                        }
                        if (p.waitFor(300, TimeUnit.MILLISECONDS) && p.exitValue() != 0) {
                            val m = err.await()
                            withContext(Dispatchers.Main) {
                                lines.add(
                                    AnnotatedString(
                                        m.ifEmpty { "Error: ${p.exitValue()}" },
                                        SpanStyle(Color.Red, 6.sp)
                                    )
                                )
                            }
                        } else {
                            delay(1800); withContext(Dispatchers.Main) { onTimeout() }
                        }
                    }
                }
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { auto = false }, onDoubleTap = {
                                scope.launch {
                                    clip.setClipEntry(
                                        ClipEntry(
                                            ClipData.newPlainText(
                                                "log", lines.joinToString("\n")
                                            )
                                        )
                                    )
                                }
                            })
                        }, state
                ) {
                    items(lines) { l ->
                        val isError = l.spanStyles.any {
                            it.item.color == Color.Red || it.item.color == Color(0xFFCF5B56)
                        }
                        Text(
                            l,
                            fontFamily = font,
                            modifier = if (isError) Modifier
                                .clickable { showEdit = true }
                                .padding(vertical = 1.dp) else Modifier)
                    }
                }
            } else content()
        }
        Column(
            Modifier
                .weight(0.146f)
                .fillMaxWidth(), Arrangement.Center, Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val b =
                    remember { ctx.assets.open("go.png").use { BitmapFactory.decodeStream(it) } }
                Image(b.asImageBitmap(), null, Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text("SuiDDD", color = Color.White, fontSize = 9.sp, fontFamily = font)
            }
            Text(
                "Device Debugging Deployment",
                color = Color.Gray,
                fontSize = 6.sp,
                fontFamily = font
            )
        }
    }
}
package sui.k.als.boot

import android.content.ClipData
import android.graphics.BitmapFactory
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sui.k.als.localAppFont
import java.util.concurrent.TimeUnit

@Composable
fun Splash(
    modifier: Modifier = Modifier,
    onTimeout: (() -> Unit)? = null,
    header: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val font = localAppFont.current
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            Modifier
                .weight(0.236f)
                .fillMaxWidth(), Alignment.BottomCenter
        ) {
            header()
            Text("AndLinSys", color = Color.White, fontSize = 9.sp, fontFamily = font)
        }
        Column(
            Modifier
                .weight(0.618f)
                .fillMaxWidth()
        ) {
            if (onTimeout != null) {
                val logLines = remember { mutableStateListOf<AnnotatedString>() }
                val listState = rememberLazyListState()
                var autoScroll by remember { mutableStateOf(true) }
                val clipboard = LocalClipboard.current
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val result = runCatching {
                            ProcessBuilder(
                                "su",
                                "-c",
                                "logcat --uid ${Process.myUid()} -v tag"
                            ).start()
                        }
                        val process = result.getOrNull()
                        if (process == null) {
                            withContext(Dispatchers.Main) {
                                logLines.add(
                                    AnnotatedString(
                                        result.exceptionOrNull()?.message ?: "Unknown Error",
                                        SpanStyle(Color.Red, 6.sp)
                                    )
                                )
                            }
                            return@withContext
                        }
                        val errorStreamTask =
                            async { process.errorStream.bufferedReader().use { it.readText() } }
                        launch {
                            process.inputStream.bufferedReader().useLines { lines ->
                                lines.forEach { line ->
                                    val styledString = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = when (line.getOrNull(0)) {
                                                    'V' -> Color(0xFFD6D6D6); 'D' -> Color(
                                                        0xFFCFE7FF
                                                    ); 'I' -> Color(0xFFE9F5E6)
                                                    'W' -> Color(0xFFF5EAC1); 'E' -> Color(
                                                        0xFFCF5B56
                                                    ); 'A' -> Color(0xFF7F0000)
                                                    else -> Color.White
                                                }, fontSize = 6.sp
                                            )
                                        ) { append(line) }
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (logLines.size > 2999) logLines.removeAt(0)
                                        logLines.add(styledString)
                                        if (autoScroll) scope.launch {
                                            listState.scrollToItem(
                                                logLines.size - 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        process.waitFor(300, TimeUnit.MILLISECONDS)
                        if (!process.isAlive && process.exitValue() != 0) {
                            val errorMessage = errorStreamTask.await()
                            withContext(Dispatchers.Main) {
                                logLines.add(
                                    AnnotatedString(
                                        errorMessage.ifEmpty { "Exit code: ${process.exitValue()}" },
                                        SpanStyle(Color.Red, 6.sp)
                                    )
                                )
                            }
                        } else {
                            delay(1800)
                            withContext(Dispatchers.Main) { onTimeout() }
                        }
                    }
                }
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { autoScroll = false },
                                onDoubleTap = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(
                                                ClipData.newPlainText(
                                                    "log",
                                                    logLines.joinToString("\n")
                                                )
                                            )
                                        )
                                    }
                                })
                        }, listState
                ) { items(logLines) { Text(it, fontFamily = font) } }
            } else content()
        }
        Column(
            Modifier
                .weight(0.146f)
                .fillMaxWidth(),
            Arrangement.Center,
            Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bitmap = remember {
                    context.assets.open("go.png").use { BitmapFactory.decodeStream(it) }
                }
                Image(bitmap.asImageBitmap(), null, Modifier.size(18.dp))
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
package sui.k.als.ide

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import io.github.rosemoe.sora.widget.*
import io.github.rosemoe.sora.widget.schemes.*
import sui.k.als.R
import sui.k.als.ui.*
import java.io.*

var idePath by mutableStateOf<String?>(null)

fun ideOpen(path: String?) {
    idePath = path
}

@Composable
fun IDE() {
    val path = idePath ?: return
    val file = remember(path) { File(path) }
    val text = remember(path) { if (file.exists()) file.readText() else "" }
    val editor = remember { mutableStateOf<CodeEditor?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                CodeEditor(ctx).apply {
                    colorScheme = EditorColorScheme()
                    setText(text)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    editor.value = this
                }
            }, modifier = Modifier.fillMaxSize()
        )
        ALSButton(
            icon = R.drawable.save,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            click = {
                editor.value?.let { file.writeText(it.text.toString()) }
                ideOpen(null)
            })
    }
}

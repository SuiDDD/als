package sui.k.als

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cmd
import sui.k.als.tty.createTTYInstance
import sui.k.als.tty.ttySession
import sui.k.als.ui.ALSButton
import sui.k.als.ui.ALSList
import androidx.core.content.edit

var suPath by mutableStateOf("su")
val su get() = suPath

@Composable
fun Splash(
    modifier: Modifier = Modifier, instance: TTYInstance? = null, onTimeout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var internalInstance by remember { mutableStateOf<TTYInstance?>(null) }
    val active = instance ?: internalInstance
    var showIcon by remember { mutableStateOf(false) }
    var showSuInput by remember { mutableStateOf(false) }
    var checkCount by remember { mutableIntStateOf(0) }
    var inputPath by remember { mutableStateOf("") }

    val appIcon = remember {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }

    LaunchedEffect(checkCount) {
        val sp = context.getSharedPreferences("su", 0)
        suPath = sp.getString("su", "su") ?: "su"
        inputPath = suPath

        val suWorked = try {
            Runtime.getRuntime().exec(arrayOf(suPath, "-v")).waitFor() == 0
        } catch (_: Exception) {
            false
        }

        if (!suWorked) {
            showSuInput = true
            return@LaunchedEffect
        }

        showSuInput = false
        if (active == null) internalInstance =
            createTTYInstance(context, TTYSessionStub(), TTYViewStub())
        ttySession = (instance ?: internalInstance)?.session
        launch {
            delay(90)
            cmd(su)
            cmd("clear")
        }
        showIcon = true
        delay(900)
        onTimeout?.invoke()
    }

    Box(modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
        active?.let { tty ->
            AndroidView(factory = {
                val parent = tty.view.parent as? android.view.ViewGroup
                parent?.removeView(tty.view)
                tty.view
            }, modifier = Modifier.fillMaxSize(), update = { it.onScreenUpdated() })
        }

        if (showSuInput) {
            Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxWidth(1f / 3f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ALSList(
                        data = "",
                        value = inputPath,
                        first = true,
                        last = true,
                        onValueChange = { inputPath = it }
                    )
                    Spacer(Modifier.height(9.dp))
                    ALSButton(R.drawable.arrow_forward) {
                        suPath = inputPath
                        context.getSharedPreferences("su", 0).edit { putString("su", suPath) }
                        checkCount++
                    }
                }
            }
        } else if (showIcon) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                ALSButton(appIcon, iconSize = 27.dp)
            }
        }
    }
}

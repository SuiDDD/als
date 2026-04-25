package sui.k.als

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val appIcon = remember {
        context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
    }
    LaunchedEffect(Unit) {
        suPath = context.getSharedPreferences("su", 0).getString("su", "su") ?: "su"
        if (active == null) internalInstance =
            createTTYInstance(context, TTYSessionStub(), TTYViewStub())
        ttySession = (instance ?: internalInstance)?.session
        launch {
            delay(90)
            cmd(su)
            cmd("clear")
        }
        showIcon = true
        delay(1800)
        onTimeout?.invoke()
    }
    Box(modifier.fillMaxSize(), Alignment.Center) {
        active?.let { tty ->
            AndroidView(factory = {
                (tty.view.parent as? ViewGroup)?.removeView(tty.view)
                tty.view
            }, modifier = Modifier.fillMaxSize(), update = { it.onScreenUpdated() })
        }
        if (showIcon) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                ALSButton(appIcon, iconSize = 27.dp)
            }
        }
    }
}
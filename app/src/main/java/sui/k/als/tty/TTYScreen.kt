package sui.k.als.tty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TTYScreen(instance: TTYIns) {
    if (instance.isFinished.value) return
    val den = LocalDensity.current
    val view = instance.view
    val session = instance.session
    var imePx by remember { mutableIntStateOf(0) }
    val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(instance) {
        ttySession = session
        view.requestFocus()
        view.post { view.onScreenUpdated(); view.invalidate() }
    }
    CompositionLocalProvider(LocalSession provides session) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (IMEState.isFloating) 0.dp else imeHeight + with(den) { imePx.toDp() }),
                update = { it.onScreenUpdated() })
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = imeHeight)
                    .onGloballyPositioned { imePx = it.size.height }) { TTYIME() }
        }
    }
}
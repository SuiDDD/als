package sui.k.als.vnc
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
fun Modifier.vncTouchHandler(state: VNCRenderState, viewSize: IntSize): Modifier = this.then(
    Modifier.pointerInput(viewSize) {
        detectTapGestures { pos ->
            state.bmp?.let { b ->
                val sc = (viewSize.width.toFloat() / b.width).coerceAtMost(viewSize.height.toFloat() / b.height)
                val ix = ((pos.x - (viewSize.width - b.width * sc) / 2f) / sc).toInt()
                val iy = ((pos.y - (viewSize.height - b.height * sc) / 2f) / sc).toInt()
                if (ix in 0 until b.width && iy in 0 until b.height) {
                    state.send("M $ix $iy 1\n")
                    state.send("M $ix $iy 0\n")
                }
            }
        }
    }
)
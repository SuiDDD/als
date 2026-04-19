package sui.k.als.vnc
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
@Composable
fun VNCDisplay(state: VNCRenderState) {
    val b = state.bmp ?: return
    val t = state.tick
    Canvas(Modifier.fillMaxSize()) {
        if (t >= 0) {
            val cw = b.width.toFloat()
            val ch = b.height.toFloat()
            val sw = size.width
            val sh = size.height
            val sc = (sw / cw).coerceAtMost(sh / ch)
            val dw = (cw * sc).toInt().coerceAtLeast(1)
            val dh = (ch * sc).toInt().coerceAtLeast(1)
            val ox = ((sw - dw) / 2).toInt()
            val oy = ((sh - dh) / 2).toInt()
            drawImage(
                image = b.asImageBitmap(),
                dstOffset = IntOffset(ox, oy),
                dstSize = IntSize(dw, dh)
            )
        }
    }
}
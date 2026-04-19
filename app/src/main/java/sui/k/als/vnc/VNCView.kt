package sui.k.als.vnc
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.core.graphics.createBitmap
@Composable
fun VNCScreen(vncBinaryPath: String = "/data/local/tmp/als/app/qvm/vnc", onExit: () -> Unit) {
    val state = remember { VNCRenderState() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val transformState = rememberTransformableState { zoom, adj, _ ->
        val nextScale = (scale * zoom).coerceIn(1f, 18f)
        val scaleChange = nextScale / scale
        scale = nextScale
        offset = (offset + adj * scale) * scaleChange
        val maxOffX = viewSize.width.toFloat() * (scale - 1f) / 2f
        val maxOffY = viewSize.height.toFloat() * (scale - 1f) / 2f
        offset = Offset(offset.x.coerceIn(-maxOffX, maxOffX), offset.y.coerceIn(-maxOffY, maxOffY))
    }
    BackHandler { onExit() }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val proc = ProcessBuilder("su", "-c", vncBinaryPath).start()
            state.out = proc.outputStream
            val input = proc.inputStream
            val hBuf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            try {
                while (isActive) {
                    if (input.read() != 0xAA || input.read() != 0x55 || input.read() != 0xAA || input.read() != 0x55) continue
                    hBuf.clear()
                    val headRead = input.read(hBuf.array())
                    if (headRead == -1) break
                    val w = hBuf.int
                    val h = hBuf.int
                    val size = w * h * 4
                    if (w <= 0 || h <= 0 || size > 50 * 1024 * 1024) continue
                    if (state.pArr?.size != size) state.pArr = ByteArray(size)
                    var pOff = 0
                    while (pOff < size && isActive) {
                        val r = input.read(state.pArr, pOff, size - pOff)
                        if (r == -1) break
                        pOff += r
                    }
                    val currentBmp = state.bmp
                    val newB = if (currentBmp == null || currentBmp.width != w || currentBmp.height != h) {
                        currentBmp?.recycle()
                        createBitmap(w, h)
                    } else currentBmp
                    newB.copyPixelsFromBuffer(ByteBuffer.wrap(state.pArr!!))
                    state.bmp = newB
                    state.tick++
                }
            } catch (_: Exception) {
            } finally {
                proc.destroy()
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewSize = it }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
            .transformable(transformState)
            .vncTouchHandler(state, viewSize)
    ) {
        VNCDisplay(state)
    }
}
class VNCRenderState {
    var bmp by mutableStateOf<Bitmap?>(null)
    var tick by mutableLongStateOf(0L)
    var out: java.io.OutputStream? = null
    var pArr: ByteArray? = null
    fun send(s: String) {
        val o = out ?: return
        Thread { try { o.write(s.toByteArray()); o.flush() } catch (_: Exception) {} }.start()
    }
}
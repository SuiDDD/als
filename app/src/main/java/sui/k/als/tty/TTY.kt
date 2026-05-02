package sui.k.als.tty

import android.content.*
import android.content.ClipboardManager
import android.graphics.*
import android.view.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import com.termux.terminal.*
import com.termux.view.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

data class TTYInstance(
    val session: TerminalSession, val view: TerminalView
)

val LocalSession = staticCompositionLocalOf<TerminalSession?> { null }
internal var ttySession: TerminalSession? = null
private val ttyIO = Executors.newSingleThreadExecutor()
fun cmd(cmd: String) {
    ttyIO.execute { ttySession?.write("$cmd\n") }
}

@Composable
fun TTYScreen(instance: TTYInstance, content: @Composable () -> Unit = {}) {
    val density = LocalDensity.current
    val termView = instance.view
    val termSession = instance.session
    var imeHeightPx by remember { mutableIntStateOf(0) }
    val imePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(instance) {
        ttySession = termSession
        termView.requestFocus()
        termView.post { termView.onScreenUpdated(); termView.invalidate() }
    }
    CompositionLocalProvider(LocalSession provides termSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { termView }, modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (imeHeightPx == 0 || IMEState.isFloating) 0.dp else imePadding + with(
                            density
                        ) { imeHeightPx.toDp() }), update = { view -> view.onScreenUpdated() })
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = imePadding)
                    .onGloballyPositioned { layout ->
                        imeHeightPx = layout.size.height
                    }) { content() }
        }
    }
}

fun createTTYInstance(
    context: Context, sessionClient: TTYSessionStub, viewClient: TTYViewStub
): TTYInstance {
    val session = TerminalSession(TTYENV, 9216, sessionClient)
    val view = TerminalView(context, null).apply {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        setTextSize(12)
        setTypeface(
            try {
                Typeface.createFromAsset(context.assets, "font/GoogleSansCode.ttf")
            } catch (_: Exception) {
                Typeface.MONOSPACE
            }
        )
        setBackgroundColor(android.graphics.Color.BLACK)
        setTerminalViewClient(viewClient)
        attachSession(session)
    }
    sessionClient.bindView(view)
    viewClient.bindView(view)
    return TTYInstance(session, view)
}

open class TTYSessionStub : TerminalSessionClient {
    private var boundView: TerminalView? = null
    private val updatePosted = AtomicBoolean(false)
    fun bindView(view: TerminalView) {
        boundView = view
    }

    override fun onTextChanged(session: TerminalSession) {
        if (updatePosted.compareAndSet(false, true)) {
            boundView?.post {
                updatePosted.set(false)
                boundView?.onScreenUpdated()
                boundView?.invalidate()
            }
        }
    }

    override fun onTitleChanged(session: TerminalSession) {}
    override fun onSessionFinished(session: TerminalSession) {}
    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val context = boundView?.context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("T", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val context = boundView?.context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip?.getItemAt(0)?.let { item ->
            val text = item.coerceToText(context).toString()
            ttyIO.execute { session?.write(text) }
        }
    }

    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(visible: Boolean) {
        if (updatePosted.compareAndSet(false, true)) {
            boundView?.post {
                updatePosted.set(false)
                boundView?.onScreenUpdated()
            }
        }
    }

    override fun getTerminalCursorStyle() = 2
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
    override fun logError(tag: String?, msg: String?) {}
    override fun logWarn(tag: String?, msg: String?) {}
    override fun logInfo(tag: String?, msg: String?) {}
    override fun logDebug(tag: String?, msg: String?) {}
    override fun logVerbose(tag: String?, msg: String?) {}
    override fun logStackTraceWithMessage(tag: String?, msg: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}

open class TTYViewStub : TerminalViewClient {
    private var boundView: TerminalView? = null
    private var currentSize = 12f
    fun bindView(view: TerminalView) {
        boundView = view
    }

    override fun readControlKey() = IMEState.consumeCtrl()
    override fun readAltKey() = IMEState.consumeAlt()
    override fun readShiftKey() = IMEState.consumeShift()
    override fun readFnKey() = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent, session: TerminalSession) =
        (keyCode == KeyEvent.KEYCODE_BACK)

    override fun onKeyUp(keyCode: Int, event: KeyEvent) = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession) = false
    override fun onSingleTapUp(event: MotionEvent) {}
    override fun onLongPress(event: MotionEvent) = false
    override fun onScale(scaleFactor: Float): Float {
        val newSize = (currentSize * scaleFactor).coerceIn(12f, 270f)
        if (newSize != currentSize) {
            currentSize = newSize
            boundView?.post { boundView?.setTextSize(currentSize.toInt()) }
        }
        return 1f
    }

    override fun shouldEnforceCharBasedInput() = true
    override fun shouldBackButtonBeMappedToEscape() = true
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(enabled: Boolean) {}
    override fun onEmulatorSet() {}
    override fun logError(tag: String?, msg: String?) {}
    override fun logWarn(tag: String?, msg: String?) {}
    override fun logInfo(tag: String?, msg: String?) {}
    override fun logDebug(tag: String?, msg: String?) {}
    override fun logVerbose(tag: String?, msg: String?) {}
    override fun logStackTraceWithMessage(tag: String?, msg: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}
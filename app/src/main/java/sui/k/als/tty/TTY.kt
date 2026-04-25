package sui.k.als.tty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import sui.k.als.alsPath
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class TTYInstance(
    val session: TerminalSession,
    val view: TerminalView,
    val initialized: AtomicBoolean = AtomicBoolean(false)
)

val LocalSession = staticCompositionLocalOf<TerminalSession?> { null }
internal var ttySession: TerminalSession? = null
private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "als-io").apply {
        priority = Thread.MAX_PRIORITY
    }
}

fun cmd(cmd: String) {
    ioExecutor.execute { ttySession?.write("$cmd\n") }
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
                        ) { imeHeightPx.toDp() }), update = { it.onScreenUpdated() })
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = imePadding)
                    .onGloballyPositioned { imeHeightPx = it.size.height }) { content() }
        }
    }
}

fun createTTYInstance(
    context: Context, sessionClient: TTYSessionStub, viewClient: TTYViewStub
): TTYInstance {
    val workDir = context.filesDir.absolutePath.also { File(it).mkdirs() }
    val session = TerminalSession(
        "/system/bin/sh", workDir, arrayOf("-i"), arrayOf(
            "TERM=xterm-256color",
            "HOME=$workDir",
            "LANG=en_US.UTF-8",
            "PATH=/system/bin:/system/xbin:$alsPath"
        ), 300000, sessionClient
    )
    val view = TerminalView(context, null).apply {
        isForceDarkAllowed = false
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        setTextSize(18)
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
    fun bindView(view: TerminalView) {
        boundView = view
    }

    override fun onTextChanged(session: TerminalSession) {
        boundView?.let { v -> v.post { v.onScreenUpdated(); v.invalidate() } }
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
            ioExecutor.execute { session?.write(text) }
        }
    }

    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(visible: Boolean) {
        boundView?.post { boundView?.onScreenUpdated() }
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
    private var currentSize = 18f
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
        val newSize = (currentSize * scaleFactor).coerceIn(18f, 81f)
        if (newSize != currentSize) {
            currentSize = newSize
            boundView?.let { v -> v.post { v.setTextSize(currentSize.toInt()) } }
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
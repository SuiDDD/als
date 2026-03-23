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
import sui.k.als.boot.alsPath
import sui.k.als.boot.su
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
private val io = Executors.newSingleThreadExecutor { r ->
    Thread(r, "als-io").apply {
        priority = Thread.MAX_PRIORITY
    }
}

fun cmd(s: String) {
    io.execute { ttySession?.write("$s\n") }
}

@Composable
fun TTYScreen(instance: TTYInstance) {
    val den = LocalDensity.current
    val view = instance.view
    val session = instance.session
    var imePx by remember { mutableIntStateOf(0) }
    val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(instance) {
        ttySession = session
        if (instance.initialized.compareAndSet(false, true)) {
            cmd("$su -M")
            cmd($$"export PS1=$'\\033[01;32m$(whoami)@$(getprop ro.product.model)\\033[00m:\\033[01;34m${PWD}\\033[00m# ' && cd $$alsPath && clear && busybox")
        }
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

fun createTTYInstance(
    ctx: Context, sessionClient: TTYSessionStub, viewClient: TTYViewStub
): TTYInstance {
    val dir = ctx.filesDir.absolutePath.also { File(it).mkdirs() }
    val session = TerminalSession(
        "/system/bin/sh", dir, arrayOf("-i"), arrayOf(
            "TERM=xterm-256color",
            "HOME=$dir",
            "LANG=en_US.UTF-8",
            "PATH=/system/bin:/system/xbin:$alsPath"
        ), 300000, sessionClient
    )
    val view = TerminalView(ctx, null).apply {
        isForceDarkAllowed = false
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        isFocusable = true
        isFocusableInTouchMode = true
        setTextSize(18)
        setTypeface(
            try {
                Typeface.createFromAsset(ctx.assets, "fonts/GoogleSansCode.ttf")
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

    override fun onTextChanged(s: TerminalSession) {
        boundView?.let { v -> v.post { v.onScreenUpdated(); v.invalidate() } }
    }

    override fun onTitleChanged(s: TerminalSession) {}
    override fun onSessionFinished(s: TerminalSession) {}
    override fun onCopyTextToClipboard(s: TerminalSession, t: String) {
        val ctx = boundView?.context ?: return
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("T", t))
    }

    override fun onPasteTextFromClipboard(s: TerminalSession?) {
        val ctx = boundView?.context ?: return
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.primaryClip?.getItemAt(0)?.let { item ->
            val text = item.coerceToText(ctx).toString()
            io.execute { s?.write(text) }
        }
    }

    override fun onBell(s: TerminalSession) {}
    override fun onColorsChanged(s: TerminalSession) {}
    override fun onTerminalCursorStateChange(b: Boolean) {
        boundView?.post { boundView?.onScreenUpdated() }
    }

    override fun getTerminalCursorStyle() = 2
    override fun setTerminalShellPid(s: TerminalSession, p: Int) {}
    override fun logError(t: String?, m: String?) {}
    override fun logWarn(t: String?, m: String?) {}
    override fun logInfo(t: String?, m: String?) {}
    override fun logDebug(t: String?, m: String?) {}
    override fun logVerbose(t: String?, m: String?) {}
    override fun logStackTraceWithMessage(t: String?, m: String?, e: Exception?) {}
    override fun logStackTrace(t: String?, e: Exception?) {}
}

open class TTYViewStub : TerminalViewClient {
    private var boundView: TerminalView? = null
    private var size = 18f
    fun bindView(view: TerminalView) {
        boundView = view
    }

    override fun readControlKey() = IMEState.consumeCtrl()
    override fun readAltKey() = IMEState.consumeAlt()
    override fun readShiftKey() = IMEState.consumeShift()
    override fun readFnKey() = false
    override fun onKeyDown(k: Int, e: KeyEvent, s: TerminalSession) = (k == KeyEvent.KEYCODE_BACK)
    override fun onKeyUp(k: Int, e: KeyEvent) = false
    override fun onCodePoint(c: Int, b: Boolean, s: TerminalSession) = false
    override fun onSingleTapUp(e: MotionEvent) {}
    override fun onLongPress(e: MotionEvent) = false
    override fun onScale(f: Float): Float {
        size *= f
        val targetSize = size.coerceAtLeast(9f)
        boundView?.let { v -> v.post { v.setTextSize(targetSize.toInt()) } }
        return 1f
    }

    override fun shouldEnforceCharBasedInput() = true
    override fun shouldBackButtonBeMappedToEscape() = true
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(b: Boolean) {}
    override fun onEmulatorSet() {}
    override fun logError(t: String?, m: String?) {}
    override fun logWarn(t: String?, m: String?) {}
    override fun logInfo(t: String?, m: String?) {}
    override fun logDebug(t: String?, m: String?) {}
    override fun logVerbose(t: String?, m: String?) {}
    override fun logStackTraceWithMessage(t: String?, m: String?, e: Exception?) {}
    override fun logStackTrace(t: String?, e: Exception?) {}
}
package sui.k.als.tty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.Choreographer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

val LocalSession = staticCompositionLocalOf<TerminalSession?> { null }
internal var ttySession: TerminalSession? = null
private val io = Executors.newSingleThreadExecutor { r ->
    Thread(r, "als-io").apply { priority = Thread.MAX_PRIORITY }
}

fun cmd(s: String) {
    io.execute { ttySession?.write("$s\n") }
}

@Composable
fun TTYScreen() {
    val ctx = LocalContext.current
    val den = LocalDensity.current
    val view = remember { TerminalView(ctx, null) }
    val dirty = remember { AtomicBoolean(false) }
    var imePx by remember { mutableIntStateOf(0) }
    val imeHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val policies = remember {
        File("/sys/devices/system/cpu/cpufreq/").listFiles { f -> f.name.startsWith("policy") }
            ?: emptyArray()
    }
    val cpuMask = remember {
        try {
            val count = File("/sys/devices/system/cpu/present").readText().split("-").last().trim()
                .toInt() + 1
            Integer.toHexString((1 shl count) - 1)
        } catch (_: Exception) {
            "ff"
        }
    }
    val session = remember {
        val dir = ctx.filesDir.absolutePath.also { File(it).mkdirs() }
        TerminalSession(
            "/system/bin/sh", dir, arrayOf("-i"), arrayOf(
                "TERM=xterm-256color",
                "HOME=$dir",
                "LANG=en_US.UTF-8",
                "PATH=/system/bin:/system/xbin:/data/als"
            ), 500000, object : TTYSessionStub() {
                override fun onTextChanged(s: TerminalSession) = dirty.lazySet(true)
                override fun onCopyTextToClipboard(s: TerminalSession, t: String) {
                    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                        ClipData.newPlainText("T", t)
                    )
                }

                override fun onPasteTextFromClipboard(s: TerminalSession?) {
                    val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.primaryClip?.getItemAt(0)?.let { item ->
                        val text = item.coerceToText(ctx).toString()
                        io.execute { s?.write(text) }
                    }
                }
            })
    }
    DisposableEffect(Unit) {
        val chro = Choreographer.getInstance()
        var last = System.currentTimeMillis()
        var burst = false
        val loop = object : Choreographer.FrameCallback {
            override fun doFrame(nanos: Long) {
                val now = System.currentTimeMillis()
                if (dirty.compareAndSet(true, false)) {
                    last = now
                    view.invalidate()
                    view.onScreenUpdated()
                    if (!burst) {
                        burst = true
                        io.execute {
                            try {
                                val run = mutableListOf<String>()
                                policies.forEach { run.add("echo performance > ${it.absolutePath}/scaling_governor") }
                                run.add($$"taskset -p $$cpuMask $(pgrep -f als-io)")
                                Runtime.getRuntime()
                                    .exec(arrayOf("su", "-c", run.joinToString(" && ")))
                            } catch (_: Exception) {
                            }
                        }
                    }
                } else if (burst && (now - last > 3000)) {
                    burst = false
                    io.execute {
                        try {
                            val run = mutableListOf<String>()
                            policies.forEach { run.add("echo schedutil > ${it.absolutePath}/scaling_governor") }
                            Runtime.getRuntime().exec(arrayOf("su", "-c", run.joinToString(" && ")))
                        } catch (_: Exception) {
                        }
                    }
                }
                chro.postFrameCallback(this)
            }
        }
        chro.postFrameCallback(loop)
        onDispose {
            chro.removeFrameCallback(loop)
            io.execute {
                try {
                    val run = mutableListOf<String>()
                    policies.forEach { run.add("echo schedutil > ${it.absolutePath}/scaling_governor") }
                    Runtime.getRuntime().exec(arrayOf("su", "-c", run.joinToString(" && ")))
                } catch (_: Exception) {
                }
            }
        }
    }
    val client = remember {
        object : TTYViewStub() {
            private var size = 18f
            override fun onScale(f: Float): Float {
                size *= f
                view.post { view.setTextSize(size.toInt()) }
                return 1f
            }

            override fun onSingleTapUp(e: MotionEvent) {
                view.requestFocus()
                view.postDelayed({
                    val imm =
                        ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(view, 0)
                }, 50)
            }
        }
    }
    LaunchedEffect(session) {
        ttySession = session
        cmd("su -M")
        cmd("cd /data/als && clear && busybox")
    }
    DisposableEffect(session) {
        view.apply {
            if (Build.VERSION.SDK_INT >= 29) isForceDarkAllowed = false
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            setTextSize(18)
            setTypeface(
                try {
                    Typeface.createFromAsset(ctx.assets, "fonts/GoogleSansCode.ttf")
                } catch (_: Exception) {
                    Typeface.MONOSPACE
                }
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            setTerminalViewClient(client)
            attachSession(session)
        }
        onDispose {
            io.execute { session.finishIfRunning() }
            if (ttySession == session) ttySession = null
        }
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
                    .padding(bottom = if (IMEState.isFloating) 0.dp else imeHeight + with(den) { imePx.toDp() })
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = imeHeight)
                    .onGloballyPositioned { imePx = it.size.height }) { TTYIME() }
        }
    }
}

open class TTYSessionStub : TerminalSessionClient {
    override fun onTextChanged(s: TerminalSession) {}
    override fun onTitleChanged(s: TerminalSession) {}
    override fun onSessionFinished(s: TerminalSession) {}
    override fun onCopyTextToClipboard(s: TerminalSession, t: String) {}
    override fun onPasteTextFromClipboard(s: TerminalSession?) {}
    override fun onBell(s: TerminalSession) {}
    override fun onColorsChanged(s: TerminalSession) {}
    override fun onTerminalCursorStateChange(b: Boolean) {}
    override fun getTerminalCursorStyle() = 2
    override fun setTerminalShellPid(s: TerminalSession, p: Int) {}
    override fun logError(t: String, m: String) {}
    override fun logWarn(t: String, m: String) {}
    override fun logInfo(t: String, m: String) {}
    override fun logDebug(t: String, m: String) {}
    override fun logVerbose(t: String, m: String) {}
    override fun logStackTrace(t: String, e: Exception) {}
    override fun logStackTraceWithMessage(t: String, m: String, e: Exception) {}
}

open class TTYViewStub : TerminalViewClient {
    override fun readControlKey() = IMEState.consumeCtrl()
    override fun readAltKey() = IMEState.consumeAlt()
    override fun readShiftKey() = IMEState.consumeShift()
    override fun readFnKey() = false
    override fun onKeyDown(k: Int, e: KeyEvent, s: TerminalSession) = (k == KeyEvent.KEYCODE_BACK)
    override fun onKeyUp(k: Int, e: KeyEvent) = false
    override fun onCodePoint(c: Int, b: Boolean, s: TerminalSession) = false
    override fun onSingleTapUp(e: MotionEvent) {}
    override fun onLongPress(e: MotionEvent) = false
    override fun onScale(f: Float) = 1f
    override fun shouldEnforceCharBasedInput() = true
    override fun shouldBackButtonBeMappedToEscape() = true
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(b: Boolean) {}
    override fun onEmulatorSet() {}
    override fun logError(t: String, m: String) {}
    override fun logWarn(t: String, m: String) {}
    override fun logInfo(t: String, m: String) {}
    override fun logDebug(t: String, m: String) {}
    override fun logVerbose(t: String, m: String) {}
    override fun logStackTrace(t: String, e: Exception) {}
    override fun logStackTraceWithMessage(t: String, m: String, e: Exception) {}
}
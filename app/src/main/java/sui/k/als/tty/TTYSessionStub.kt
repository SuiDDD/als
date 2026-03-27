package sui.k.als.tty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.util.concurrent.Executors

private val io = Executors.newSingleThreadExecutor { r ->
    Thread(r, "als-io").apply {
        priority = Thread.MAX_PRIORITY
    }
}

open class TTYSessionStub : TerminalSessionClient {
    private var boundInstance: TTYIns? = null
    fun bindInstance(instance: TTYIns) {
        boundInstance = instance
    }

    override fun onTextChanged(s: TerminalSession) {
        boundInstance?.view?.let { v -> v.post { v.onScreenUpdated(); v.invalidate() } }
    }

    override fun onTitleChanged(s: TerminalSession) {}
    override fun onSessionFinished(s: TerminalSession) {
        s.finishIfRunning()
        boundInstance?.isFinished?.value = true
        if (ttySession == s) ttySession = null
    }

    override fun onCopyTextToClipboard(s: TerminalSession, t: String) {
        (boundInstance?.view?.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
            ClipData.newPlainText("T", t)
        )
    }

    override fun onPasteTextFromClipboard(s: TerminalSession?) {
        val ctx = boundInstance?.view?.context ?: return
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip?.getItemAt(
            0
        )?.let { item ->
            io.execute { s?.write(item.coerceToText(ctx).toString()) }
        }
    }

    override fun getTerminalCursorStyle() = 2
    override fun logDebug(t: String?, m: String?) {}
    override fun logError(t: String?, m: String?) {}
    override fun logInfo(t: String?, m: String?) {}
    override fun logStackTrace(t: String?, e: Exception?) {}
    override fun logStackTraceWithMessage(t: String?, m: String?, e: Exception?) {}
    override fun logVerbose(t: String?, m: String?) {}
    override fun logWarn(t: String?, m: String?) {}
    override fun onBell(s: TerminalSession) {}
    override fun onColorsChanged(s: TerminalSession) {}
    override fun onTerminalCursorStateChange(b: Boolean) {}
    override fun setTerminalShellPid(s: TerminalSession, p: Int) {}
}
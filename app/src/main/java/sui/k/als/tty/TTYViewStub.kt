package sui.k.als.tty

import android.view.KeyEvent
import android.view.MotionEvent
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

open class TTYViewStub : TerminalViewClient {
    private var boundView: TerminalView? = null
    private var size = 18f
    fun bindView(view: TerminalView) {
        boundView = view
    }

    override fun copyModeChanged(b: Boolean) {}
    override fun isTerminalViewSelected() = true
    override fun logDebug(t: String?, m: String?) {}
    override fun logError(t: String?, m: String?) {}
    override fun logInfo(t: String?, m: String?) {}
    override fun logStackTrace(t: String?, e: Exception?) {}
    override fun logStackTraceWithMessage(t: String?, m: String?, e: Exception?) {}
    override fun logVerbose(t: String?, m: String?) {}
    override fun logWarn(t: String?, m: String?) {}
    override fun onCodePoint(c: Int, b: Boolean, s: TerminalSession) = false
    override fun onEmulatorSet() {}
    override fun onKeyDown(k: Int, e: KeyEvent, s: TerminalSession) = (k == KeyEvent.KEYCODE_BACK)
    override fun onKeyUp(k: Int, e: KeyEvent) = false
    override fun onLongPress(e: MotionEvent) = false
    override fun onScale(f: Float): Float {
        size = (size * f).coerceIn(18f, 99f)
        boundView?.let { v -> v.post { v.setTextSize(size.toInt()) } }
        return 1f
    }

    override fun onSingleTapUp(e: MotionEvent) {}
    override fun readAltKey() = IMEState.consumeAlt()
    override fun readControlKey() = IMEState.consumeCtrl()
    override fun readFnKey() = false
    override fun readShiftKey() = IMEState.consumeShift()
    override fun shouldBackButtonBeMappedToEscape() = true
    override fun shouldEnforceCharBasedInput() = true
    override fun shouldUseCtrlSpaceWorkaround() = false
}
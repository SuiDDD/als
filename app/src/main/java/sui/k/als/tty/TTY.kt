package sui.k.als.tty

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.staticCompositionLocalOf
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import sui.k.als.boot.BootConfig
import java.io.File

val LocalSession = staticCompositionLocalOf<TerminalSession?> { null }
internal var ttySession: TerminalSession? = null
fun cTTYIns(ctx: Context, sessionClient: TTYSessionStub, viewClient: TTYViewStub): TTYIns {
    val dir = ctx.filesDir.absolutePath.also { File(it).mkdirs() }
    val profile = File(dir, "profile")
    val d = "$"
    val script =
        $$"export HOME=$${BootConfig.alsPath}\nexport PATH=$${BootConfig.alsPath}:$${d}PATH\nPS1=$'\\001\\e[1;37m\\002$(whoami)@$(getprop ro.product.model):$PWD\\001$([ $? -eq 0 ] && echo \"\\e[1;32m\" || echo \"\\e[1;31m\")\\002$([ \"$(id -u)\" = \"0\" ] && echo \"#\" || echo \"$\")\\001\\e[0m\\002 '\ncd $${BootConfig.alsPath}"
    profile.writeText(script)
    val session = TerminalSession(
        "/system/bin/sh", dir, arrayOf("-i"), arrayOf(
            "TERM=xterm-256color", "LANG=zh_CN.UTF-8", "HOME=${BootConfig.alsPath}", "ENV=$profile"
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
    return TTYIns(session, view).also { sessionClient.bindInstance(it); viewClient.bindView(view) }
}
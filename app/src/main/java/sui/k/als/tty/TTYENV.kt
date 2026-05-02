package sui.k.als.tty

import com.termux.terminal.*
import sui.k.als.*

object TTYENV {
    val args = arrayOf("-i")
    val env = arrayOf(
        "ANDROID_ART_ROOT=${System.getenv("ANDROID_ART_ROOT")}",
        "ANDROID_DATA=${System.getenv("ANDROID_DATA")}",
        "ANDROID_I18N_ROOT=${System.getenv("ANDROID_I18N_ROOT")}",
        "ANDROID_ROOT=${System.getenv("ANDROID_ROOT")}",
        "ANDROID_RUNTIME_ROOT=${System.getenv("ANDROID_RUNTIME_ROOT")}",
        "ANDROID_STORAGE=${System.getenv("ANDROID_STORAGE")}",
        "ANDROID_TZDATA_ROOT=${System.getenv("ANDROID_TZDATA_ROOT")}",
        "BOOTCLASSPATH=${System.getenv("BOOTCLASSPATH")}",
        "COLORTERM=truecolor",
        "DEX2OATBOOTCLASSPATH=${System.getenv("DEX2OATBOOTCLASSPATH")}",
        "EXTERNAL_STORAGE=${System.getenv("EXTERNAL_STORAGE")}",
        "HOME=$alsDir",
        "LANG=en_US.UTF-8",
        "PATH=/system/bin:/system/xbin:$alsDir",
        "STANDALONE_SYSTEMSERVER_JARS=${System.getenv("STANDALONE_SYSTEMSERVER_JARS")}",
        "SYSTEMSERVERCLASSPATH=${System.getenv("SYSTEMSERVERCLASSPATH")}",
        "TERM=xterm-direct"
    )
}

fun TerminalSession(env: TTYENV, rows: Int, client: TerminalSessionClient): TerminalSession =
    TerminalSession("sh", alsDir, env.args, env.env, rows, client)
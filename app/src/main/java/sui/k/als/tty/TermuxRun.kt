package sui.k.als.tty

import sui.k.als.su
import java.util.concurrent.Executors

private val txExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "als-txc")
}

fun txc(command: String) {
    txExecutor.execute {
        try {
            val script = """
                HOME="/data/data/com.termux/files/home"
                PROP="${'$'}HOME/.termux/termux.properties"
                mkdir -p "${'$'}HOME/.termux"
                [ ! -f "${'$'}PROP" ] && touch "${'$'}PROP"
                if ! grep -q "allow-external-apps = true" "${'$'}PROP"; then
                    echo "allow-external-apps = true" >> "${'$'}PROP"
                fi
                T_UID=$(stat -c %u /data/data/com.termux)
                chown -R ${'$'}T_UID:${'$'}T_UID "${'$'}HOME/.termux"
                am broadcast -a com.termux.RELOAD_SETTINGS com.termux/.app.SettingsReceiver > /dev/null
                am startservice --user 0 \
                    -n com.termux/.app.RunCommandService \
                    -a com.termux.RUN_COMMAND \
                    --es com.termux.RUN_COMMAND_PATH "/data/data/com.termux/files/usr/bin/login" \
                    --esa com.termux.RUN_COMMAND_ARGUMENTS "$command" \
                    --ez com.termux.RUN_COMMAND_BACKGROUND false \
                    --ei com.termux.RUN_COMMAND_SESSION_ACTION 2 > /dev/null
            """.trimIndent()
            Runtime.getRuntime().exec(arrayOf(su, "-c", script)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

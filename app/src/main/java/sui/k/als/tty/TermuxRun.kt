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
                
                # 强力清理并设置属性，确保不被注释且值唯一
                if [ -f "${'$'}PROP" ]; then
                    grep -v "allow-external-apps" "${'$'}PROP" > "${'$'}PROP.tmp"
                else
                    touch "${'$'}PROP.tmp"
                fi
                echo "allow-external-apps = true" >> "${'$'}PROP.tmp"
                mv "${'$'}PROP.tmp" "${'$'}PROP"
                
                # 修正 UID 并确保权限可读
                T_UID=$(stat -c %u /data/data/com.termux)
                chown -R ${'$'}T_UID:${'$'}T_UID "${'$'}HOME/.termux"
                chmod 600 "${'$'}PROP"
                
                # 发送广播强制 Termux 刷新配置 (使用完整路径)
                am broadcast --user 0 -a com.termux.RELOAD_SETTINGS com.termux/com.termux.app.SettingsReceiver > /dev/null
                
                # 稍微延迟等待配置生效
                sleep 0.2
                
                # 使用 sh 执行命令，避免 login 模式下参数解析失败
                # --esa 传参建议用逗号分隔多个参数
                am startservice --user 0 \
                    -n com.termux/.app.RunCommandService \
                    -a com.termux.RUN_COMMAND \
                    --es com.termux.RUN_COMMAND_PATH "/data/data/com.termux/files/usr/bin/sh" \
                    --esa com.termux.RUN_COMMAND_ARGUMENTS "-c,$command" \
                    --ez com.termux.RUN_COMMAND_BACKGROUND false \
                    --ei com.termux.RUN_COMMAND_SESSION_ACTION 2 > /dev/null
            """.trimIndent()
            Runtime.getRuntime().exec(arrayOf(su, "-M", "-c", script)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

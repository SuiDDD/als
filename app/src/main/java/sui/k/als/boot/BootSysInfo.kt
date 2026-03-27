package sui.k.als.boot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BootSysInfo {
    suspend fun getInfo(su: String): String = withContext(Dispatchers.IO) {
        val cmd =
            "echo \"$(uname -m)\\n$(/system/bin/getenforce)\\n$(df /data | awk 'NR==2 {printf \"%.2f GB\", $4/1024/1024}') Free\\n$(cat /sys/class/power_supply/battery/capacity)%\\n$(uname -r)\""
        try {
            ProcessBuilder(su, "-c", cmd).start().inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun checkRoot(su: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ProcessBuilder(su, "-c", "echo 1").start().waitFor() == 0
        }.getOrElse { false }
    }

    suspend fun hasEnv(su: String): Boolean = withContext(Dispatchers.IO) {
        val check = "[ -f ${BootConfig.alsPath}busybox ]"
        ProcessBuilder(su, "-c", check).start().waitFor() == 0
    }

    suspend fun isGunyah(su: String): Boolean = withContext(Dispatchers.IO) {
        ProcessBuilder(su, "-c", "[ -e /dev/gunyah ]").start().waitFor() == 0
    }
}
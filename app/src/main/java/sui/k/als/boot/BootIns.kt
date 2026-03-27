package sui.k.als.boot

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object BootIns {
    fun deploy(ctx: Context, su: String) {
        val path = BootConfig.alsPath
        ProcessBuilder(su, "-c", "mkdir -p $path && chmod 777 $path").start().waitFor()
        listOf("busybox", "01.tar.xz").forEach { name ->
            val tmp = File(ctx.cacheDir, name)
            ctx.assets.open(name)
                .use { input -> FileOutputStream(tmp).use { output -> input.copyTo(output) } }
            ProcessBuilder(
                su, "-c", "cp ${tmp.absolutePath} $path$name && chmod 755 $path$name"
            ).start().waitFor()
            tmp.delete()
        }
    }

    fun ensureBin(su: String) {
        val cmd =
            "[ -f ${BootConfig.alsPath}i ] || (cd ${BootConfig.alsPath} && ./busybox tar -xJf 01.tar.xz)"
        ProcessBuilder(su, "-c", cmd).start().waitFor()
    }
}
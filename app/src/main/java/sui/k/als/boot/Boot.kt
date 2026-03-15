package sui.k.als.boot

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sui.k.als.localAppFont
import java.io.File

@Composable
fun BootScreen(onFinished: () -> Unit) {
    val font = localAppFont.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var finished by remember { mutableStateOf(false) }
    var subMenu by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var systemInfo by remember { mutableStateOf("") }
    var containers by remember { mutableStateOf(listOf<String>()) }
    var containerSelected by remember { mutableStateOf<String?>(null) }
    fun scanContainers() = scope.launch(Dispatchers.IO) {
        containers = ProcessBuilder(
            "su",
            "-c",
            "ls -d /data/als/dev/*/ 2>/dev/null | xargs -n1 basename"
        ).start().inputStream.bufferedReader().readLines().filter { it.contains("-") }
            .sortedBy { it.substringBefore("-").toIntOrNull() ?: 999 }
    }

    val menuItems = when {
        containerSelected != null -> listOf("BACK")
        subMenu -> listOf("Import RootFS", "Back")
        else -> containers + listOf("Linux Setup", "Exit")
    }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    isRunning = true
                    val fileName = context.contentResolver.query(it, null, null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(
                                cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                            ) else "rootfs"
                        } ?: "rootfs"
                    val tempFile = File(
                        context.cacheDir,
                        fileName
                    ).apply {
                        context.contentResolver.openInputStream(it)
                            ?.use { input -> outputStream().use { out -> input.copyTo(out) } }
                    }
                    val nextIndex = (ProcessBuilder(
                        "su",
                        "-c",
                        "ls -d /data/als/dev/*/ 2>/dev/null | wc -l"
                    ).start().inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 0) + 1
                    val targetDir = "/data/als/dev/$nextIndex-$fileName"
                    ProcessBuilder(
                        "su",
                        "-c",
                        "mkdir -p $targetDir && tar -xapf ${tempFile.absolutePath} -C $targetDir"
                    ).start().waitFor()
                    tempFile.delete(); isRunning = false; finished = true; scanContainers()
                }
            }
        }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ProcessBuilder(
                "su",
                "-c",
                "[ ! -d /data/als/sys/bin ] && mkdir -p /data/als/sys/bin; [ ! -d /data/als/dev ] && mkdir -p /data/als/dev"
            ).start().waitFor()
            if (ProcessBuilder(
                    "su",
                    "-c",
                    "[ ! -f /data/als/busybox ] && echo 1"
                ).start().inputStream.bufferedReader().readText().contains("1")
            ) {
                File(context.cacheDir, "busybox").apply {
                    context.assets.open("busybox")
                        .use { i -> outputStream().use { o -> i.copyTo(o) } }
                    ProcessBuilder(
                        "su",
                        "-c",
                        "cp $absolutePath /data/als/busybox && chmod 755 /data/als/busybox"
                    ).start().waitFor()
                    delete()
                }
            }
            ProcessBuilder("su", "-c", "/data/als/busybox --install -s /data/als/sys/bin").start()
                .waitFor()
            systemInfo = ProcessBuilder(
                "su",
                "-c",
                "echo \"\$(uname -m)\n\$(/system/bin/getenforce)\n\$(df /data | awk 'NR==2 {printf \"%.2f GB\", \$4/1024/1024}') Free\n\$(cat /sys/class/power_supply/battery/capacity)% [\$(cat /sys/class/power_supply/battery/status)]\n\$(uname -r)\""
            ).start().inputStream.bufferedReader().readText()
            scanContainers()
        }
    }
    Splash(
        modifier = Modifier
            .pointerInput(subMenu, isRunning, finished, containerSelected) {
                if (!isRunning && !finished) detectVerticalDragGestures { _, d ->
                    if (d > 15f) selectedIndex =
                        (selectedIndex - 1).coerceAtLeast(0) else if (d < -15f) selectedIndex =
                        (selectedIndex + 1).coerceAtMost(menuItems.size - 1)
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isRunning) {
                    if (finished) {
                        finished = false; subMenu = false; selectedIndex = 0
                    } else if (containerSelected != null) {
                        containerSelected = null; selectedIndex = 0
                    } else if (subMenu) when (selectedIndex) {
                        0 -> filePicker.launch("*/*"); 1 -> {
                            subMenu = false; selectedIndex = 0
                        }
                    }
                    else when (val item = menuItems[selectedIndex]) {
                        "Linux Setup" -> {
                            subMenu = true; selectedIndex = 0
                        }; "Exit" -> onFinished(); else -> {
                            containerSelected = item; selectedIndex = 0
                        }
                    }
                }
            },
        header = {
            Text(
                systemInfo,
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                Color.Green,
                fontSize = 9.sp,
                fontFamily = font,
                lineHeight = 11.sp
            )
        },
        content = {
            Spacer(Modifier.height(10.dp))
            if (!isRunning && !finished) menuItems.forEachIndexed { i, t ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(if (selectedIndex == i) Color.DarkGray else Color.Transparent),
                    Alignment.Center
                ) {
                    Text(
                        if (selectedIndex == i) "> $t <" else t,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = font
                    )
                }
            }
        }
    )
}

package sui.k.als.boot
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sui.k.als.localAppFont
@Composable
fun BootScreen(onFinished: () -> Unit) {
    val font = localAppFont.current
    val scope = rememberCoroutineScope()
    var systemInfo by remember { mutableStateOf("") }
    var gunyahStatus by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val cmd = """echo "$(uname -m)
$(/system/bin/getenforce)
$(df /data | awk 'NR==2 {printf "%.2f GB", ${'$'}4/1024/1024}') Free
$(cat /sys/class/power_supply/battery/capacity)% [$(cat /sys/class/power_supply/battery/status)]
$(uname -r)""""
            systemInfo = try {
                ProcessBuilder("su", "-c", cmd).start().inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                "ROOT_ACCESS_DENIED"
            }
        }
    }
    Splash(
        header = {
            Text(
                text = systemInfo,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                color = Color.Green,
                fontSize = 9.sp,
                fontFamily = font,
                lineHeight = 11.sp
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(8.dp))
                val newBg = when (gunyahStatus) {
                    true -> Color.Green
                    false -> Color.Red
                    null -> Color.Transparent
                }
                MenuLine("New", font, customBg = newBg) {
                    scope.launch(Dispatchers.IO) {
                        val hasGunyah = try {
                            val p = ProcessBuilder("su", "-c", "[ -e /dev/gunyah ] && echo 1 || echo 0").start()
                            p.inputStream.bufferedReader().readText().trim() == "1"
                        } catch (e: Exception) {
                            false
                        }
                        gunyahStatus = hasGunyah
                    }
                }
                MenuLine("Exit", font) { onFinished() }
            }
        }
    )
}
@Composable
fun MenuLine(
    text: String,
    font: androidx.compose.ui.text.font.FontFamily,
    customBg: Color = Color.Transparent,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .background(if (customBg != Color.Transparent) customBg else Color.Transparent, RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "  $text",
            color = if (customBg == Color.Green || customBg == Color.Red) Color.Black else Color.LightGray,
            fontSize = 9.sp,
            fontFamily = font
        )
    }
}
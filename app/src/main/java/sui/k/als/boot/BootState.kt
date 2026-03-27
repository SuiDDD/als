package sui.k.als.boot

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BootState(private val context: Context) {
    var information by mutableStateOf("")
    var showEdit by mutableStateOf(false)
    var refreshTrigger by mutableIntStateOf(0)
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (!BootSysInfo.checkRoot(BootSU.path)) {
            showEdit = true
            return@withContext
        }
        showEdit = false
        if (!BootSysInfo.hasEnv(BootSU.path)) BootIns.deploy(context, BootSU.path)
        information = BootSysInfo.getInfo(BootSU.path)
    }
}

@Composable
fun BootSysInfoText(info: String, font: FontFamily) {
    Text(
        text = info,
        modifier = Modifier.fillMaxSize(),
        color = BootConfig.ubuntuOrange,
        fontSize = 9.sp,
        fontFamily = font,
        lineHeight = 11.sp
    )
}
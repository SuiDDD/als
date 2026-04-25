package sui.k.als.vm.cvm

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import sui.k.als.alsPath
import sui.k.als.localFont

@Composable
fun CVMPreview(stateMap: MutableMap<String, Any>) {
    val command by remember {
        derivedStateOf {
            buildString {
                append("$alsPath/app/cvm/crosvm run ")
                append(stateMap["archive"]?.toString() ?: "")
                append(stateMap["processor"]?.toString() ?: "")
                append(stateMap["memory"]?.toString() ?: "")
                append(stateMap["display"]?.toString() ?: "")
                append(stateMap["disk"]?.toString() ?: "")
            }.also { stateMap["cvmcmd"] = it }
        }
    }
    SelectionContainer {
        Text(command, fontSize = 9.sp, color = Color.Gray, fontFamily = localFont.current)
    }
}
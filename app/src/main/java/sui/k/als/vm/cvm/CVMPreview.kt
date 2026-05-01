package sui.k.als.vm.cvm

import androidx.compose.foundation.text.selection.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import sui.k.als.*

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
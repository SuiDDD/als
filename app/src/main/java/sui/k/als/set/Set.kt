package sui.k.als.set

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sui.k.als.R
import sui.k.als.ui.ALSButton
import sui.k.als.ui.ALSList

@Composable
fun Set(onBack: () -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (showAbout) {
            About { showAbout = false }
        } else {
            Column(Modifier.fillMaxSize().padding(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ALSButton(icon = R.drawable.info, click = { showAbout = true })
                    ALSList(data = "关于", first = true, last = true, onClick = { showAbout = true })
                }
            }
        }
    }
}

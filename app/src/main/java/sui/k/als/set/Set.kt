package sui.k.als.set

import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import sui.k.als.ui.*

@Composable
fun Set(onBack: () -> Unit) {
    var showAbout by remember { mutableStateOf(false) }
    BackHandler { if (showAbout) showAbout = false else onBack() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (showAbout) {
            About { showAbout = false }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(9.dp)
            ) {
                ALSList(
                    data = "关于", first = true, last = true, onClick = { showAbout = true })
            }
        }
    }
}
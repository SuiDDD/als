package sui.k.als

import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import sui.k.als.chr.qcom.*
import sui.k.als.set.*
import sui.k.als.tty.*
import sui.k.als.ui.*
import sui.k.als.vm.*

@Composable
fun App() {
    var showQVM by remember { mutableStateOf(false) }
    var showCVM by remember { mutableStateOf(false) }
    var showChr by remember { mutableStateOf(false) }
    var showSet by remember { mutableStateOf(false) }
    var activeTTY by remember { mutableStateOf<TTYInstance?>(null) }
    LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(activeTTY != null || showQVM || showCVM || showChr || showSet) {
        when {
            activeTTY != null -> activeTTY = null
            showQVM -> showQVM = false
            showCVM -> showCVM = false
            showChr -> showChr = false
            showSet -> showSet = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (activeTTY != null) {
            TTYScreen(activeTTY!!) { TTYIME() }
        } else when {
            showQVM -> QVM { showQVM = false }
            showChr -> Chr(onTTYCreated = { activeTTY = it }, scope = scope)
            showSet -> Set { showSet = false }
            else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ALSButton("Q", iconTint = Color(0xFFFD6500)) { showQVM = true }
                    ALSButton("C") { showChr = true }
                    ALSButton(R.drawable.settings) { showSet = true }
                }
            }
        }
    }
}

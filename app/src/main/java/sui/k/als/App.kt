package sui.k.als
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import sui.k.als.set.Set
import sui.k.als.tty.TTYIME
import sui.k.als.tty.TTYInstance
import sui.k.als.tty.TTYScreen
import sui.k.als.ui.ALSButton
import sui.k.als.vm.CVM
import sui.k.als.vm.QVM
@Composable
fun App() {
    var showQVM by remember { mutableStateOf(false) }
    var showCVM by remember { mutableStateOf(false) }
    var showChr by remember { mutableStateOf(false) }
    var showSet by remember { mutableStateOf(false) }
    var activeTTY by remember { mutableStateOf<TTYInstance?>(null) }
    val context = LocalContext.current
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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (activeTTY != null) {
            TTYScreen(activeTTY!!) { TTYIME() }
        } else when {
            showQVM -> QVM { showQVM = false }
            showCVM -> CVM { showCVM = false }
            showChr -> sui.k.als.chr.qcom.Chr(onExit = { showChr = false }, onTTYCreated = { activeTTY = it }, scope = scope)
            showSet -> Set { showSet = false }
            else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ALSButton("Q", iconTint = Color(0xFFFD6500)) { showQVM = true }
                    ALSButton("C", iconTint = Color(0xFF37AAC6)) { showCVM = true }
                    ALSButton("C") { showChr = true }
                    ALSButton(R.drawable.settings) { showSet = true }
                }
            }
        }
    }
}
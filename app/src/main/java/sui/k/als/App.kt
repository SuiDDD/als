package sui.k.als

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sui.k.als.chr.qcom.chr
import sui.k.als.set.Set
import sui.k.als.ui.ALSButton
import sui.k.als.vm.CVM
import sui.k.als.vm.QVM

@Composable
fun App() {
    val (showQVM, setShowQVM) = remember { mutableStateOf(false) }
    val (showCVM, setShowCVM) = remember { mutableStateOf(false) }
    val (showSet, setShowSet) = remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            showQVM -> QVM { setShowQVM(false) }
            showCVM -> CVM { setShowCVM(false) }
            showSet -> Set { setShowSet(false) }
            else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ALSButton("Q", iconTint = Color(0xFFFD6500)) { setShowQVM(true) }
                    ALSButton("C", iconTint = Color(0xFF37AAC6)) { setShowCVM(true) }
                    ALSButton("C") { chr() }
                    ALSButton(R.drawable.settings) { setShowSet(true) }
                }
            }
        }
    }
}
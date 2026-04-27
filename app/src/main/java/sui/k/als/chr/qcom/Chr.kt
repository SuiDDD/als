package sui.k.als.chr.qcom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import sui.k.als.R
import sui.k.als.tty.TTYInstance
import sui.k.als.ui.ALSButton

@Composable
fun Chr(onExit: () -> Unit, onTTYCreated: (TTYInstance) -> Unit, scope: CoroutineScope) {
    var isCreating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (isCreating) {
            ChrCreate(onBack = { isCreating = false }, onTTYCreated = onTTYCreated, scope = scope)
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 9.dp),
                Alignment.Center
            ) {
                ALSButton(R.drawable.add) { isCreating = true }
            }
        }
    }
}
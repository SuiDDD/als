package sui.k.als.boot

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.tty.TTYIns

object BootSys {
    fun onClick(
        context: Context,
        scope: CoroutineScope,
        imm: InputMethodManager,
        sessions: SnapshotStateList<Pair<String, TTYIns>>,
        label: String,
        onActiveChanged: (TTYIns) -> Unit,
        onShowTerminal: (Boolean) -> Unit,
        onError: (Color) -> Unit
    ) {
        scope.launch {
            if (!BootSysInfo.isGunyah(BootSU.path)) {
                onError(Color.Red); return@launch
            }
            BootIns.ensureBin(BootSU.path)
            val instance = BootSession.create(
                context, imm
            ) {
                sessions.find { it.second.view.isFocused }?.second ?: sessions.lastOrNull()?.second
            }
            sessions.add(label to instance)
            onActiveChanged(instance)
            onShowTerminal(true)
            delay(90)
            instance.session.write("su\n")
            instance.session.write("clear && ./i\n")
        }
    }
}
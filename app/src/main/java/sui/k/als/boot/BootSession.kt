package sui.k.als.boot

import android.content.Context
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sui.k.als.tty.TTYIns
import sui.k.als.tty.TTYSessionStub
import sui.k.als.tty.TTYViewStub
import sui.k.als.tty.cTTYIns

object BootSession {
    fun create(
        context: Context, inputMethodManager: InputMethodManager, getActive: () -> TTYIns?
    ): TTYIns {
        return cTTYIns(context, object : TTYSessionStub() {}, object : TTYViewStub() {
            override fun onSingleTapUp(e: MotionEvent) {
                val current = getActive()
                current?.view?.requestFocus()
                inputMethodManager.showSoftInput(current?.view, 0)
            }
        })
    }

    fun onClick(
        context: Context,
        scope: CoroutineScope,
        imm: InputMethodManager,
        sessions: SnapshotStateList<Pair<String, TTYIns>>,
        label: String,
        onActiveChanged: (TTYIns) -> Unit,
        onShowTerminal: (Boolean) -> Unit
    ) {
        val instance = create(context, imm) {
            sessions.find { it.second.view.isFocused }?.second ?: sessions.lastOrNull()?.second
        }
        sessions.add("${label}#${sessions.size}" to instance)
        onActiveChanged(instance)
        onShowTerminal(true)
        scope.launch {
            delay(200)
            instance.session.write("su\n")
            delay(100)
            instance.session.write("cd ${BootConfig.alsPath} && clear && ./busybox\n")
        }
    }

    @Composable
    fun SessionBar(
        sessions: List<Pair<String, TTYIns>>,
        active: TTYIns?,
        font: FontFamily,
        onSelect: (TTYIns) -> Unit
    ) {
        if (sessions.isEmpty()) return
        LazyRow(
            Modifier
                .fillMaxWidth()
                .height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(sessions) { _, pair ->
                val isActive = active == pair.second
                Box(
                    Modifier
                        .width(90.dp)
                        .fillMaxHeight()
                        .background(if (isActive) BootConfig.ubuntuOrange else BootConfig.menuItemBg)
                        .clickable { onSelect(pair.second) }, Alignment.Center
                ) {
                    Text(
                        pair.first,
                        color = if (isActive) Color.Black else Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = font
                    )
                }
            }
        }
    }
}
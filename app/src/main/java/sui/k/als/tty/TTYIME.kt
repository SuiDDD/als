package sui.k.als.tty

import android.view.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.*

object IMEState {
    var isCtrlActive by mutableStateOf(false)
    var isShiftActive by mutableStateOf(false)
    var isAltActive by mutableStateOf(false)
    var isCapsActive by mutableStateOf(false)
    var isFullKeyboardVisible by mutableStateOf(false)
    var isFloating by mutableStateOf(false)
    var keyboardOffset by mutableStateOf(IntOffset.Zero)
    fun consumeCtrl() = isCtrlActive
    fun consumeShift() = isShiftActive
    fun consumeAlt() = isAltActive
}

private val keyCodes =
    "Tab·\t¦Esc·\u001b¦Enter·\r¦Back·\u007f¦ · ¦↑·\u001b[A¦↓·\u001b[B¦←·\u001b[D¦→·\u001b[C¦Home·\u001b[1~¦End·\u001b[4~¦Del·\u001b[3~¦F1·\u001bOP¦F2·\u001bOQ¦F3·\u001bOR¦F4·\u001bOS¦F5·\u001b[15~¦F6·\u001b[17~¦F7·\u001b[18~¦F8·\u001b[19~¦F9·\u001b[20~¦F10·\u001b[21~¦F11·\u001b[23~¦F12·\u001b[24~".split(
        '¦'
    ).associate { it.split('·').let { p -> p[0] to p[1] } }
private val symbolMap =
    "`~·1!·2@·3#·4$·5%·6^·7&·8*·9(·0)·-_·=+·[{·]}·\\|·;:·'\"·,<·.>·/?".split('·')
        .associate { it[0].toString() to it[1].toString() }

@Composable
fun TTYIME() {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val windowSize = LocalWindowInfo.current.containerSize
    val panelHeight =
        with(density) { (if (config.orientation == 2) windowSize.height / 2 else windowSize.height / 3).toDp() }
    BackHandler(IMEState.isFullKeyboardVisible) {
        IMEState.isFloating = false; IMEState.isFullKeyboardVisible = false
    }
    Box(modifier = if (IMEState.isFloating) Modifier
        .offset { IMEState.keyboardOffset }
        .size(360.dp, panelHeight) else Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(0.7f))
        ) {
            if (!IMEState.isFullKeyboardVisible) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    listOf(
                        listOf("Esc", "F1", "F2", "F3", "·", "F4", "F5", "F6", "Del"),
                        listOf("Shift", "F7", "F8", "F9", "↑", "F10", "F11", "F12", "Back"),
                        listOf("Tab", "Ctrl", "Alt", "←", "↓", "→", "Home", "End", "Enter")
                    ).forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            row.forEach {
                                KeyBase(
                                    it, 1f, it in listOf("Ctrl", "Alt", "Shift"), it == "·"
                                )
                            }
                        }
                    }
                }
            } else {
                val layout =
                    "Esc·F1·F2·F3·F4·F5·F6··F7·F8·F9·F10·F11·F12·Del¦`·1·2·3·4·5·6·7·8·9·0·-·=·Back¦Tab·Q·W·E·R·T·Y·U·I·O·P·[·]·\\¦Caps·A·S·D·F·G·H·J·K·L·;·'·Enter¦Shift·Z·X·C·V·B·N·M·,·.·↑·/¦Ctrl·Alt·Home· ·End·←·↓·→".split(
                        '¦'
                    ).map { it.split('·') }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(panelHeight)
                ) {
                    layout.forEach { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            row.forEach { label ->
                                KeyBase(
                                    label, when (label) {
                                        " " -> 3.9f; "Ctrl", "Alt", "Home", "End" -> 1.2f; else -> 1f
                                    }, label in listOf("Ctrl", "Shift", "Alt", "Caps"), label == ""
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyBase(
    label: String, weight: Float, isMod: Boolean = false, isCtrl: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val isActive = when (label) {
        "Ctrl" -> IMEState.isCtrlActive; "Shift" -> IMEState.isShiftActive; "Alt" -> IMEState.isAltActive; "Caps" -> IMEState.isCapsActive; else -> false
    }
    val disp = when {
        isCtrl -> ""; isMod || label.length > 1 || (!label[0].isLetter() && !symbolMap.containsKey(
            label
        )) -> label; IMEState.isShiftActive -> symbolMap[label]
            ?: label.uppercase(); IMEState.isCapsActive && label[0].isLetter() -> label.uppercase(); else -> label
    }
    Box(modifier = Modifier
        .weight(weight)
        .fillMaxHeight()
        .pointerInput(label) {
            if (isCtrl) detectDragGestures(
                onDragStart = {
                isPressed = true; if (IMEState.isFullKeyboardVisible) {
                IMEState.isFloating = true; view.performHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS
                )
            }
            },
                onDragEnd = { isPressed = false },
                onDragCancel = { isPressed = false }) { change, drag ->
                if (IMEState.isFloating) {
                    change.consume(); IMEState.keyboardOffset += IntOffset(
                        drag.x.roundToInt(), drag.y.roundToInt()
                    )
                }
            }
        }
        .pointerInput(label) {
            detectTapGestures(onPress = {
                isPressed = true; view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (isCtrl) {
                    try {
                        awaitRelease(); IMEState.isFullKeyboardVisible =
                            !IMEState.isFullKeyboardVisible.also {
                                if (!it) {
                                    IMEState.isFloating = false; IMEState.keyboardOffset =
                                        IntOffset.Zero
                                }
                            }
                    } finally {
                        isPressed = false
                    }
                } else if (isMod) {
                    val setter: (Boolean) -> Unit = { v ->
                        when (label) {
                            "Ctrl" -> IMEState.isCtrlActive = v; "Shift" -> IMEState.isShiftActive =
                            v; "Alt" -> IMEState.isAltActive = v; "Caps" -> IMEState.isCapsActive =
                            v
                        }
                    }
                    setter(true)
                    try {
                        awaitRelease()
                    } finally {
                        setter(false); isPressed = false
                    }
                } else {
                    val job = scope.launch {
                        processKey(label); delay(270); while (true) {
                        processKey(label); delay(30)
                    }
                    }
                    try {
                        awaitRelease()
                    } finally {
                        job.cancel(); isPressed = false
                    }
                }
            })
        }
        .background(Color.Transparent), contentAlignment = Alignment.Center) {
        Text(
            disp,
            color = if (isPressed || isActive) Color.Gray else Color.White,
            fontSize = 12.sp,
            softWrap = false
        )
    }
}

private fun processKey(label: String) {
    keyCodes[label]?.let { sendToTTY(if (IMEState.isAltActive && label != "Alt") "\u001b$it" else it) }
        ?: run {
            val upper =
                IMEState.isShiftActive || (IMEState.isCapsActive && label.length == 1 && label[0].isLetter())
            var text = if (IMEState.isShiftActive) (symbolMap[label]
                ?: label.uppercase()) else if (upper) label.uppercase() else label.lowercase()
            if (IMEState.isCtrlActive && text.length == 1) text.uppercase()[0].let {
                if (it in '@'..'_') text = (it.code - '@'.code).toChar().toString()
            }
            sendToTTY(if (IMEState.isAltActive) "\u001b$text" else text)
        }
}

private fun sendToTTY(data: String) = ttySession?.write(data)
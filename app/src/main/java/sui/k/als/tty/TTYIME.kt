package sui.k.als.tty

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object IMEState {
    var isCtrlActive by mutableStateOf(false)
    var isShiftActive by mutableStateOf(false)
    var isAltActive by mutableStateOf(false)
    var isCapsActive by mutableStateOf(false)
    var isFullKeyboardVisible by mutableStateOf(false)
    var isFloating by mutableStateOf(false)
    var keyboardOffset by mutableStateOf(IntOffset(0, 0))
    fun consumeCtrl(): Boolean = isCtrlActive
    fun consumeShift(): Boolean = isShiftActive
    fun consumeAlt(): Boolean = isAltActive
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
    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val containerHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val panelHeight = if (isLandscape) containerHeight / 2 else containerHeight / 3
    BackHandler(IMEState.isFullKeyboardVisible) {
        if (IMEState.isFloating) IMEState.isFloating = false
        IMEState.isFullKeyboardVisible = false
    }
    Box(modifier = if (IMEState.isFloating) Modifier
        .offset { IMEState.keyboardOffset }
        .size(width = 360.dp, height = panelHeight) else Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.70f))
        ) {
            if (!IMEState.isFullKeyboardVisible) Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                listOf(
                    listOf("Esc", "F1", "F2", "F3", "·", "F4", "F5", "F6", "Del"),
                    listOf("Shift", "F7", "F8", "F9", "↑", "F10", "F11", "F12", "Back"),
                    listOf("Tab", "Ctrl", "Alt", "←", "↓", "→", "Home", "End", "Enter")
                ).forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        rowKeys.forEach { keyLabel ->
                            KeyBase(
                                label = keyLabel,
                                width = null,
                                weight = 1f,
                                isModifier = keyLabel in listOf("Ctrl", "Alt", "Shift"),
                                isControl = keyLabel == "·"
                            )
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
                                val kw = when (label) {
                                    " " -> 3.9f; "Ctrl", "Alt", "Home", "End" -> 1.2f; else -> 1f
                                }
                                KeyBase(
                                    label,
                                    weight = kw,
                                    width = null,
                                    isModifier = label in listOf("Ctrl", "Shift", "Alt", "Caps"),
                                    isControl = label == ""
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
    label: String,
    width: Dp?,
    weight: Float?,
    isModifier: Boolean = false,
    isControl: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }
    val view = LocalView.current
    val isActive = when (label) {
        "Ctrl" -> IMEState.isCtrlActive; "Shift" -> IMEState.isShiftActive; "Alt" -> IMEState.isAltActive; "Caps" -> IMEState.isCapsActive; else -> false
    }
    val contentColor = if (isPressed || isActive) Color(0xFFE95420) else Color.White
    val displayText = when {
        isControl -> ""
        isModifier || label.length > 1 || (!label[0].isLetter() && !symbolMap.containsKey(label)) -> label
        IMEState.isShiftActive -> symbolMap[label] ?: label.uppercase()
        IMEState.isCapsActive && label[0].isLetter() -> label.uppercase()
        else -> label
    }
    Box(
        modifier = (if (weight != null) Modifier.weight(weight) else if (width != null) Modifier.width(
        width
    ) else Modifier)
            .fillMaxHeight()
            .pointerInput(label) {
                if (isControl) {
                    detectDragGestures(onDragStart = {
                        if (IMEState.isFullKeyboardVisible) {
                            IMEState.isFloating = true; view.performHapticFeedback(
                                HapticFeedbackConstants.LONG_PRESS
                            )
                        }
                    }, onDrag = { change, dragAmount ->
                        if (IMEState.isFloating) {
                            change.consume(); IMEState.keyboardOffset += IntOffset(
                                dragAmount.x.roundToInt(), dragAmount.y.roundToInt()
                            )
                        }
                    })
                }
            }
            .pointerInput(label) {
                detectTapGestures(onPress = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val startTime = System.currentTimeMillis()
                    if (isControl) {
                        try {
                            awaitRelease()
                            IMEState.isFullKeyboardVisible = !IMEState.isFullKeyboardVisible
                            if (!IMEState.isFullKeyboardVisible) {
                                IMEState.isFloating = false; IMEState.keyboardOffset =
                                    IntOffset(0, 0)
                            }
                        } finally {
                        }
                    } else {
                        repeatJob = coroutineScope.launch {
                            if (isModifier) {
                                when (label) {
                                    "Ctrl" -> IMEState.isCtrlActive =
                                        !IMEState.isCtrlActive; "Shift" -> IMEState.isShiftActive =
                                    !IMEState.isShiftActive; "Alt" -> IMEState.isAltActive =
                                    !IMEState.isAltActive; "Caps" -> IMEState.isCapsActive =
                                    !IMEState.isCapsActive
                                }
                            } else {
                                processKey(label)
                                delay(300L)
                                while (true) {
                                    processKey(label)
                                    val duration =
                                        System.currentTimeMillis() - startTime; delay(if (duration < 3000) 35L else 12L)
                                }
                            }
                        }
                        try {
                            awaitRelease()
                        } finally {
                            repeatJob?.cancel()
                        }
                    }
                })
            }
            .background(Color.Transparent), contentAlignment = Alignment.Center) {
        Text(displayText, color = contentColor, fontSize = 12.sp, softWrap = false)
    }
}

private fun processKey(label: String) {
    val code = keyCodes[label]
    if (code != null) sendToTTY(if (IMEState.isAltActive && label != "Alt") "\u001b$code" else code)
    else {
        val useUpper =
            IMEState.isShiftActive || (IMEState.isCapsActive && label.length == 1 && label[0].isLetter())
        var text = if (IMEState.isShiftActive) (symbolMap[label]
            ?: label.uppercase()) else if (useUpper) label.uppercase() else label.lowercase()
        if (IMEState.isCtrlActive && text.length == 1) {
            val upper = text.uppercase()[0]; if (upper in '@'..'_') text =
                (upper.code - '@'.code).toChar().toString()
        }
        sendToTTY(if (IMEState.isAltActive) "\u001b$text" else text)
    }
}

private fun sendToTTY(data: String) = ttySession?.write(data)
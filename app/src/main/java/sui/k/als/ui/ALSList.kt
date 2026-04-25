package sui.k.als.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import sui.k.als.localFont

@Composable
fun ALSList(
    data: Any?,
    show: Boolean = false,
    value: String? = null,
    checked: Boolean = true,
    first: Boolean = false,
    last: Boolean = false,
    backgrounds: Color? = null,
    onDismiss: () -> Unit = {},
    onValueChange: ((String) -> Unit)? = null,
    onClick: (String) -> Unit = {}
) {
    if (data is List<*>) {
        if (show) Dialog(onDismiss) {
            Column(
                Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Black)
                    .verticalScroll(rememberScrollState())
            ) {
                data.forEachIndexed { i, d ->
                    ALSList(
                        data = d.toString(), first = i == 0, last = i == data.size - 1
                    ) { onClick(it); onDismiss() }
                }
            }
        }
    } else {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val shape = when {
            first && last -> RoundedCornerShape(9.dp)
            first -> RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp)
            last -> RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp)
            else -> RoundedCornerShape(0.dp)
        }
        val bg = backgrounds ?: if (pressed) Color.Gray else Color.DarkGray
        Row(Modifier
            .fillMaxWidth()
            .height(27.dp)
            .clip(shape)
            .background(bg)
            .let {
                if (onValueChange == null) it.clickable(
                    interaction, null
                ) { onClick(data.toString()) } else it
            }
            .padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                data.toString(),
                modifier = if (value != null) Modifier.weight(0.4f) else Modifier,
                color = if (checked) Color.White else Color.Gray,
                fontSize = 9.sp,
                fontFamily = localFont.current
            )
            if (value != null) {
                if (onValueChange != null) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(0.6f),
                        interactionSource = interaction,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 10.sp,
                            color = Color.White,
                            fontFamily = localFont.current,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(Color.White)
                    )
                } else {
                    Text(
                        value,
                        modifier = Modifier.weight(0.6f),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = localFont.current,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
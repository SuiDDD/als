package sui.k.als.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import sui.k.als.*

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
    iconContent: @Composable (RowScope.() -> Unit)? = null,
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
                data.forEachIndexed { i, it ->
                    ALSList(
                        it,
                        first = i == 0,
                        last = i == data.size - 1,
                        onClick = { s -> onClick(s); onDismiss() })
                }
            }
        }; return
    }
    val textData = data?.toString() ?: ""
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(
        topStart = if (first) 9.dp else 0.dp,
        topEnd = if (first) 9.dp else 0.dp,
        bottomStart = if (last) 9.dp else 0.dp,
        bottomEnd = if (last) 9.dp else 0.dp
    ); Row(
        Modifier
            .fillMaxWidth()
            .height(27.dp)
            .clip(shape)
            .background(backgrounds ?: if (isPressed) Color.Gray else Color(0xFF111111))
            .then(
                if (onValueChange == null) Modifier.clickable(
                    interaction, null
                ) { onClick(textData) } else Modifier)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val style = TextStyle(
            fontSize = 9.sp, color = Color.White, fontFamily = localFont.current
        ); if (value == null && onValueChange != null) BasicTextField(
        textData,
        onValueChange,
        Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = style.copy(textAlign = TextAlign.Start),
        cursorBrush = SolidColor(Color.White)
    ) else {
        Text(
            textData,
            Modifier
                .weight(1f)
                .padding(start = 3.dp),
            color = if (checked) Color.White else Color.Gray,
            fontSize = 9.sp,
            fontFamily = localFont.current
        ); value?.let { v ->
            Box(Modifier.weight(0.6f)) {
                onValueChange?.let {
                    BasicTextField(
                        v,
                        it,
                        Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = style.copy(textAlign = TextAlign.End),
                        cursorBrush = SolidColor(Color.White)
                    )
                } ?: Text(
                    v,
                    Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontSize = 9.sp,
                    textAlign = TextAlign.End,
                    fontFamily = localFont.current
                )
            }
        }; iconContent?.invoke(this)
    }
    }
}
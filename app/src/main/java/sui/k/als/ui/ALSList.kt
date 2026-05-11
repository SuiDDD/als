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
    background: Color? = null,
    onDismiss: () -> Unit = {},
    onValueChange: ((String) -> Unit)? = null,
    iconContent: @Composable (RowScope.() -> Unit)? = null,
    onClick: (String) -> Unit = {}
) {
    if (data is List<*> && show) {
        Dialog(onDismiss) {
            Column(
                Modifier
                    .fillMaxWidth(0.9f)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black)
                    .verticalScroll(rememberScrollState())
            ) {
                data.forEachIndexed { index, item ->
                    ALSList(item, first = index == 0, last = index == data.size - 1, onClick = { onClick(it); onDismiss() })
                }
            }
        }
        return
    }
    val text = data?.toString().orEmpty()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(
        topStart = if (first) 3.dp else 0.dp,
        topEnd = if (first) 3.dp else 0.dp,
        bottomStart = if (last) 3.dp else 0.dp,
        bottomEnd = if (last) 3.dp else 0.dp
    )
    val textStyle = TextStyle(fontSize = 9.sp, color = Color.White, fontFamily = localFont.current)
    Row(
        Modifier
            .fillMaxWidth()
            .height(27.dp)
            .clip(shape)
            .background(background ?: if (isPressed) Color.Gray else Color(0xFF111111))
            .then(if (onValueChange == null) Modifier.clickable(interactionSource, null) { onClick(text) } else Modifier)
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (value == null && onValueChange != null) {
            BasicTextField(text, onValueChange, Modifier.fillMaxWidth(), singleLine = true, textStyle = textStyle, cursorBrush = SolidColor(Color.White))
        } else {
            Text(text, Modifier.weight(0.3f), color = if (checked) Color.White else Color.Gray, fontSize = 9.sp, fontFamily = localFont.current)
            value?.let { content ->
                Box(Modifier.weight(0.7f)) {
                    onValueChange?.let {
                        BasicTextField(content, it, Modifier.fillMaxWidth(), singleLine = true, textStyle = textStyle.copy(textAlign = TextAlign.End), cursorBrush = SolidColor(Color.White))
                    } ?: Text(content, Modifier.fillMaxWidth(), color = Color.White, fontSize = 9.sp, textAlign = TextAlign.End, fontFamily = localFont.current)
                }
            }
            iconContent?.invoke(this)
        }
    }
}
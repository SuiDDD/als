package sui.k.als.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.localFont

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ALSButton(
    icon: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 27.dp,
    iconSize: Dp = 18.dp,
    rad: Dp = 9.dp,
    regColor: Color = Color.DarkGray,
    pressedColor: Color = Color.Gray,
    iconTint: Color = Color.White,
    longClick: (() -> Unit)? = null,
    click: () -> Unit = {}
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(rad))
            .background(if (pressed) pressedColor else regColor)
            .combinedClickable(interaction, null, onLongClick = longClick, onClick = click),
        Alignment.Center
    ) {
        when (icon) {
            is Int -> Icon(painterResource(icon), null, Modifier.size(iconSize), iconTint)
            is ImageBitmap -> Image(icon, null, Modifier.size(iconSize))
            is String -> Text(
                icon, color = iconTint, fontSize = 9.sp, fontFamily = localFont.current
            )
        }
    }
}
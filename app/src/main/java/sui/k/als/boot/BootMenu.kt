package sui.k.als.boot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BootMenu(
    text: String, font: FontFamily, background: Color = Color.Transparent, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    Box(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
            .background(if (isPressed) BootConfig.ubuntuOrange else background, RectangleShape)
            .clickable(interactionSource, null, onClick = onClick), Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (background == Color.Red) Color.Black else Color.White,
            fontSize = 9.sp,
            fontFamily = font
        )
    }
}
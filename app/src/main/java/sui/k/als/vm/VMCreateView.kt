package sui.k.als.vm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.R
import sui.k.als.localFont

@Composable
fun ExpressiveCanvas(
    title: String,
    navigationItems: List<String>,
    onAction: () -> Unit,
    content: @Composable (Int) -> Unit
) {
    val font = localFont.current
    var activeIndex by remember { mutableIntStateOf(0) }
    Surface(color = Color.Black) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.fillMaxHeight(0.018f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 9.sp, fontFamily = font, color = Color.White)
                val interaction = remember { MutableInteractionSource() }
                val isPressed by interaction.collectIsPressedAsState()
                Box(
                    Modifier
                        .size(18.dp)
                        .clickable(interaction, null, onClick = onAction),
                    Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.save_wght300_24px),
                        null,
                        Modifier.size(12.dp),
                        if (isPressed) Color.White else Color.Gray
                    )
                }
            }
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .width(45.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    navigationItems.forEachIndexed { index, name ->
                        val interaction = remember { MutableInteractionSource() }
                        val isPressed by interaction.collectIsPressedAsState()
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .clickable(interaction, null) { activeIndex = index },
                            Alignment.CenterStart
                        ) {
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontFamily = font,
                                color = if (activeIndex == index || isPressed) Color.White else Color.Gray
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                    color = Color.Black
                ) {
                    AnimatedContent(activeIndex, transitionSpec = {
                        val spec = tween<Float>(90, easing = LinearEasing)
                        fadeIn(spec).togetherWith(fadeOut(spec)).using(SizeTransform(false))
                    }, label = "") { target ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(18.dp), Alignment.CenterStart
                            ) {
                                Text(
                                    text = navigationItems[target],
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontFamily = font
                                )
                            }
                            content(target)
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputCell(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(18.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.3f),
            color = Color.White,
            fontSize = 9.sp,
            fontFamily = localFont.current
        )
        BasicTextField(
            value, onValueChange, Modifier.weight(0.7f), textStyle = TextStyle(
                fontSize = 9.sp,
                color = Color.White,
                fontFamily = localFont.current,
                textAlign = TextAlign.End
            ), cursorBrush = SolidColor(Color.White)
        )
    }
}

@Composable
fun ToggleCell(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clickable(interaction, null) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (isPressed) Color.Gray else Color.White,
            fontSize = 9.sp,
            fontFamily = localFont.current
        )
        Box(
            Modifier
                .size(9.dp)
                .background(if (checked) Color.White else Color.Transparent, CircleShape)
                .border(0.3.dp, if (checked) Color.White else Color.Gray, CircleShape)
        )
    }
}
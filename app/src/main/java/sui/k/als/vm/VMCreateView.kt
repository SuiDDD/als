package sui.k.als.vm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.localAppFont

private val GlobalTextSize = 12.sp
private val AccentColor = Color(0xFFE95420)
private val BgGray = Color(0xFFF6F6F6)
private val PanelGray = Color(0xFFF1F1F1)

@Composable
fun ExpressiveCanvas(
    title: String,
    navigationItems: List<String>,
    onAction: () -> Unit,
    content: @Composable (Int) -> Unit
) {
    val appFont = localAppFont.current
    var activeIndex by remember { mutableIntStateOf(0) }
    Surface(color = BgGray) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(title, fontSize = 16.sp, fontFamily = appFont)
                Surface(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null) { onAction() },
                    color = AccentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Save,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .width(90.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp)
                ) {
                    navigationItems.forEachIndexed { index, name ->
                        val isSelected = activeIndex == index
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(vertical = 4.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { activeIndex = index }),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AccentColor.copy(0.08f) else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.padding(start = 9.dp)
                            ) {
                                Text(
                                    name,
                                    fontSize = GlobalTextSize,
                                    color = if (isSelected) AccentColor else Color(0xFF444444),
                                    fontFamily = appFont
                                )
                            }
                        }
                    }
                }
                Surface(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                    color = PanelGray,
                    shape = RoundedCornerShape(topStart = 24.dp)
                ) {
                    AnimatedContent(
                        targetState = activeIndex, transitionSpec = {
                            val animationSpec =
                                tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
                            (fadeIn(animationSpec) + scaleIn(
                                initialScale = 0.98f, animationSpec = animationSpec
                            )).togetherWith(
                                fadeOut(animationSpec) + scaleOut(
                                    targetScale = 1.02f, animationSpec = animationSpec
                                )
                            ).using(SizeTransform(clip = false))
                        }) { targetIndex ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                navigationItems[targetIndex],
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.Gray.copy(0.5f),
                                fontFamily = appFont,
                                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color.Black.copy(0.03f))
                            ) {
                                Column { content(targetIndex) }
                            }
                            Spacer(Modifier.height(32.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = GlobalTextSize,
            fontFamily = localAppFont.current,
            modifier = Modifier.weight(1f)
        )
        BasicTextField(
            value, onValueChange, textStyle = TextStyle(
                fontSize = GlobalTextSize,
                fontFamily = localAppFont.current,
                textAlign = TextAlign.End,
                color = AccentColor
            )
        )
    }
}

@Composable
fun ToggleCell(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(remember { MutableInteractionSource() }, null) { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = GlobalTextSize,
            fontFamily = localAppFont.current,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked, onCheckedChange, Modifier.scale(0.72f), colors = SwitchDefaults.colors(
                checkedTrackColor = AccentColor, uncheckedTrackColor = Color.LightGray.copy(0.3f)
            )
        )
    }
}

@Composable
fun Separator() =
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp, Color.Black.copy(0.04f))
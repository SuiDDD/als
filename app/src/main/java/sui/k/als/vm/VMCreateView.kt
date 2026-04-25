package sui.k.als.vm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import sui.k.als.ui.ALSButton

@Composable
fun ExpressiveCanvas(
    icons: List<Int>,
    activeIndex: Int,
    onIndexChange: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onAction: () -> Unit,
    content: @Composable (Int) -> Unit
) {
    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(9.dp)
        ) {
            Column(
                Modifier
                    .width(27.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                icons.forEachIndexed { i, icon ->
                    val active = activeIndex == i
                    ALSButton(
                        icon = icon,
                        regColor = if (active) Color.Gray else Color.DarkGray,
                        iconTint = if (active) Color.White else Color.LightGray,
                        longClick = { onLongClick(i) }) { onIndexChange(i) }
                }
                ALSButton(sui.k.als.R.drawable.save) { onAction() }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 9.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                color = Color.Black
            ) {
                AnimatedContent(
                    targetState = activeIndex, transitionSpec = {
                        fadeIn(
                            tween(
                                90, easing = LinearEasing
                            )
                        ).togetherWith(fadeOut(tween(90, easing = LinearEasing)))
                            .using(SizeTransform(false))
                    }, label = ""
                ) { target ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        content(
                            target
                        )
                    }
                }
            }
        }
    }
}
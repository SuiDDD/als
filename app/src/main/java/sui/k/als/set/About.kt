package sui.k.als.set

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sui.k.als.localFont

@Composable
fun About(onBack: () -> Unit) {
    BackHandler { onBack() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier
                .padding(9.dp)
                .then(
                    if (isLandscape) Modifier.fillMaxWidth(1f / 3f).fillMaxHeight()
                    else Modifier.fillMaxWidth().height(configuration.screenHeightDp.dp / 3)
                )
                .clip(RoundedCornerShape(9.dp))
                .drawBehind {
                    drawRect(
                        Brush.linearGradient(
                            0.0f to Color(0xFF111111),
                            1.0f to Color(0xFF444444),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f)
                        )
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "A L S",
                color = Color.White,
                fontSize = 36.sp,
                fontFamily = localFont.current
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "26.4.25 P3",
                color = Color.Gray,
                fontSize = 9.sp,
                fontFamily = localFont.current
            )
        }
    }
}
